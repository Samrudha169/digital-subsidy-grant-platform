package com.dsgp.verification.service;

import com.dsgp.application.entity.SchemeApplication;
import com.dsgp.beneficiary.repository.SchemeApplicationRepository;
import com.dsgp.eligibility.entity.EligibilityResult;
import com.dsgp.eligibility.entity.EligibilityStatus;
import com.dsgp.eligibility.repository.EligibilityResultRepository;
import com.dsgp.verification.dto.VerificationActionRequest;
import com.dsgp.verification.dto.VerificationHistoryEntry;
import com.dsgp.verification.dto.VerificationStatusResponse;
import com.dsgp.verification.entity.VerificationAction;
import com.dsgp.verification.entity.VerificationRecord;
import com.dsgp.verification.entity.VerificationStage;
import com.dsgp.verification.exception.InvalidVerificationTransitionException;
import com.dsgp.verification.repository.VerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of the multi-level verification workflow.
 *
 * <h3>State machine</h3>
 * <pre>
 * PENDING            → UNDER_REVIEW       (startVerification)
 * UNDER_REVIEW       → FIELD_APPROVED     (approveAtField)
 * UNDER_REVIEW       → ESCALATED          (escalateAtField)
 * UNDER_REVIEW       → REJECTED           (rejectAtField)
 * FIELD_APPROVED     → APPROVED           (approveAtFinance)
 * FIELD_APPROVED     → REJECTED           (rejectAtFinance)
 * ESCALATED          → DISTRICT_APPROVED  (approveAtDistrict)
 * ESCALATED          → REJECTED           (rejectAtDistrict)
 * DISTRICT_APPROVED  → APPROVED           (approveAtFinance)
 * DISTRICT_APPROVED  → REJECTED           (rejectAtFinance)
 * </pre>
 *
 * <h3>Routing policy (Assumption — documented)</h3>
 * <p>The project documentation ({@code verification-workflow.md §5}) states that
 * routing from Field → District is triggered by the Field Officer choosing to
 * <em>escalate</em> (e.g. when the application value exceeds the Field Officer's
 * authorisation limit, or documentation is incomplete).
 *
 * <p><strong>The documentation does NOT specify numeric eligibility-score or
 * grant-amount thresholds for automatic routing.</strong> Therefore this
 * implementation uses the Field Officer's explicit decision (APPROVE vs ESCALATE)
 * as the routing mechanism. Constants
 * {@link #FIELD_SCORE_ESCALATION_THRESHOLD} and
 * {@link #FIELD_GRANT_ESCALATION_THRESHOLD} are defined below and left at
 * sentinel values ({@code 0}) so they are easy to configure later without
 * restructuring the code. Until non-zero values are set, auto-escalation
 * based on score/amount is disabled and the Field Officer's manual decision
 * is used exclusively.
 *
 * <h3>SLA values (from {@code verification-workflow.md §8})</h3>
 * <ul>
 *   <li>Field Officer:   5 working days</li>
 *   <li>District Officer: 3 working days</li>
 *   <li>Finance Approver: 2 working days</li>
 * </ul>
 * SLA values are stored as constants; automated escalation scheduling is
 * designed to be added in Milestone 3 without changing this service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VerificationServiceImpl implements VerificationService {

    // ── SLA constants (from verification-workflow.md §8) ─────────────────────
    /** Field Officer review target: 5 working days. */
    public static final int FIELD_SLA_DAYS     = 5;
    /** District Officer review target: 3 working days. */
    public static final int DISTRICT_SLA_DAYS  = 3;
    /** Finance Approver review target: 2 working days. */
    public static final int FINANCE_SLA_DAYS   = 2;

    // ── Routing thresholds (NOT specified in docs — configurable placeholders) ─
    /**
     * Minimum eligibility score below which auto-escalation occurs.
     * Set to 0 to disable automatic score-based routing (Field Officer decides).
     * The project documentation does not specify this value.
     */
    public static final int FIELD_SCORE_ESCALATION_THRESHOLD = 0;

    /**
     * Grant amount (₹) above which auto-escalation occurs.
     * Set to 0 to disable automatic grant-amount-based routing.
     * The project documentation does not specify this value.
     */
    public static final long FIELD_GRANT_ESCALATION_THRESHOLD = 0L;

    // ── Application status string constants ───────────────────────────────────
    private static final String STATUS_PENDING           = "PENDING";
    private static final String STATUS_UNDER_REVIEW      = "UNDER_REVIEW";
    private static final String STATUS_FIELD_APPROVED    = "FIELD_APPROVED";
    private static final String STATUS_ESCALATED         = "ESCALATED";
    private static final String STATUS_DISTRICT_APPROVED = "DISTRICT_APPROVED";
    private static final String STATUS_APPROVED          = "APPROVED";
    private static final String STATUS_REJECTED          = "REJECTED";

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final SchemeApplicationRepository  applicationRepository;
    private final EligibilityResultRepository  eligibilityResultRepository;
    private final VerificationRepository       verificationRepository;

    // ════════════════════════════════════════════════════════════════════════
    // Public API
    // ════════════════════════════════════════════════════════════════════════

    @Override
    public VerificationStatusResponse startVerification(Long applicationId,
                                                        VerificationActionRequest request) {
        SchemeApplication app = requireApplication(applicationId);
        requireStatus(app, STATUS_PENDING,
                "Verification can only be started for a PENDING application. " +
                "Current status: " + app.getApplicationStatus());

        // Guard: the application must have a passing eligibility result
        EligibilityResult eligibility = eligibilityResultRepository
                .findByBeneficiaryIdAndSchemeId(
                        app.getBeneficiary().getId(),
                        app.getScheme().getId())
                .orElseThrow(() -> new InvalidVerificationTransitionException(
                        "Cannot start verification: no eligibility result found for " +
                        "beneficiaryId=" + app.getBeneficiary().getId() +
                        " schemeId=" + app.getScheme().getId()));

        if (eligibility.getEligibilityStatus() == EligibilityStatus.INELIGIBLE) {
            throw new InvalidVerificationTransitionException(
                    "Cannot start verification: beneficiary is INELIGIBLE for this scheme. " +
                    "Eligibility score: " + eligibility.getTotalScore() + "/100.");
        }

        updateStatus(app, STATUS_UNDER_REVIEW);
        recordAction(app, VerificationStage.FIELD, VerificationAction.APPROVE,
                request.getPerformedBy(),
                coalesce(request.getRemarks(), "Verification started — assigned to Field Officer."));

        log.info("Verification started: applicationId={}, by={}", applicationId, request.getPerformedBy());
        return buildResponse(app);
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationStatusResponse getStatus(Long applicationId) {
        return buildResponse(requireApplication(applicationId));
    }

    // ── Field Officer actions ─────────────────────────────────────────────────

    @Override
    public VerificationStatusResponse approveAtField(Long applicationId,
                                                     VerificationActionRequest request) {
        SchemeApplication app = requireApplication(applicationId);
        requireStatus(app, STATUS_UNDER_REVIEW,
                "Field approval requires status UNDER_REVIEW. Current: " + app.getApplicationStatus());

        updateStatus(app, STATUS_FIELD_APPROVED);
        recordAction(app, VerificationStage.FIELD, VerificationAction.APPROVE,
                request.getPerformedBy(), request.getRemarks());

        log.info("Field APPROVE: applicationId={}, by={}", applicationId, request.getPerformedBy());
        return buildResponse(app);
    }

    @Override
    public VerificationStatusResponse rejectAtField(Long applicationId,
                                                    VerificationActionRequest request) {
        SchemeApplication app = requireApplication(applicationId);
        requireStatus(app, STATUS_UNDER_REVIEW,
                "Field rejection requires status UNDER_REVIEW. Current: " + app.getApplicationStatus());
        requireRemarks(request, "Field rejection requires a reason in the remarks field.");

        updateStatus(app, STATUS_REJECTED);
        recordAction(app, VerificationStage.FIELD, VerificationAction.REJECT,
                request.getPerformedBy(), request.getRemarks());

        log.info("Field REJECT: applicationId={}, by={}", applicationId, request.getPerformedBy());
        return buildResponse(app);
    }

    @Override
    public VerificationStatusResponse escalateAtField(Long applicationId,
                                                      VerificationActionRequest request) {
        SchemeApplication app = requireApplication(applicationId);
        requireStatus(app, STATUS_UNDER_REVIEW,
                "Escalation requires status UNDER_REVIEW. Current: " + app.getApplicationStatus());

        updateStatus(app, STATUS_ESCALATED);
        recordAction(app, VerificationStage.FIELD, VerificationAction.ESCALATE,
                request.getPerformedBy(), request.getRemarks());

        log.info("Field ESCALATE: applicationId={}, by={}", applicationId, request.getPerformedBy());
        return buildResponse(app);
    }

    // ── District Officer actions ──────────────────────────────────────────────

    @Override
    public VerificationStatusResponse approveAtDistrict(Long applicationId,
                                                        VerificationActionRequest request) {
        SchemeApplication app = requireApplication(applicationId);
        requireStatus(app, STATUS_ESCALATED,
                "District approval requires status ESCALATED. Current: " + app.getApplicationStatus());

        updateStatus(app, STATUS_DISTRICT_APPROVED);
        recordAction(app, VerificationStage.DISTRICT, VerificationAction.APPROVE,
                request.getPerformedBy(), request.getRemarks());

        log.info("District APPROVE: applicationId={}, by={}", applicationId, request.getPerformedBy());
        return buildResponse(app);
    }

    @Override
    public VerificationStatusResponse rejectAtDistrict(Long applicationId,
                                                       VerificationActionRequest request) {
        SchemeApplication app = requireApplication(applicationId);
        requireStatus(app, STATUS_ESCALATED,
                "District rejection requires status ESCALATED. Current: " + app.getApplicationStatus());
        requireRemarks(request, "District rejection requires a reason in the remarks field.");

        updateStatus(app, STATUS_REJECTED);
        recordAction(app, VerificationStage.DISTRICT, VerificationAction.REJECT,
                request.getPerformedBy(), request.getRemarks());

        log.info("District REJECT: applicationId={}, by={}", applicationId, request.getPerformedBy());
        return buildResponse(app);
    }

    // ── Finance Approver actions ──────────────────────────────────────────────

    @Override
    public VerificationStatusResponse approveAtFinance(Long applicationId,
                                                       VerificationActionRequest request) {
        SchemeApplication app = requireApplication(applicationId);
        // Finance may approve from FIELD_APPROVED (direct) or DISTRICT_APPROVED (after escalation)
        requireStatusIn(app,
                List.of(STATUS_FIELD_APPROVED, STATUS_DISTRICT_APPROVED),
                "Finance approval requires status FIELD_APPROVED or DISTRICT_APPROVED. " +
                "Current: " + app.getApplicationStatus());

        updateStatus(app, STATUS_APPROVED);
        recordAction(app, VerificationStage.FINANCE, VerificationAction.APPROVE,
                request.getPerformedBy(), request.getRemarks());

        log.info("Finance APPROVE: applicationId={}, by={}", applicationId, request.getPerformedBy());
        return buildResponse(app);
    }

    @Override
    public VerificationStatusResponse rejectAtFinance(Long applicationId,
                                                      VerificationActionRequest request) {
        SchemeApplication app = requireApplication(applicationId);
        requireStatusIn(app,
                List.of(STATUS_FIELD_APPROVED, STATUS_DISTRICT_APPROVED),
                "Finance rejection requires status FIELD_APPROVED or DISTRICT_APPROVED. " +
                "Current: " + app.getApplicationStatus());
        requireRemarks(request, "Finance rejection requires a reason in the remarks field.");

        updateStatus(app, STATUS_REJECTED);
        recordAction(app, VerificationStage.FINANCE, VerificationAction.REJECT,
                request.getPerformedBy(), request.getRemarks());

        log.info("Finance REJECT: applicationId={}, by={}", applicationId, request.getPerformedBy());
        return buildResponse(app);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Private helpers
    // ════════════════════════════════════════════════════════════════════════

    private SchemeApplication requireApplication(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new com.dsgp.application.exception.ApplicationException(
                        "Application not found with ID: " + applicationId));
    }

    private void requireStatus(SchemeApplication app, String required, String message) {
        if (!required.equals(app.getApplicationStatus())) {
            throw new InvalidVerificationTransitionException(message);
        }
    }

    private void requireStatusIn(SchemeApplication app, List<String> allowed, String message) {
        if (!allowed.contains(app.getApplicationStatus())) {
            throw new InvalidVerificationTransitionException(message);
        }
    }

    private void requireRemarks(VerificationActionRequest request, String message) {
        if (request.getRemarks() == null || request.getRemarks().isBlank()) {
            throw new InvalidVerificationTransitionException(message);
        }
    }

    private void updateStatus(SchemeApplication app, String newStatus) {
        app.setApplicationStatus(newStatus);
        applicationRepository.save(app);
    }

    private void recordAction(SchemeApplication app,
                               VerificationStage stage,
                               VerificationAction action,
                               String performedBy,
                               String remarks) {
        VerificationRecord record = VerificationRecord.builder()
                .schemeApplication(app)
                .stage(stage)
                .actionTaken(action)
                .performedBy(performedBy)
                .remarks(remarks)
                .build();
        verificationRepository.save(record);
    }

    private VerificationStatusResponse buildResponse(SchemeApplication app) {
        List<VerificationRecord> records =
                verificationRepository.findBySchemeApplicationIdOrderByPerformedAtAsc(app.getId());

        List<VerificationHistoryEntry> history = records.stream()
                .map(r -> VerificationHistoryEntry.builder()
                        .stage(r.getStage().name())
                        .action(r.getActionTaken().name())
                        .performedBy(r.getPerformedBy())
                        .performedAt(r.getPerformedAt())
                        .remarks(r.getRemarks())
                        .build())
                .toList();

        return VerificationStatusResponse.builder()
                .applicationId(app.getId())
                .beneficiaryId(app.getBeneficiary().getId())
                .beneficiaryName(app.getBeneficiary().getFullName())
                .schemeId(app.getScheme().getId())
                .schemeName(app.getScheme().getSchemeName())
                .applicationStatus(app.getApplicationStatus())
                .applicationDate(app.getApplicationDate())
                .history(history)
                .build();
    }

    private String coalesce(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }
}
