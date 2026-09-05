package com.dsgp.verification.service;

import com.dsgp.verification.dto.VerificationActionRequest;
import com.dsgp.verification.dto.VerificationStatusResponse;

/**
 * Service interface for the multi-level verification workflow.
 *
 * <h3>State machine summary</h3>
 * <pre>
 * PENDING          → UNDER_REVIEW      (startVerification)
 * UNDER_REVIEW     → FIELD_APPROVED    (approveAtField)
 * UNDER_REVIEW     → ESCALATED         (escalateAtField)
 * UNDER_REVIEW     → REJECTED          (rejectAtField)
 * FIELD_APPROVED   → APPROVED          (approveAtFinance)
 * FIELD_APPROVED   → REJECTED          (rejectAtFinance)
 * ESCALATED        → DISTRICT_APPROVED (approveAtDistrict)
 * ESCALATED        → REJECTED          (rejectAtDistrict)
 * DISTRICT_APPROVED → APPROVED         (approveAtFinance)
 * DISTRICT_APPROVED → REJECTED         (rejectAtFinance)
 * </pre>
 *
 * <p>Terminal states: {@code APPROVED} and {@code REJECTED}.
 * An Administrator may reopen a rejected application by resetting it to
 * {@code PENDING} (future Milestone 5 feature).
 *
 * <p>Routing: The verification-workflow.md specification states routing is based
 * on the Field Officer's decision (APPROVE vs ESCALATE). Numeric routing thresholds
 * (e.g. by eligibility score or grant amount) are NOT currently specified in the
 * project documentation — see implementation notes in
 * {@link VerificationServiceImpl} for the configurable routing policy.
 *
 * @see com.dsgp.verification.entity.VerificationRecord
 * @see com.dsgp.verification.entity.VerificationStage
 */
public interface VerificationService {

    /**
     * Starts the verification process for a {@code PENDING} application.
     * Moves the application status from {@code PENDING} → {@code UNDER_REVIEW}.
     *
     * @param applicationId primary key of the {@code scheme_applications} row
     * @param request       officer identifier and optional remarks
     * @return the updated application status response
     * @throws com.dsgp.beneficiary.exception.BeneficiaryNotFoundException
     *         if the application does not exist
     * @throws com.dsgp.verification.exception.InvalidVerificationTransitionException
     *         if the application is not in {@code PENDING} state
     */
    VerificationStatusResponse startVerification(Long applicationId,
                                                 VerificationActionRequest request);

    /**
     * Retrieves the current status and full verification history for an application.
     *
     * @param applicationId primary key of the application
     * @return status response with history
     */
    VerificationStatusResponse getStatus(Long applicationId);

    // ── Field Officer actions ─────────────────────────────────────────────────

    /**
     * Field Officer approves the application.
     * UNDER_REVIEW → FIELD_APPROVED.
     *
     * @param applicationId target application
     * @param request       officer identifier + optional remarks
     */
    VerificationStatusResponse approveAtField(Long applicationId,
                                              VerificationActionRequest request);

    /**
     * Field Officer rejects the application (terminal).
     * UNDER_REVIEW → REJECTED.
     * Remarks are mandatory for rejections.
     *
     * @param applicationId target application
     * @param request       officer identifier + mandatory rejection remarks
     */
    VerificationStatusResponse rejectAtField(Long applicationId,
                                             VerificationActionRequest request);

    /**
     * Field Officer escalates the application to the District Officer.
     * UNDER_REVIEW → ESCALATED.
     *
     * @param applicationId target application
     * @param request       officer identifier + escalation reason
     */
    VerificationStatusResponse escalateAtField(Long applicationId,
                                               VerificationActionRequest request);

    // ── District Officer actions ──────────────────────────────────────────────

    /**
     * District Officer approves the escalated application.
     * ESCALATED → DISTRICT_APPROVED.
     *
     * @param applicationId target application
     * @param request       officer identifier + optional remarks
     */
    VerificationStatusResponse approveAtDistrict(Long applicationId,
                                                 VerificationActionRequest request);

    /**
     * District Officer rejects the application (terminal).
     * ESCALATED → REJECTED.
     *
     * @param applicationId target application
     * @param request       officer identifier + mandatory rejection remarks
     */
    VerificationStatusResponse rejectAtDistrict(Long applicationId,
                                                VerificationActionRequest request);

    // ── Finance Approver actions ──────────────────────────────────────────────

    /**
     * Finance Approver grants final approval.
     * FIELD_APPROVED or DISTRICT_APPROVED → APPROVED.
     *
     * @param applicationId target application
     * @param request       officer identifier + optional remarks
     */
    VerificationStatusResponse approveAtFinance(Long applicationId,
                                                VerificationActionRequest request);

    /**
     * Finance Approver rejects the application (terminal).
     * FIELD_APPROVED or DISTRICT_APPROVED → REJECTED.
     *
     * @param applicationId target application
     * @param request       officer identifier + mandatory rejection remarks
     */
    VerificationStatusResponse rejectAtFinance(Long applicationId,
                                               VerificationActionRequest request);
}
