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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        // Move to District Officer level
        // ------------------------------------------------------------

        updateStatus(
                application,
                STATUS_FIELD_APPROVED
        );

        recordAction(
                application,
                VerificationStage.FIELD,
                VerificationAction.APPROVE,
                request.getPerformedBy(),
                coalesce(
                        request.getRemarks(),
                        "All Field verification criteria verified. " +
                                "Application approved at Field level."
                )
        );

        log.info(
                "Field APPROVE: applicationId={}, by={}",
                applicationId,
                request.getPerformedBy()
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

        updateStatus(
                application,
                STATUS_FIELD_APPROVED
        );

        recordAction(
                application,
                VerificationStage.FIELD,
                VerificationAction.APPROVE,
                performedBy,
                coalesce(
                        remarks,
                        "Field verification completed."
                )
        );
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
        // Create District criteria automatically after Field approval
        // ------------------------------------------------------------

        if (stage == VerificationStage.DISTRICT
                && STATUS_FIELD_APPROVED.equals(
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
        // Create Finance criteria automatically after District approval
        // ------------------------------------------------------------

        if (stage == VerificationStage.FINANCE
                && STATUS_DISTRICT_APPROVED.equals(
                application.getApplicationStatus())
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
                STATUS_FIELD_APPROVED,
                "District approval requires status FIELD_APPROVED. " +
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
                STATUS_FIELD_APPROVED,
                "District rejection requires status FIELD_APPROVED. " +
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
         * Finance receives the application only after
         * District approval.
         */
        requireStatus(
                application,
                STATUS_DISTRICT_APPROVED,
                "Finance approval requires status DISTRICT_APPROVED. " +
                        "Current status: " +
                        application.getApplicationStatus()
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
        // All Finance criteria must be VERIFIED
        // ------------------------------------------------------------

        ensureAllCriteriaVerified(
                applicationId,
                VerificationStage.FINANCE
        );

        // ------------------------------------------------------------
        // Final approval
        // ------------------------------------------------------------

        updateStatus(
                application,
                STATUS_APPROVED
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

        requireStatus(
                application,
                STATUS_DISTRICT_APPROVED,
                "Finance rejection requires status DISTRICT_APPROVED. " +
                        "Current status: " +
                        application.getApplicationStatus()
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

    private void createFieldCriteria(
            SchemeApplication application) {

        Long applicationId =
                application.getId();

        List<VerificationCriterion> existing =
                verificationCriterionRepository
                        .findBySchemeApplicationIdAndStageOrderByIdAsc(
                                applicationId,
                                VerificationStage.FIELD
                        );

        if (existing != null && !existing.isEmpty()) {
            return;
        }

        /*
         * Field Officer has exactly 6 criteria.
         *
         * These are the criteria already used in the
         * current application workflow.
         */

        List<VerificationCriterion> criteria =
                new ArrayList<>();

        criteria.add(
                buildCriterion(
                        application,
                        VerificationStage.FIELD,
                        "IDENTITY_ADDRESS",
                        "Identity & Address Verification"
                )
        );

        criteria.add(
                buildCriterion(
                        application,
                        VerificationStage.FIELD,
                        "INCOME",
                        "Income document verified"
                )
        );

        if ("PM-KISAN".equalsIgnoreCase(
                application.getScheme().getSchemeName())) {

            criteria.add(
                    buildCriterion(
                            application,
                            VerificationStage.FIELD,
                            "LAND",
                            "Land ownership/holding verified"
                    )
            );
        }

        String occupation = application.getBeneficiary().getOccupation();

        if (occupation != null &&
                (occupation.equalsIgnoreCase("Farmer")
                        || occupation.equalsIgnoreCase("Business Owner")
                        || occupation.equalsIgnoreCase("Self Employed"))) {

            criteria.add(
                    buildCriterion(
                            application,
                            VerificationStage.FIELD,
                            "LAND",
                            "Land ownership/holding verified"
                    )
            );
        }

        criteria.add(
                buildCriterion(
                        application,
                        VerificationStage.FIELD,
                        "CATEGORY",
                        "Category certificate/details verified"
                )
        );

        criteria.add(
                buildCriterion(
                        application,
                        VerificationStage.FIELD,
                        "DOCUMENTS",
                        "Required Documents Complete"
                )
        );

        verificationCriterionRepository.saveAll(
                criteria
        );

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

        Long applicationId =
                application.getId();

        List<VerificationCriterion> existing =
                verificationCriterionRepository
                        .findBySchemeApplicationIdAndStageOrderByIdAsc(
                                applicationId,
                                VerificationStage.DISTRICT
                        );

        if (existing != null && !existing.isEmpty()) {
            return;
        }

        /*
         * District Officer has criteria that are different
         * from Field Officer criteria.
         */

        List<VerificationCriterion> criteria =
                new ArrayList<>();

        criteria.add(
                buildCriterion(
                        application,
                        VerificationStage.DISTRICT,
                        "JURISDICTION",
                        "District Jurisdiction Verified"
                )
        );

        criteria.add(
                buildCriterion(
                        application,
                        VerificationStage.DISTRICT,
                        "FIELD_REVIEW",
                        "Field Verification Review"
                )
        );

        criteria.add(
                buildCriterion(
                        application,
                        VerificationStage.DISTRICT,
                        "SCHEME_REVIEW",
                        "Scheme and Application Details Verified"
                )
        );

        verificationCriterionRepository.saveAll(
                criteria
        );

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

        Long applicationId =
                application.getId();

        List<VerificationCriterion> existing =
                verificationCriterionRepository
                        .findBySchemeApplicationIdAndStageOrderByIdAsc(
                                applicationId,
                                VerificationStage.FINANCE
                        );

        if (existing != null && !existing.isEmpty()) {
            return;
        }

        /*
         * Finance Approver has criteria that are different
         * from Field and District criteria.
         */

        List<VerificationCriterion> criteria =
                new ArrayList<>();

        criteria.add(
                buildCriterion(
                        application,
                        VerificationStage.FINANCE,
                        "DISTRICT_APPROVAL",
                        "District Approval Verified"
                )
        );

        criteria.add(
                buildCriterion(
                        application,
                        VerificationStage.FINANCE,
                        "GRANT_AMOUNT",
                        "Grant Amount Verified"
                )
        );

        criteria.add(
                buildCriterion(
                        application,
                        VerificationStage.FINANCE,
                        "PAYMENT_DETAILS",
                        "Payment Details Verified"
                )
        );

        verificationCriterionRepository.saveAll(
                criteria
        );

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

        if (criteria == null || criteria.isEmpty()) {

            throw new InvalidVerificationTransitionException(
                    "No " +
                            stage.name() +
                            " verification criteria have been created."
            );
        }

        List<VerificationCriterion> notVerified =
                criteria.stream()
                        .filter(c ->
                                c.getStatus()
                                        != VerificationCriterionStatus.VERIFIED
                        )
                        .toList();

        if (!notVerified.isEmpty()) {

            throw new InvalidVerificationTransitionException(
                    stage.name() +
                            " Officer cannot approve until all verification " +
                            "criteria are VERIFIED. Verified: " +
                            (criteria.size() - notVerified.size()) +
                            "/" +
                            criteria.size()
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