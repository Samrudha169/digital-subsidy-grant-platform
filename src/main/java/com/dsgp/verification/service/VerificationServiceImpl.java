package com.dsgp.verification.service;

import com.dsgp.application.entity.SchemeApplication;
import com.dsgp.authentication.entity.Officer;
import com.dsgp.authentication.entity.OfficerRole;
import com.dsgp.authentication.repository.OfficerRepository;
import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.beneficiary.repository.SchemeApplicationRepository;
import com.dsgp.eligibility.entity.EligibilityResult;
import com.dsgp.eligibility.entity.EligibilityStatus;
import com.dsgp.eligibility.repository.EligibilityResultRepository;
import com.dsgp.verification.dto.VerificationActionRequest;
import com.dsgp.verification.dto.VerificationCriterionResponse;
import com.dsgp.verification.dto.VerificationCriterionUpdateRequest;
import com.dsgp.verification.dto.VerificationHistoryEntry;
import com.dsgp.verification.dto.VerificationStatusResponse;
import com.dsgp.verification.entity.VerificationAction;
import com.dsgp.verification.entity.VerificationCriterion;
import com.dsgp.verification.entity.VerificationCriterionStatus;
import com.dsgp.verification.entity.VerificationRecord;
import com.dsgp.verification.entity.VerificationStage;
import com.dsgp.verification.exception.InvalidVerificationTransitionException;
import com.dsgp.verification.repository.VerificationCriterionRepository;
import com.dsgp.verification.repository.VerificationRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.dsgp.beneficiary.repository.BeneficiaryDocumentRepository;
import com.dsgp.beneficiary.entity.DocumentType;
import com.dsgp.disbursement.entity.DisbursementType;
import com.dsgp.disbursement.service.DisbursementPlanService;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VerificationServiceImpl implements VerificationService {

    // ========================================================================
    // APPLICATION STATUS CONSTANTS
    // ========================================================================

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_UNDER_REVIEW = "UNDER_REVIEW";
    private static final String STATUS_FIELD_APPROVED = "FIELD_APPROVED";
    private static final String STATUS_ESCALATED = "ESCALATED";
    private static final String STATUS_DISTRICT_APPROVED = "DISTRICT_APPROVED";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CORRECTION_REQUIRED =
            "CORRECTION_REQUIRED";

    // ========================================================================
    // DEPENDENCIES
    // ========================================================================

    private final SchemeApplicationRepository applicationRepository;
    private final OfficerRepository officerRepository;
    private final EligibilityResultRepository eligibilityResultRepository;
    private final VerificationRecordRepository verificationRecordRepository;
    private final VerificationCriterionRepository verificationCriterionRepository;
    private final BeneficiaryDocumentRepository documentRepository;
    private final DisbursementPlanService disbursementPlanService;

    // ========================================================================
    // ROUTING THRESHOLDS  (configurable via application.properties)
    // ========================================================================

    /**
     * Minimum eligibility score required for direct-to-Finance routing.
     * Applications with a score below this threshold are always escalated
     * to the District Officer, regardless of grant amount.
     */
    @Value("${dsgp.verification.routing.minimum-direct-finance-score:60}")
    private int minimumDirectFinanceScore;

    /**
     * Maximum grant amount (inclusive) that qualifies for direct-to-Finance
     * routing. Applications whose scheme grant amount exceeds this value —
     * or where the grant amount is null — are escalated to the District Officer.
     */
    @Value("${dsgp.verification.routing.maximum-direct-finance-grant-amount:10000}")
    private int maximumDirectFinanceGrantAmount;

    // ========================================================================
    // START VERIFICATION
    // ========================================================================

    @Override
    public VerificationStatusResponse startVerification(
            Long applicationId,
            VerificationActionRequest request) {

        requireRequest(request);

        SchemeApplication application =
                requireApplication(applicationId);

        requireStatus(
                application,
                STATUS_PENDING,
                "Verification can only be started for a PENDING application. " +
                        "Current status: " +
                        application.getApplicationStatus()
        );

        Officer officer =
                requireOfficer(
                        request.getPerformedBy()
                );

        requireRole(
                officer,
                OfficerRole.FIELD_OFFICER,
                "Only a Field Officer can start verification."
        );

        // ------------------------------------------------------------
        // Eligibility guard
        // ------------------------------------------------------------

        EligibilityResult eligibility =
                eligibilityResultRepository
                        .findByBeneficiaryIdAndSchemeId(
                                application.getBeneficiary().getId(),
                                application.getScheme().getId()
                        )
                        .orElseThrow(() ->
                                new InvalidVerificationTransitionException(
                                        "Cannot start verification: " +
                                                "no eligibility result found."
                                )
                        );

        if (eligibility.getEligibilityStatus()
                == EligibilityStatus.INELIGIBLE) {

            throw new InvalidVerificationTransitionException(
                    "Cannot start verification: beneficiary is INELIGIBLE. " +
                            "Eligibility score: " +
                            eligibility.getTotalScore() +
                            "/100."
            );
        }

        // ------------------------------------------------------------
        // Move application to Field Officer review
        // ------------------------------------------------------------

        updateStatus(
                application,
                STATUS_UNDER_REVIEW
        );

        // ------------------------------------------------------------
        // Create Field Officer criteria
        // ------------------------------------------------------------

        createFieldCriteria(application);

        recordAction(
                application,
                VerificationStage.FIELD,
                VerificationAction.START,
                request.getPerformedBy(),
                coalesce(
                        request.getRemarks(),
                        "Verification started and assigned to Field Officer."
                )
        );

        log.info(
                "Verification started: applicationId={}, by={}",
                applicationId,
                request.getPerformedBy()
        );

        return buildResponse(application);
    }

    // ========================================================================
    // GET STATUS
    // ========================================================================

    @Override
    @Transactional(readOnly = true)
    public VerificationStatusResponse getStatus(
            Long applicationId) {

        return buildResponse(
                requireApplication(applicationId)
        );
    }

    // ========================================================================
    // FIELD OFFICER - APPROVE
    // ========================================================================

    @Override
    public VerificationStatusResponse approveAtField(
            Long applicationId,
            VerificationActionRequest request) {

        requireRequest(request);

        SchemeApplication application =
                requireApplication(applicationId);

        requireStatus(
                application,
                STATUS_UNDER_REVIEW,
                "Field approval requires status UNDER_REVIEW. " +
                        "Current status: " +
                        application.getApplicationStatus()
        );

        Officer officer =
                requireOfficer(
                        request.getPerformedBy()
                );

        requireRole(
                officer,
                OfficerRole.FIELD_OFFICER,
                "Only a Field Officer can approve at Field level."
        );

        // ------------------------------------------------------------
        // All Field criteria must be VERIFIED
        // ------------------------------------------------------------

        ensureAllCriteriaVerified(
                applicationId,
                VerificationStage.FIELD
        );

        // ------------------------------------------------------------
        // Routing: score + grant amount decide the next status.
        // ------------------------------------------------------------

        EligibilityResult eligibility =
                eligibilityResultRepository
                        .findByBeneficiaryIdAndSchemeId(
                                application.getBeneficiary().getId(),
                                application.getScheme().getId()
                        )
                        .orElseThrow(() ->
                                new InvalidVerificationTransitionException(
                                        "Cannot approve at Field: " +
                                                "no eligibility result found for " +
                                                "this application."
                                )
                        );

        String nextStatus = routeAfterFieldApproval(
                eligibility.getTotalScore(),
                application.getScheme().getGrantAmount()
        );

        updateStatus(application, nextStatus);

        String defaultRemarks = STATUS_FIELD_APPROVED.equals(nextStatus)
                ? "All Field criteria verified. Routed directly to Finance "
                + "(score=" + eligibility.getTotalScore()
                + ", grant=" + application.getScheme().getGrantAmount() + ")."
                : "All Field criteria verified. Escalated to District Officer "
                + "(score=" + eligibility.getTotalScore()
                + ", grant=" + application.getScheme().getGrantAmount() + ").";

        recordAction(
                application,
                VerificationStage.FIELD,
                VerificationAction.APPROVE,
                request.getPerformedBy(),
                coalesce(
                        request.getRemarks(),
                        defaultRemarks
                )
        );

        log.info(
                "Field APPROVE: applicationId={}, by={}, nextStatus={}",
                applicationId,
                request.getPerformedBy(),
                nextStatus
        );

        return buildResponse(application);
    }

    // ========================================================================
    // FIELD OFFICER - REJECT
    // ========================================================================

    @Override
    public VerificationStatusResponse rejectAtField(
            Long applicationId,
            VerificationActionRequest request) {

        requireRequest(request);

        SchemeApplication application =
                requireApplication(applicationId);

        requireStatus(
                application,
                STATUS_UNDER_REVIEW,
                "Field rejection requires status UNDER_REVIEW. " +
                        "Current status: " +
                        application.getApplicationStatus()
        );

        Officer officer =
                requireOfficer(
                        request.getPerformedBy()
                );

        requireRole(
                officer,
                OfficerRole.FIELD_OFFICER,
                "Only a Field Officer can reject at Field level."
        );

        requireRemarks(
                request,
                "Field rejection requires a reason in the remarks field."
        );

        updateStatus(
                application,
                STATUS_REJECTED
        );

        recordAction(
                application,
                VerificationStage.FIELD,
                VerificationAction.REJECT,
                request.getPerformedBy(),
                request.getRemarks()
        );

        log.info(
                "Field REJECT: applicationId={}, by={}",
                applicationId,
                request.getPerformedBy()
        );

        return buildResponse(application);
    }

    // ========================================================================
    // FIELD OFFICER - ESCALATE
    // ========================================================================

    @Override
    public VerificationStatusResponse escalateAtField(
            Long applicationId,
            VerificationActionRequest request) {

        requireRequest(request);

        SchemeApplication application =
                requireApplication(applicationId);

        requireStatus(
                application,
                STATUS_UNDER_REVIEW,
                "Escalation requires status UNDER_REVIEW. " +
                        "Current status: " +
                        application.getApplicationStatus()
        );

        Officer officer =
                requireOfficer(
                        request.getPerformedBy()
                );

        requireRole(
                officer,
                OfficerRole.FIELD_OFFICER,
                "Only a Field Officer can escalate an application."
        );

        updateStatus(
                application,
                STATUS_ESCALATED
        );

        recordAction(
                application,
                VerificationStage.FIELD,
                VerificationAction.ESCALATE,
                request.getPerformedBy(),
                request.getRemarks()
        );

        log.info(
                "Field ESCALATE: applicationId={}, by={}",
                applicationId,
                request.getPerformedBy()
        );

        return buildResponse(application);
    }

    // ========================================================================
    // FIELD OFFICER - REQUEST CORRECTION
    // ========================================================================

    @Override
    public VerificationStatusResponse requestCorrection(
            Long applicationId,
            VerificationActionRequest request) {

        requireRequest(request);

        SchemeApplication application =
                requireApplication(applicationId);

        requireStatus(
                application,
                STATUS_UNDER_REVIEW,
                "Correction request requires status UNDER_REVIEW. " +
                        "Current status: " +
                        application.getApplicationStatus()
        );

        Officer officer =
                requireOfficer(
                        request.getPerformedBy()
                );

        requireRole(
                officer,
                OfficerRole.FIELD_OFFICER,
                "Only a Field Officer can request correction."
        );

        requireRemarks(
                request,
                "Correction request requires remarks describing " +
                        "what needs to be corrected."
        );

        updateStatus(
                application,
                STATUS_CORRECTION_REQUIRED
        );

        recordAction(
                application,
                VerificationStage.FIELD,
                VerificationAction.REQUEST_CORRECTION,
                request.getPerformedBy(),
                request.getRemarks()
        );

        log.info(
                "Field REQUEST_CORRECTION: applicationId={}, by={}",
                applicationId,
                request.getPerformedBy()
        );

        return buildResponse(application);
    }

    // ========================================================================
    // BENEFICIARY - RESUBMIT
    // ========================================================================

    @Override
    public VerificationStatusResponse resubmitByBeneficiary(
            Long applicationId,
            VerificationActionRequest request) {

        requireRequest(request);

        SchemeApplication application =
                requireApplication(applicationId);

        requireStatus(
                application,
                STATUS_CORRECTION_REQUIRED,
                "Resubmission requires status CORRECTION_REQUIRED. " +
                        "Current status: " +
                        application.getApplicationStatus()
        );

        updateStatus(
                application,
                STATUS_UNDER_REVIEW
        );

        recordAction(
                application,
                VerificationStage.FIELD,
                VerificationAction.START,
                request.getPerformedBy(),
                coalesce(
                        request.getRemarks(),
                        "Beneficiary resubmitted application after corrections."
                )
        );

        log.info(
                "Beneficiary RESUBMIT: applicationId={}, by={}",
                applicationId,
                request.getPerformedBy()
        );

        return buildResponse(application);
    }

    // ========================================================================
    // LEGACY FIELD COMPLETION
    // ========================================================================

    @Override
    public void completeFieldVerification(
            Long applicationId,
            String performedBy,
            String remarks) {

        SchemeApplication application =
                requireApplication(applicationId);

        requireStatus(
                application,
                STATUS_UNDER_REVIEW,
                "Field verification requires status UNDER_REVIEW."
        );

        ensureAllCriteriaVerified(
                applicationId,
                VerificationStage.FIELD
        );

        EligibilityResult eligibility =
                eligibilityResultRepository
                        .findByBeneficiaryIdAndSchemeId(
                                application.getBeneficiary().getId(),
                                application.getScheme().getId()
                        )
                        .orElseThrow(() ->
                                new InvalidVerificationTransitionException(
                                        "Cannot complete Field verification: " +
                                                "no eligibility result found."
                                )
                        );

        String nextStatus = routeAfterFieldApproval(
                eligibility.getTotalScore(),
                application.getScheme().getGrantAmount()
        );

        updateStatus(application, nextStatus);

        recordAction(
                application,
                VerificationStage.FIELD,
                VerificationAction.APPROVE,
                performedBy,
                coalesce(
                        remarks,
                        "Field verification completed. Next status: " + nextStatus + "."
                )
        );
    }

    // ========================================================================
    // ROUTING DECISION
    // ========================================================================

    /**
     * Decides the post-field-approval status using the configurable thresholds:
     * <ul>
     *   <li>Score &ge; {@code minimumDirectFinanceScore} <b>AND</b>
     *       grant &le; {@code maximumDirectFinanceGrantAmount}
     *       &rarr; {@code FIELD_APPROVED} (direct to Finance)</li>
     *   <li>Otherwise (low score, high grant, or null grant)
     *       &rarr; {@code ESCALATED} (District Officer queue)</li>
     * </ul>
     *
     * <p>These thresholds are project configuration values loaded from
     * {@code application.properties}. They are not government policy.</p>
     *
     * @param totalScore   eligibility score from the persisted EligibilityResult
     * @param grantAmount  scheme grant amount; {@code null} is treated as
     *                     "above threshold" and forces escalation
     * @return {@code "FIELD_APPROVED"} or {@code "ESCALATED"}
     */
    private String routeAfterFieldApproval(
            int totalScore,
            BigDecimal grantAmount) {

        if (grantAmount == null) {
            log.debug(
                    "Routing: grant amount is null → ESCALATED "
                            + "(score={})",
                    totalScore
            );
            return STATUS_ESCALATED;
        }

        boolean scoreOk =
                totalScore >= minimumDirectFinanceScore;
        boolean grantOk =
                grantAmount.compareTo(
                        BigDecimal.valueOf(maximumDirectFinanceGrantAmount)
                ) <= 0;

        if (scoreOk && grantOk) {
            log.debug(
                    "Routing: score={} >= {}, grant={} <= {} → FIELD_APPROVED",
                    totalScore,
                    minimumDirectFinanceScore,
                    grantAmount,
                    maximumDirectFinanceGrantAmount
            );
            return STATUS_FIELD_APPROVED;
        }

        log.debug(
                "Routing: score={} (min={}), grant={} (max={}) → ESCALATED",
                totalScore,
                minimumDirectFinanceScore,
                grantAmount,
                maximumDirectFinanceGrantAmount
        );
        return STATUS_ESCALATED;
    }

    // ========================================================================
    // GET CRITERIA
    // ========================================================================

    @Override
    @Transactional
    public List<VerificationCriterionResponse> getCriteria(
            Long applicationId,
            VerificationStage stage) {

        SchemeApplication application =
                requireApplication(applicationId);

        List<VerificationCriterion> criteria =
                verificationCriterionRepository
                        .findBySchemeApplicationIdAndStageOrderByIdAsc(
                                applicationId,
                                stage
                        );

        // ------------------------------------------------------------
        // Create District criteria automatically after Field escalation
        // ------------------------------------------------------------
        //
        // When the Field Officer approves and the routing decision sends
        // the application to the District Officer, the status is ESCALATED
        // (not FIELD_APPROVED, which is the direct-to-Finance route).

        if (stage == VerificationStage.DISTRICT
                && STATUS_ESCALATED.equals(
                application.getApplicationStatus())
                && (criteria == null || criteria.isEmpty())) {

            createDistrictCriteria(application);

            criteria =
                    verificationCriterionRepository
                            .findBySchemeApplicationIdAndStageOrderByIdAsc(
                                    applicationId,
                                    VerificationStage.DISTRICT
                            );
        }

        // ------------------------------------------------------------
        // Create Finance criteria automatically when ready for Finance
        // ------------------------------------------------------------
        //
        // Two routes reach Finance:
        //   - FIELD_APPROVED  : direct route (high score, low grant amount)
        //   - DISTRICT_APPROVED : escalated route (via District Officer)

        boolean readyForFinance =
                STATUS_FIELD_APPROVED.equals(
                        application.getApplicationStatus())
                        || STATUS_DISTRICT_APPROVED.equals(
                        application.getApplicationStatus());

        if (stage == VerificationStage.FINANCE
                && readyForFinance
                && (criteria == null || criteria.isEmpty())) {

            createFinanceCriteria(application);

            criteria =
                    verificationCriterionRepository
                            .findBySchemeApplicationIdAndStageOrderByIdAsc(
                                    applicationId,
                                    VerificationStage.FINANCE
                            );
        }

        if (criteria == null) {
            return List.of();
        }

        return criteria.stream()
                .map(this::toCriterionResponse)
                .toList();
    }

    // ========================================================================
    // UPDATE CRITERION
    // ========================================================================

    @Override
    public VerificationCriterionResponse updateCriterion(
            Long applicationId,
            Long criterionId,
            VerificationCriterionUpdateRequest request) {

        if (request == null) {

            throw new InvalidVerificationTransitionException(
                    "Verification criterion update request is required."
            );
        }

        if (request.getPerformedBy() == null
                || request.getPerformedBy().isBlank()) {

            throw new InvalidVerificationTransitionException(
                    "performedBy is required."
            );
        }

        if (request.getStatus() == null) {

            throw new InvalidVerificationTransitionException(
                    "Criterion status is required."
            );
        }

        SchemeApplication application =
                requireApplication(applicationId);

        VerificationCriterion criterion =
                verificationCriterionRepository
                        .findById(criterionId)
                        .orElseThrow(() ->
                                new InvalidVerificationTransitionException(
                                        "Verification criterion not found: " +
                                                criterionId
                                )
                        );

        // ------------------------------------------------------------
        // Make sure criterion belongs to this application
        // ------------------------------------------------------------

        if (criterion.getSchemeApplication() == null
                || criterion.getSchemeApplication().getId() == null
                || !criterion.getSchemeApplication()
                .getId()
                .equals(applicationId)) {

            throw new InvalidVerificationTransitionException(
                    "Verification criterion does not belong to this application."
            );
        }

        Officer officer =
                requireOfficer(
                        request.getPerformedBy()
                );

        // ------------------------------------------------------------
        // FIELD criteria → only Field Officer
        // ------------------------------------------------------------

        if (criterion.getStage() == VerificationStage.FIELD) {

            requireRole(
                    officer,
                    OfficerRole.FIELD_OFFICER,
                    "Only a Field Officer can update Field criteria."
            );
        }

        // ------------------------------------------------------------
        // DISTRICT criteria → only District Officer
        // ------------------------------------------------------------

        if (criterion.getStage() == VerificationStage.DISTRICT) {

            requireRole(
                    officer,
                    OfficerRole.DISTRICT_OFFICER,
                    "Only a District Officer can update District criteria."
            );
        }

        // ------------------------------------------------------------
        // FINANCE criteria → only Finance Approver
        // ------------------------------------------------------------

        if (criterion.getStage() == VerificationStage.FINANCE) {

            requireRole(
                    officer,
                    OfficerRole.FINANCE_APPROVER,
                    "Only a Finance Approver can update Finance criteria."
            );
        }

        if (request.getStatus() == VerificationCriterionStatus.FAILED
                && (request.getRemarks() == null
                || request.getRemarks().isBlank())) {

            throw new InvalidVerificationTransitionException(
                    "Remarks are required when rejecting a criterion."
            );
        }

        if (request.getStatus() == VerificationCriterionStatus.VERIFIED) {
            validateRequiredProof(application, criterion);
        }

        criterion.setStatus(
                request.getStatus()
        );

        criterion.setVerifiedBy(
                request.getPerformedBy()
        );

        criterion.setRemarks(
                request.getRemarks()
        );

        if (request.getStatus()
                == VerificationCriterionStatus.VERIFIED) {

            criterion.setVerifiedAt(
                    LocalDateTime.now()
            );

        } else {

            criterion.setVerifiedAt(null);
        }

        VerificationCriterion saved =
                verificationCriterionRepository.save(
                        criterion
                );

        recordAction(
                application,
                criterion.getStage(),
                VerificationAction.CRITERION_VERIFIED,
                request.getPerformedBy(),
                coalesce(
                        request.getRemarks(),
                        "Criterion updated: " +
                                criterion.getCriterionName() +
                                " → " +
                                request.getStatus().name()
                )
        );

        return toCriterionResponse(saved);
    }

    // ========================================================================
    // DISTRICT OFFICER - APPROVE
    // ========================================================================

    @Override
    public VerificationStatusResponse approveAtDistrict(
            Long applicationId,
            VerificationActionRequest request) {

        requireRequest(request);

        SchemeApplication application =
                requireApplication(applicationId);

        /*
         * District Officer works only after Field Officer approval.
         */
        requireStatus(
                application,
                STATUS_ESCALATED,
                "District approval requires status ESCALATED. " +
                        "Current status: " +
                        application.getApplicationStatus()
        );

        Officer officer =
                requireOfficer(
                        request.getPerformedBy()
                );

        requireRole(
                officer,
                OfficerRole.DISTRICT_OFFICER,
                "Only a District Officer can approve at District level."
        );

        // ------------------------------------------------------------
        // Ensure District criteria exist before validation
        // ------------------------------------------------------------

        createDistrictCriteria(application);

        // ------------------------------------------------------------
        // All District criteria must be VERIFIED
        // ------------------------------------------------------------

        ensureAllCriteriaVerified(
                applicationId,
                VerificationStage.DISTRICT
        );

        // ------------------------------------------------------------
        // Move to Finance
        // ------------------------------------------------------------

        updateStatus(
                application,
                STATUS_DISTRICT_APPROVED
        );

        recordAction(
                application,
                VerificationStage.DISTRICT,
                VerificationAction.APPROVE,
                request.getPerformedBy(),
                coalesce(
                        request.getRemarks(),
                        "All District verification criteria verified. " +
                                "Application approved at District level."
                )
        );

        log.info(
                "District APPROVE: applicationId={}, by={}",
                applicationId,
                request.getPerformedBy()
        );

        return buildResponse(application);
    }

    // ========================================================================
    // DISTRICT OFFICER - REJECT
    // ========================================================================

    @Override
    public VerificationStatusResponse rejectAtDistrict(
            Long applicationId,
            VerificationActionRequest request) {

        requireRequest(request);

        SchemeApplication application =
                requireApplication(applicationId);

        requireStatus(
                application,
                STATUS_ESCALATED,
                "District rejection requires status ESCALATED. " +
                        "Current status: " +
                        application.getApplicationStatus()
        );

        Officer officer =
                requireOfficer(
                        request.getPerformedBy()
                );

        requireRole(
                officer,
                OfficerRole.DISTRICT_OFFICER,
                "Only a District Officer can reject at District level."
        );

        requireRemarks(
                request,
                "District rejection requires a reason in the remarks field."
        );

        updateStatus(
                application,
                STATUS_REJECTED
        );

        recordAction(
                application,
                VerificationStage.DISTRICT,
                VerificationAction.REJECT,
                request.getPerformedBy(),
                request.getRemarks()
        );

        log.info(
                "District REJECT: applicationId={}, by={}",
                applicationId,
                request.getPerformedBy()
        );

        return buildResponse(application);
    }

    // ========================================================================
    // FINANCE APPROVER - APPROVE
    // ========================================================================

    @Override
    public VerificationStatusResponse approveAtFinance(
            Long applicationId,
            VerificationActionRequest request) {

        requireRequest(request);

        SchemeApplication application =
                requireApplication(applicationId);

        /*
         * Finance receives applications via two routes:
         *   - FIELD_APPROVED  : direct route (high score, low grant amount)
         *   - DISTRICT_APPROVED : escalated route (via District Officer)
         */
        requireStatusOneOf(
                application,
                "Finance approval requires status FIELD_APPROVED or " +
                        "DISTRICT_APPROVED. Current status: " +
                        application.getApplicationStatus(),
                STATUS_FIELD_APPROVED,
                STATUS_DISTRICT_APPROVED
        );

        Officer officer =
                requireOfficer(
                        request.getPerformedBy()
                );

        requireRole(
                officer,
                OfficerRole.FINANCE_APPROVER,
                "Only a Finance Approver can approve at Finance level."
        );

        // ------------------------------------------------------------
        // Ensure Finance criteria exist before validation
        // ------------------------------------------------------------

        createFinanceCriteria(application);

        // ------------------------------------------------------------
        // All Finance criteria must be VERIFIED
        // ------------------------------------------------------------

        ensureAllCriteriaVerified(
                applicationId,
                VerificationStage.FINANCE
        );

        // ------------------------------------------------------------
        // Final approval
        // ------------------------------------------------------------

        // ------------------------------------------------------------
// Final approval
// ------------------------------------------------------------

        application.setSanctionedAmount(
                application.getScheme().getGrantAmount()
        );

        updateStatus(
                application,
                STATUS_APPROVED
        );

        disbursementPlanService.createPlan(
                application,
                DisbursementType.STAGED
        );

        recordAction(
                application,
                VerificationStage.FINANCE,
                VerificationAction.APPROVE,
                request.getPerformedBy(),
                coalesce(
                        request.getRemarks(),
                        "All Finance verification criteria verified. " +
                                "Application finally approved."
                )
        );

        log.info(
                "Finance APPROVE: applicationId={}, by={}",
                applicationId,
                request.getPerformedBy()
        );

        return buildResponse(application);
    }

    // ========================================================================
    // FINANCE APPROVER - REJECT
    // ========================================================================

    @Override
    public VerificationStatusResponse rejectAtFinance(
            Long applicationId,
            VerificationActionRequest request) {

        requireRequest(request);

        SchemeApplication application =
                requireApplication(applicationId);

        requireStatusOneOf(
                application,
                "Finance rejection requires status FIELD_APPROVED or " +
                        "DISTRICT_APPROVED. Current status: " +
                        application.getApplicationStatus(),
                STATUS_FIELD_APPROVED,
                STATUS_DISTRICT_APPROVED
        );

        Officer officer =
                requireOfficer(
                        request.getPerformedBy()
                );

        requireRole(
                officer,
                OfficerRole.FINANCE_APPROVER,
                "Only a Finance Approver can reject at Finance level."
        );

        requireRemarks(
                request,
                "Finance rejection requires a reason in the remarks field."
        );

        updateStatus(
                application,
                STATUS_REJECTED
        );

        recordAction(
                application,
                VerificationStage.FINANCE,
                VerificationAction.REJECT,
                request.getPerformedBy(),
                request.getRemarks()
        );

        log.info(
                "Finance REJECT: applicationId={}, by={}",
                applicationId,
                request.getPerformedBy()
        );

        return buildResponse(application);
    }

    // ========================================================================
    // CREATE FIELD CRITERIA
    // ========================================================================

    private void createFieldCriteria(SchemeApplication application) {

        Long applicationId = application.getId();

        List<VerificationCriterion> existing =
                verificationCriterionRepository
                        .findBySchemeApplicationIdAndStageOrderByIdAsc(
                                applicationId,
                                VerificationStage.FIELD
                        );

        if (existing != null && !existing.isEmpty()) {
            return;
        }

        List<VerificationCriterion> criteria = new ArrayList<>();

        // 1. Identity Verification
        criteria.add(buildCriterion(
                application,
                VerificationStage.FIELD,
                "IDENTITY_PROOF",
                "Verify the beneficiary identity using the submitted Identity Proof"
        ));

        // 2. Address Verification
        criteria.add(buildCriterion(
                application,
                VerificationStage.FIELD,
                "ADDRESS_PROOF",
                "Verify the beneficiary address using the submitted Address Proof"
        ));

        // 3. Document Completeness
        criteria.add(buildCriterion(
                application,
                VerificationStage.FIELD,
                "DOCUMENT_COMPLETENESS",
                "Review all supporting documents submitted with the application"
        ));

        verificationCriterionRepository.saveAll(criteria);

        log.info(
                "Created {} FIELD criteria for applicationId={}",
                criteria.size(),
                applicationId
        );
    }

    // ========================================================================
    // CREATE DISTRICT CRITERIA
    // ========================================================================

    private void createDistrictCriteria(
            SchemeApplication application) {

        Long applicationId = application.getId();

        List<VerificationCriterion> existing =
                verificationCriterionRepository
                        .findBySchemeApplicationIdAndStageOrderByIdAsc(
                                applicationId,
                                VerificationStage.DISTRICT
                        );

        if (existing != null && !existing.isEmpty()) {
            return;
        }

        List<VerificationCriterion> criteria = new ArrayList<>();

        criteria.add(buildCriterion(
                application,
                VerificationStage.DISTRICT,
                "JURISDICTION",
                "Verify district jurisdiction"
        ));

        criteria.add(buildCriterion(
                application,
                VerificationStage.DISTRICT,
                "FIELD_REPORT_REVIEW",
                "Review Field Officer report and visit details"
        ));

        criteria.add(buildCriterion(
                application,
                VerificationStage.DISTRICT,
                "SCHEME_ELIGIBILITY",
                "Verify scheme eligibility and supporting documents"
        ));

        criteria.add(buildCriterion(
                application,
                VerificationStage.DISTRICT,
                "ANNUAL_INCOME",
                "Verify annual income and income certificate"
        ));

        criteria.add(buildCriterion(
                application,
                VerificationStage.DISTRICT,
                "CATEGORY_VERIFICATION",
                "Verify category and category certificate"
        ));

        criteria.add(buildCriterion(
                application,
                VerificationStage.DISTRICT,
                "AGE_PROOF",
                "Verify age using government identity document"
        ));

        criteria.add(buildCriterion(
                application,
                VerificationStage.DISTRICT,
                "PREVIOUS_SUBSIDY",
                "Check previous subsidy records"
        ));

        criteria.add(buildCriterion(
                application,
                VerificationStage.DISTRICT,
                "DUPLICATE_APPLICATION",
                "Check duplicate applications and beneficiary records"
        ));

        verificationCriterionRepository.saveAll(criteria);

        log.info(
                "Created {} DISTRICT criteria for applicationId={}",
                criteria.size(),
                applicationId
        );
    }

    // ========================================================================
    // CREATE FINANCE CRITERIA
    // ========================================================================

    private void createFinanceCriteria(
            SchemeApplication application) {

        Long applicationId = application.getId();

        List<VerificationCriterion> existing =
                verificationCriterionRepository
                        .findBySchemeApplicationIdAndStageOrderByIdAsc(
                                applicationId,
                                VerificationStage.FINANCE
                        );

        if (existing != null && !existing.isEmpty()) {
            return;
        }

        List<VerificationCriterion> criteria = new ArrayList<>();

        criteria.add(buildCriterion(
                application,
                VerificationStage.FINANCE,
                "DISTRICT_APPROVAL",
                "Verify District Officer approval"
        ));

        criteria.add(buildCriterion(
                application,
                VerificationStage.FINANCE,
                "BANK_OWNERSHIP",
                "Verify bank account ownership using passbook or cancelled cheque"
        ));

        criteria.add(buildCriterion(
                application,
                VerificationStage.FINANCE,
                "ACCOUNT_NUMBER",
                "Verify beneficiary account number"
        ));

        criteria.add(buildCriterion(
                application,
                VerificationStage.FINANCE,
                "IFSC_VERIFICATION",
                "Verify IFSC code using bank document"
        ));

        criteria.add(buildCriterion(
                application,
                VerificationStage.FINANCE,
                "BENEFICIARY_ACCOUNT_MATCH",
                "Verify bank account belongs to beneficiary"
        ));

        criteria.add(buildCriterion(
                application,
                VerificationStage.FINANCE,
                "GRANT_AMOUNT",
                "Verify sanctioned amount and scheme calculation"
        ));

        criteria.add(buildCriterion(
                application,
                VerificationStage.FINANCE,
                "BUDGET_AVAILABILITY",
                "Verify budget availability"
        ));

        criteria.add(buildCriterion(
                application,
                VerificationStage.FINANCE,
                "PREVIOUS_PAYMENT",
                "Check previous payment records"
        ));

        criteria.add(buildCriterion(
                application,
                VerificationStage.FINANCE,
                "PAYMENT_ELIGIBILITY",
                "Verify payment eligibility"
        ));

        verificationCriterionRepository.saveAll(criteria);

        log.info(
                "Created {} FINANCE criteria for applicationId={}",
                criteria.size(),
                applicationId
        );
    }

    // ========================================================================
    // BUILD CRITERION
    // ========================================================================

    private VerificationCriterion buildCriterion(
            SchemeApplication application,
            VerificationStage stage,
            String code,
            String name) {

        return VerificationCriterion.builder()
                .schemeApplication(application)
                .stage(stage)
                .criterionCode(code)
                .criterionName(name)
                .status(VerificationCriterionStatus.PENDING)
                .build();
    }

    // ========================================================================
    // REQUIRED DOCUMENT VALIDATION
    // ========================================================================

    private void validateRequiredProof(
            SchemeApplication application,
            VerificationCriterion criterion) {

        Long beneficiaryId = application.getBeneficiary().getId().longValue();
        String code = criterion.getCriterionCode();
        DocumentType requiredDocument = null;

        switch (code) {
            case "IDENTITY_PROOF":
            case "AGE_PROOF":
                requiredDocument = DocumentType.IDENTITY_PROOF;
                break;

            case "ADDRESS_PROOF":
                requiredDocument = DocumentType.ADDRESS_PROOF;
                break;

            case "INCOME_PROOF":
            case "ANNUAL_INCOME":
                requiredDocument = DocumentType.INCOME_CERTIFICATE;
                break;

            case "CATEGORY_PROOF":
            case "CATEGORY_VERIFICATION":
                requiredDocument = DocumentType.CATEGORY_CERTIFICATE;
                break;

            case "LAND_PROOF":
            case "LAND_OWNERSHIP":
                requiredDocument = DocumentType.LAND_RECORD;
                break;

            case "OCCUPATION_PROOF":
            case "OCCUPATION_VERIFICATION":
                requiredDocument = DocumentType.OCCUPATION_PROOF;
                break;

            case "BANK_OWNERSHIP":
            case "ACCOUNT_NUMBER":
            case "IFSC_VERIFICATION":
            case "BENEFICIARY_ACCOUNT_MATCH":
                requiredDocument = DocumentType.BANK_ACCOUNT_PROOF;
                break;

            default:
                break;
        }

        if (requiredDocument == null) {
            return;
        }

        boolean documentExists =
                documentRepository.existsByBeneficiaryIdAndDocumentType(
                        beneficiaryId,
                        requiredDocument
                );

        if (!documentExists) {
            throw new InvalidVerificationTransitionException(
                    "Cannot verify criterion '"
                            + criterion.getCriterionName()
                            + "'. Required document is missing: "
                            + requiredDocument.name()
            );
        }
    }

    // ========================================================================
    // ENSURE ALL CRITERIA VERIFIED
    // ========================================================================

    private void ensureAllCriteriaVerified(
            Long applicationId,
            VerificationStage stage) {

        List<VerificationCriterion> criteria =
                verificationCriterionRepository
                        .findBySchemeApplicationIdAndStageOrderByIdAsc(
                                applicationId,
                                stage
                        );

        if (criteria == null) {
            criteria = new ArrayList<>();
        }

        // Remove only unwanted legacy FIELD criteria.
        // Keep criteria with missing codes because unit tests may use mocks.
        if (stage == VerificationStage.FIELD) {

            criteria = criteria.stream()
                    .filter(c -> {

                        String code = c.getCriterionCode();
                        String name = c.getCriterionName();

                        if ((code == null || code.isBlank())
                                && (name == null || name.isBlank())) {
                            return true;
                        }

                        String text = (
                                (code == null ? "" : code)
                                        + " "
                                        + (name == null ? "" : name)
                        ).toUpperCase();

                        return !text.contains("INCOME")
                                && !text.contains("CATEGORY")
                                && !text.contains("LAND");
                    })
                    .toList();
        }

        int totalCriteria = criteria.size();

        long verifiedCriteria = criteria.stream()
                .filter(c ->
                        c.getStatus() == VerificationCriterionStatus.VERIFIED
                )
                .count();

        if (verifiedCriteria != totalCriteria || totalCriteria == 0) {

            throw new InvalidVerificationTransitionException(
                    stage.name()
                            + " Officer cannot approve until all verification "
                            + "criteria are VERIFIED. Verified: "
                            + verifiedCriteria
                            + "/"
                            + totalCriteria
            );
        }
    }

    // ========================================================================
    // CRITERION RESPONSE
    // ========================================================================

    private VerificationCriterionResponse toCriterionResponse(
            VerificationCriterion criterion) {

        return VerificationCriterionResponse.builder()
                .id(criterion.getId())
                .stage(criterion.getStage())
                .criterionCode(criterion.getCriterionCode())
                .criterionName(criterion.getCriterionName())
                .status(criterion.getStatus())
                .verifiedBy(criterion.getVerifiedBy())
                .remarks(criterion.getRemarks())
                .verifiedAt(criterion.getVerifiedAt())
                .build();
    }

    // ========================================================================
    // APPLICATION
    // ========================================================================

    private SchemeApplication requireApplication(
            Long applicationId) {

        return applicationRepository
                .findById(applicationId)
                .orElseThrow(() ->
                        new com.dsgp.application.exception.ApplicationException(
                                "Application not found with ID: " +
                                        applicationId
                        )
                );
    }

    // ========================================================================
    // OFFICER
    // ========================================================================

    private Officer requireOfficer(
            String username) {

        if (username == null
                || username.isBlank()) {

            throw new InvalidVerificationTransitionException(
                    "Officer username is required."
            );
        }

        return officerRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new InvalidVerificationTransitionException(
                                "Officer not found: " +
                                        username
                        )
                );
    }

    private void requireRole(
            Officer officer,
            OfficerRole requiredRole,
            String message) {

        if (officer.getRole() != requiredRole) {

            throw new InvalidVerificationTransitionException(
                    message
            );
        }

        if (!officer.isActive()) {

            throw new InvalidVerificationTransitionException(
                    "Officer account is inactive."
            );
        }
    }

    // ========================================================================
    // STATUS HELPERS
    // ========================================================================

    private void requireStatus(
            SchemeApplication application,
            String requiredStatus,
            String message) {

        if (!requiredStatus.equals(
                application.getApplicationStatus()
        )) {

            throw new InvalidVerificationTransitionException(
                    message
            );
        }
    }

    /**
     * Throws {@link InvalidVerificationTransitionException} unless the
     * application's current status matches at least one of the supplied
     * {@code acceptedStatuses}.
     */
    private void requireStatusOneOf(
            SchemeApplication application,
            String message,
            String... acceptedStatuses) {

        String current = application.getApplicationStatus();

        for (String accepted : acceptedStatuses) {
            if (accepted.equals(current)) {
                return;
            }
        }

        throw new InvalidVerificationTransitionException(message);
    }

    private void updateStatus(
            SchemeApplication application,
            String newStatus) {

        application.setApplicationStatus(
                newStatus
        );

        applicationRepository.save(
                application
        );
    }

    // ========================================================================
    // REQUEST VALIDATION
    // ========================================================================

    private void requireRequest(
            VerificationActionRequest request) {

        if (request == null) {

            throw new InvalidVerificationTransitionException(
                    "Verification action request is required."
            );
        }

        if (request.getPerformedBy() == null
                || request.getPerformedBy().isBlank()) {

            throw new InvalidVerificationTransitionException(
                    "performedBy is required."
            );
        }
    }

    private void requireRemarks(
            VerificationActionRequest request,
            String message) {

        if (request == null
                || request.getRemarks() == null
                || request.getRemarks().isBlank()) {

            throw new InvalidVerificationTransitionException(
                    message
            );
        }
    }

    // ========================================================================
    // AUDIT HISTORY
    // ========================================================================

    private void recordAction(
            SchemeApplication application,
            VerificationStage stage,
            VerificationAction action,
            String performedBy,
            String remarks) {

        VerificationRecord record =
                VerificationRecord.builder()
                        .schemeApplication(application)
                        .stage(stage)
                        .actionTaken(action)
                        .performedBy(performedBy)
                        .remarks(remarks)
                        .build();

        verificationRecordRepository.save(
                record
        );
    }

    // ========================================================================
    // RESPONSE
    // ========================================================================

    private VerificationStatusResponse buildResponse(
            SchemeApplication application) {

        List<VerificationRecord> records =
                verificationRecordRepository
                        .findBySchemeApplicationIdOrderByPerformedAtAsc(
                                application.getId()
                        );

        if (records == null) {
            records = List.of();
        }

        List<VerificationHistoryEntry> history =
                records.stream()
                        .map(record ->
                                VerificationHistoryEntry.builder()
                                        .stage(
                                                record.getStage().name()
                                        )
                                        .action(
                                                record.getActionTaken().name()
                                        )
                                        .performedBy(
                                                record.getPerformedBy()
                                        )
                                        .performedAt(
                                                record.getPerformedAt()
                                        )
                                        .remarks(
                                                record.getRemarks()
                                        )
                                        .build()
                        )
                        .toList();

        Beneficiary beneficiary =
                application.getBeneficiary();

        Scheme scheme =
                application.getScheme();

        return VerificationStatusResponse.builder()
                .applicationId(
                        application.getId()
                )
                .beneficiaryId(
                        beneficiary.getId()
                )
                .beneficiaryName(
                        beneficiary.getFullName()
                )
                .schemeId(
                        scheme.getId()
                )
                .schemeName(
                        scheme.getSchemeName()
                )
                .applicationStatus(
                        application.getApplicationStatus()
                )
                .applicationDate(
                        application.getApplicationDate()
                )
                .history(
                        history
                )
                .build();
    }

    // ========================================================================
    // STRING HELPER
    // ========================================================================

    private String coalesce(
            String value,
            String fallback) {

        return value != null && !value.isBlank()
                ? value
                : fallback;
    }
}
