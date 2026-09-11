package com.dsgp.verification.service;

import com.dsgp.verification.dto.VerificationActionRequest;
import com.dsgp.verification.dto.VerificationCriterionResponse;
import com.dsgp.verification.dto.VerificationCriterionUpdateRequest;
import com.dsgp.verification.dto.VerificationStatusResponse;
import com.dsgp.verification.entity.VerificationStage;

import java.util.List;

/**
 * Service interface for the multi-level verification workflow.
 *
 * <h3>State machine</h3>
 *
 * <pre>
 * PENDING
 *     ↓
 * UNDER_REVIEW
 *     ↓
 * ESCALATED
 *     ↓
 * DISTRICT_APPROVED
 *     ↓
 * APPROVED
 *
 * Other possible transitions:
 *
 * UNDER_REVIEW → REJECTED
 * UNDER_REVIEW → CORRECTION_REQUIRED
 * CORRECTION_REQUIRED → UNDER_REVIEW
 * ESCALATED → REJECTED
 * DISTRICT_APPROVED → REJECTED
 * </pre>
 *
 * <p>Field approval is no longer a direct approval action.
 * The Field Officer must verify all Field criteria first and then
 * complete Field verification.</p>
 *
 * <p>District and Finance verification are separate stages and are
 * restricted to their respective officer roles.</p>
 */
public interface VerificationService {

    // ─────────────────────────────────────────────────────────────────────
    // General verification
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Starts Field verification for a PENDING application.
     *
     * <p>PENDING → UNDER_REVIEW</p>
     *
     * <p>This also creates the Field verification criteria for the
     * application if they do not already exist.</p>
     *
     * @param applicationId application primary key
     * @param request officer identifier and optional remarks
     * @return updated verification status
     */
    VerificationStatusResponse startVerification(
            Long applicationId,
            VerificationActionRequest request
    );

    /**
     * Retrieves the current application status and verification history.
     *
     * @param applicationId application primary key
     * @return current status and history
     */
    VerificationStatusResponse getStatus(
            Long applicationId
    );

    // ─────────────────────────────────────────────────────────────────────
    // Field Officer actions
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Legacy Field Officer approval endpoint.
     *
     * <p>Direct Field approval is no longer allowed.
     * Field verification must be completed through the criteria checklist.</p>
     *
     * @param applicationId application primary key
     * @param request officer identifier and remarks
     * @return verification status
     */
    VerificationStatusResponse approveAtField(
            Long applicationId,
            VerificationActionRequest request
    );

    /**
     * Field Officer rejects the application.
     *
     * <p>UNDER_REVIEW → REJECTED</p>
     *
     * @param applicationId application primary key
     * @param request officer identifier and mandatory rejection remarks
     * @return updated verification status
     */
    VerificationStatusResponse rejectAtField(
            Long applicationId,
            VerificationActionRequest request
    );

    /**
     * Legacy Field escalation endpoint.
     *
     * <p>Direct escalation is no longer allowed.
     * The application is automatically routed to the District Officer
     * after all Field criteria have been verified.</p>
     *
     * @param applicationId application primary key
     * @param request officer identifier and remarks
     * @return verification status
     */
    VerificationStatusResponse escalateAtField(
            Long applicationId,
            VerificationActionRequest request
    );

    /**
     * Field Officer requests correction from the beneficiary.
     *
     * <p>UNDER_REVIEW → CORRECTION_REQUIRED</p>
     *
     * @param applicationId application primary key
     * @param request officer identifier and mandatory correction remarks
     * @return updated verification status
     */
    VerificationStatusResponse requestCorrection(
            Long applicationId,
            VerificationActionRequest request
    );

    /**
     * Beneficiary resubmits an application after making corrections.
     *
     * <p>CORRECTION_REQUIRED → UNDER_REVIEW</p>
     *
     * @param applicationId application primary key
     * @param request beneficiary identifier and optional remarks
     * @return updated verification status
     */
    VerificationStatusResponse resubmitByBeneficiary(
            Long applicationId,
            VerificationActionRequest request
    );

    /**
     * Completes Field verification after all Field criteria are verified.
     *
     * <p>UNDER_REVIEW → ESCALATED</p>
     *
     * <p>ESCALATED represents the District Officer queue.</p>
     *
     * @param applicationId application primary key
     * @param performedBy Field Officer username
     * @param remarks optional completion remarks
     */
    void completeFieldVerification(
            Long applicationId,
            String performedBy,
            String remarks
    );

    // ─────────────────────────────────────────────────────────────────────
    // Verification criteria
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Retrieves verification criteria for an application and stage.
     *
     * <p>Example:</p>
     *
     * <pre>
     * GET /verification/applications/1/criteria?stage=FIELD
     * </pre>
     *
     * @param applicationId application primary key
     * @param stage verification stage
     * @return criteria belonging to that stage
     */
    List<VerificationCriterionResponse> getCriteria(
            Long applicationId,
            VerificationStage stage
    );

    /**
     * Updates the status of a verification criterion.
     *
     * <p>The backend verifies that the officer's role matches the
     * criterion's stage.</p>
     *
     * @param applicationId application primary key
     * @param criterionId criterion primary key
     * @param request criterion status, officer and remarks
     * @return updated criterion
     */
    VerificationCriterionResponse updateCriterion(
            Long applicationId,
            Long criterionId,
            VerificationCriterionUpdateRequest request
    );

    // ─────────────────────────────────────────────────────────────────────
    // District Officer actions
    // ─────────────────────────────────────────────────────────────────────

    /**
     * District Officer approves the application after completing
     * District verification criteria.
     *
     * <p>ESCALATED → DISTRICT_APPROVED</p>
     *
     * @param applicationId application primary key
     * @param request District Officer identifier and remarks
     * @return updated verification status
     */
    VerificationStatusResponse approveAtDistrict(
            Long applicationId,
            VerificationActionRequest request
    );

    /**
     * District Officer rejects the application.
     *
     * <p>ESCALATED → REJECTED</p>
     *
     * @param applicationId application primary key
     * @param request District Officer identifier and mandatory remarks
     * @return updated verification status
     */
    VerificationStatusResponse rejectAtDistrict(
            Long applicationId,
            VerificationActionRequest request
    );

    // ─────────────────────────────────────────────────────────────────────
    // Finance Approver actions
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Finance Approver gives final approval.
     *
     * <p>DISTRICT_APPROVED → APPROVED</p>
     *
     * <p>Finance cannot approve directly after Field verification.
     * District approval is mandatory.</p>
     *
     * @param applicationId application primary key
     * @param request Finance Officer identifier and remarks
     * @return final verification status
     */
    VerificationStatusResponse approveAtFinance(
            Long applicationId,
            VerificationActionRequest request
    );

    /**
     * Finance Approver rejects the application.
     *
     * <p>DISTRICT_APPROVED → REJECTED</p>
     *
     * @param applicationId application primary key
     * @param request Finance Officer identifier and mandatory remarks
     * @return updated verification status
     */
    VerificationStatusResponse rejectAtFinance(
            Long applicationId,
            VerificationActionRequest request
    );
}