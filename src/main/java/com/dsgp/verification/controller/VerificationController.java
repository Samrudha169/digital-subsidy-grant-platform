package com.dsgp.verification.controller;

import com.dsgp.verification.dto.VerificationActionRequest;
import com.dsgp.verification.dto.VerificationStatusResponse;
import com.dsgp.verification.service.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.dsgp.verification.dto.VerificationCriterionResponse;
import com.dsgp.verification.dto.VerificationCriterionUpdateRequest;
import com.dsgp.verification.entity.VerificationStage;
import java.util.List;

/**
 * REST controller for the multi-level verification workflow.
 *
 * <p>Base path: {@code /verification}
 * (full path: {@code /api/v1/verification}).
 *
 * <h3>Endpoints</h3>
 * <pre>
 * POST /verification/applications/{id}/start            — Start verification (PENDING → UNDER_REVIEW)
 * GET  /verification/applications/{id}                  — Get status + audit history
 * POST /verification/applications/{id}/field-approve    — Field Officer approve (UNDER_REVIEW → FIELD_APPROVED)
 * POST /verification/applications/{id}/field-reject     — Field Officer reject  (UNDER_REVIEW → REJECTED)
 * POST /verification/applications/{id}/escalate         — Field Officer escalate (UNDER_REVIEW → ESCALATED)
 * POST /verification/applications/{id}/request-correction — Field Officer request correction (UNDER_REVIEW → CORRECTION_REQUIRED)
 * POST /verification/applications/{id}/resubmit         — Beneficiary resubmit (CORRECTION_REQUIRED → UNDER_REVIEW)
 * POST /verification/applications/{id}/district-approve — District Officer approve (ESCALATED → DISTRICT_APPROVED)
 * POST /verification/applications/{id}/district-reject  — District Officer reject  (ESCALATED → REJECTED)
 * POST /verification/applications/{id}/finance-approve  — Finance approve (FIELD_APPROVED|DISTRICT_APPROVED → APPROVED)
 * POST /verification/applications/{id}/finance-reject   — Finance reject  (FIELD_APPROVED|DISTRICT_APPROVED → REJECTED)
 * </pre>
 *
 * <p>All action endpoints return the updated {@link VerificationStatusResponse}
 * with the full audit history. Errors use the standard {@code ApiErrorResponse}
 * shape defined in {@code api-design.md §1.2}.
 */
@RestController
@RequestMapping("/verification/applications")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    // ── Start verification ─────────────────────────────────────────────────

    /**
     * Starts the verification process for a PENDING application.
     * Moves status: PENDING → UNDER_REVIEW.
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<VerificationStatusResponse> startVerification(
            @PathVariable Long id,
            @Valid @RequestBody VerificationActionRequest request) {

        return ResponseEntity.ok(verificationService.startVerification(id, request));
    }

    // ── Get status ────────────────────────────────────────────────────────

    /**
     * Returns the current application status and complete verification history.
     */
    @GetMapping("/{id}")
    public ResponseEntity<VerificationStatusResponse> getStatus(@PathVariable Long id) {
        return ResponseEntity.ok(verificationService.getStatus(id));
    }


    // ── Verification criteria ───────────────────────────────────────────────

    /**
     * Returns verification criteria for the requested application and stage.
     *
     * Example:
     * GET /verification/applications/1/criteria?stage=FIELD
     */
    @GetMapping("/{id}/criteria")
    public ResponseEntity<List<VerificationCriterionResponse>> getCriteria(
            @PathVariable Long id,
            @RequestParam VerificationStage stage) {

        return ResponseEntity.ok(
                verificationService.getCriteria(id, stage)
        );
    }

    /**
     * Updates one verification criterion.
     *
     * Example:
     * PATCH /verification/applications/1/criteria/1
     */
    @PatchMapping("/{id}/criteria/{criterionId}")
    public ResponseEntity<VerificationCriterionResponse> updateCriterion(
            @PathVariable Long id,
            @PathVariable Long criterionId,
            @Valid @RequestBody VerificationCriterionUpdateRequest request) {

        return ResponseEntity.ok(
                verificationService.updateCriterion(id, criterionId, request)
        );
    }

    // ── Field Officer actions ──────────────────────────────────────────────

    /**
     * Field Officer approves: UNDER_REVIEW → FIELD_APPROVED.
     */
    @PostMapping("/{id}/field-approve")
    public ResponseEntity<VerificationStatusResponse> fieldApprove(
            @PathVariable Long id,
            @Valid @RequestBody VerificationActionRequest request) {

        return ResponseEntity.ok(verificationService.approveAtField(id, request));
    }

    /**
     * Field Officer rejects: UNDER_REVIEW → REJECTED. Remarks mandatory.
     */
    @PostMapping("/{id}/field-reject")
    public ResponseEntity<VerificationStatusResponse> fieldReject(
            @PathVariable Long id,
            @Valid @RequestBody VerificationActionRequest request) {

        return ResponseEntity.ok(verificationService.rejectAtField(id, request));
    }

    /**
     * Field Officer escalates: UNDER_REVIEW → ESCALATED.
     */
    @PostMapping("/{id}/escalate")
    public ResponseEntity<VerificationStatusResponse> escalate(
            @PathVariable Long id,
            @Valid @RequestBody VerificationActionRequest request) {

        return ResponseEntity.ok(verificationService.escalateAtField(id, request));
    }

    /**
     * Field Officer requests correction: UNDER_REVIEW → CORRECTION_REQUIRED.
     * Remarks are mandatory and must describe what needs to be corrected.
     */
    @PostMapping("/{id}/request-correction")
    public ResponseEntity<VerificationStatusResponse> requestCorrection(
            @PathVariable Long id,
            @Valid @RequestBody VerificationActionRequest request) {

        return ResponseEntity.ok(verificationService.requestCorrection(id, request));
    }

    /**
     * Beneficiary resubmits after correction: CORRECTION_REQUIRED → UNDER_REVIEW.
     */
    @PostMapping("/{id}/resubmit")
    public ResponseEntity<VerificationStatusResponse> resubmit(
            @PathVariable Long id,
            @Valid @RequestBody VerificationActionRequest request) {

        return ResponseEntity.ok(verificationService.resubmitByBeneficiary(id, request));
    }

    // ── District Officer actions ───────────────────────────────────────────

    /**
     * District Officer approves: ESCALATED → DISTRICT_APPROVED.
     */
    @PostMapping("/{id}/district-approve")
    public ResponseEntity<VerificationStatusResponse> districtApprove(
            @PathVariable Long id,
            @Valid @RequestBody VerificationActionRequest request) {

        return ResponseEntity.ok(verificationService.approveAtDistrict(id, request));
    }

    /**
     * District Officer rejects: ESCALATED → REJECTED. Remarks mandatory.
     */
    @PostMapping("/{id}/district-reject")
    public ResponseEntity<VerificationStatusResponse> districtReject(
            @PathVariable Long id,
            @Valid @RequestBody VerificationActionRequest request) {

        return ResponseEntity.ok(verificationService.rejectAtDistrict(id, request));
    }

    // ── Finance Approver actions ───────────────────────────────────────────

    /**
     * Finance Approver grants final approval: FIELD_APPROVED|DISTRICT_APPROVED → APPROVED.
     */
    @PostMapping("/{id}/finance-approve")
    public ResponseEntity<VerificationStatusResponse> financeApprove(
            @PathVariable Long id,
            @Valid @RequestBody VerificationActionRequest request) {

        return ResponseEntity.ok(verificationService.approveAtFinance(id, request));
    }

    /**
     * Finance Approver rejects: FIELD_APPROVED|DISTRICT_APPROVED → REJECTED. Remarks mandatory.
     */
    @PostMapping("/{id}/finance-reject")
    public ResponseEntity<VerificationStatusResponse> financeReject(
            @PathVariable Long id,
            @Valid @RequestBody VerificationActionRequest request) {

        return ResponseEntity.ok(verificationService.rejectAtFinance(id, request));
    }
}
