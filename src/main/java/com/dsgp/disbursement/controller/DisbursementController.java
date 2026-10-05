package com.dsgp.disbursement.controller;

import com.dsgp.application.entity.SchemeApplication;
import com.dsgp.audit.service.AuditLogService;
import com.dsgp.beneficiary.repository.SchemeApplicationRepository;
import com.dsgp.disbursement.dto.DisbursementStageRequest;
import com.dsgp.disbursement.entity.ComplianceStatus;
import com.dsgp.disbursement.entity.DisbursementPlan;
import com.dsgp.disbursement.entity.DisbursementStage;
import com.dsgp.disbursement.entity.DisbursementType;
import com.dsgp.disbursement.repository.DisbursementPlanRepository;
import com.dsgp.disbursement.service.DisbursementPlanService;
import com.dsgp.disbursement.service.DisbursementStageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disbursements")
@RequiredArgsConstructor
public class DisbursementController {

    private final DisbursementStageService disbursementStageService;
    private final DisbursementPlanRepository disbursementPlanRepository;
    private final DisbursementPlanService disbursementPlanService;
    private final SchemeApplicationRepository schemeApplicationRepository;
    private final AuditLogService auditLogService;

    /*
     * Get disbursement plan using application ID
     */
    @GetMapping("/application/{applicationId}/plan")
    public ResponseEntity<DisbursementPlan> getPlanByApplicationId(
            @PathVariable Long applicationId) {

        return disbursementPlanRepository
                .findByApplication_Id(applicationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /*
     * Create disbursement plan for an approved application
     */
    @PostMapping("/application/{applicationId}/plan")
    public ResponseEntity<DisbursementPlan> createPlan(
            @PathVariable Long applicationId,
            @RequestParam DisbursementType type,
            @RequestParam(required = false) Long officerId) {

        SchemeApplication application =
                schemeApplicationRepository.findById(applicationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Application not found: " + applicationId));

        DisbursementPlan plan =
                disbursementPlanService.createPlan(
                        application,
                        type
                );

        /*
         * Create audit log when officerId is available.
         */
        if (officerId != null && auditLogService != null) {

            auditLogService.createAuditLog(
                    officerId,
                    "DISBURSEMENT_PLAN_CREATED",
                    "DISBURSEMENT_PLAN",
                    plan.getId(),
                    "Disbursement plan created"
            );
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(plan);
    }

    /*
     * Create a new disbursement stage
     */
    @PostMapping("/{planId}/stages")
    public ResponseEntity<DisbursementStage> createStage(
            @PathVariable Long planId,
            @RequestBody DisbursementStageRequest request,
            @RequestParam(required = false) Long officerId) {

        DisbursementStage stage =
                disbursementStageService.createStage(
                        planId,
                        request.getStageNumber(),
                        request.getAmount(),
                        request.getMilestone(),
                        request.getDueDate()
                );

        /*
         * Create audit log when officerId is available.
         */
        if (officerId != null && auditLogService != null) {

            auditLogService.createAuditLog(
                    officerId,
                    "DISBURSEMENT_STAGE_CREATED",
                    "DISBURSEMENT_STAGE",
                    stage.getId(),
                    "Disbursement stage created"
            );
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(stage);
    }

    /*
     * Get all stages for a disbursement plan
     */
    @GetMapping("/{planId}/stages")
    public ResponseEntity<List<DisbursementStage>> getStages(
            @PathVariable Long planId) {

        return ResponseEntity.ok(
                disbursementStageService.getStages(planId)
        );
    }

    /*
     * Verify a disbursement stage.
     *
     * Compliance must be COMPLETED before this succeeds.
     */
    @PutMapping("/stages/{stageId}/verify")
    public ResponseEntity<DisbursementStage> verifyStage(
            @PathVariable Long stageId,
            @RequestParam(required = false) Long officerId) {

        DisbursementStage stage =
                disbursementStageService.verifyStage(stageId);

        /*
         * Create audit log when officerId is available.
         */
        if (officerId != null && auditLogService != null) {

            auditLogService.createAuditLog(
                    officerId,
                    "DISBURSEMENT_STAGE_VERIFIED",
                    "DISBURSEMENT_STAGE",
                    stage.getId(),
                    "Disbursement stage verified"
            );
        }

        return ResponseEntity.ok(stage);
    }

    /*
     * Release a verified disbursement stage
     */
    @PutMapping("/stages/{stageId}/release")
    public ResponseEntity<DisbursementStage> releaseStage(
            @PathVariable Long stageId,
            @RequestParam(required = false) Long officerId) {

        DisbursementStage stage =
                disbursementStageService.releaseStage(stageId);

        /*
         * Create audit log when officerId is available.
         */
        if (officerId != null && auditLogService != null) {

            auditLogService.createAuditLog(
                    officerId,
                    "DISBURSEMENT_STAGE_RELEASED",
                    "DISBURSEMENT_STAGE",
                    stage.getId(),
                    "Disbursement stage released"
            );
        }

        return ResponseEntity.ok(stage);
    }

    /*
     * Update compliance status for a disbursement stage.
     *
     * Required:
     * status      = PENDING / COMPLETED / NON_COMPLIANT
     * remarks     = Finance Officer's compliance review remarks
     * verifiedBy  = Finance Officer who performed the review
     */
    @PutMapping("/stages/{stageId}/compliance")
    public ResponseEntity<DisbursementStage> updateComplianceStatus(
            @PathVariable Long stageId,
            @RequestParam ComplianceStatus status,
            @RequestParam String remarks,
            @RequestParam String verifiedBy,
            @RequestParam(required = false) Long officerId) {

        DisbursementStage stage =
                disbursementStageService.updateComplianceStatus(
                        stageId,
                        status,
                        remarks,
                        verifiedBy
                );

        /*
         * Create audit log when officerId is available.
         */
        if (officerId != null && auditLogService != null) {

            auditLogService.createAuditLog(
                    officerId,
                    "COMPLIANCE_STATUS_UPDATED",
                    "DISBURSEMENT_STAGE",
                    stage.getId(),
                    "Compliance status updated to " + status
            );
        }

        return ResponseEntity.ok(stage);
    }
}