package com.dsgp.disbursement.controller;

import com.dsgp.disbursement.dto.DisbursementStageRequest;
import com.dsgp.disbursement.entity.DisbursementPlan;
import com.dsgp.disbursement.entity.DisbursementStage;
import com.dsgp.disbursement.repository.DisbursementPlanRepository;
import com.dsgp.disbursement.service.DisbursementStageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.dsgp.application.entity.SchemeApplication;
import com.dsgp.beneficiary.repository.SchemeApplicationRepository;
import com.dsgp.disbursement.entity.DisbursementType;
import com.dsgp.disbursement.service.DisbursementPlanService;
import com.dsgp.disbursement.entity.ComplianceStatus;

import java.util.List;


@RestController
@RequestMapping("/api/disbursements")
@RequiredArgsConstructor
public class DisbursementController {

    private final DisbursementStageService disbursementStageService;
    private final DisbursementPlanRepository disbursementPlanRepository;
    private final DisbursementPlanService disbursementPlanService;
    private final SchemeApplicationRepository schemeApplicationRepository;
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
            @RequestParam DisbursementType type) {

        SchemeApplication application =
                schemeApplicationRepository.findById(applicationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Application not found: " + applicationId));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(disbursementPlanService.createPlan(
                        application,
                        type
                ));
    }


    /*
     * Create a new disbursement stage
     */
    @PostMapping("/{planId}/stages")
    public ResponseEntity<DisbursementStage> createStage(
            @PathVariable Long planId,
            @RequestBody DisbursementStageRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(disbursementStageService.createStage(
                        planId,
                        request.getStageNumber(),
                        request.getAmount(),
                        request.getMilestone(),
                        request.getDueDate()
                ));
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
     * Verify a disbursement stage
     */
    @PutMapping("/stages/{stageId}/verify")
    public ResponseEntity<DisbursementStage> verifyStage(
            @PathVariable Long stageId) {

        return ResponseEntity.ok(
                disbursementStageService.verifyStage(stageId)
        );
    }

    /*
     * Release a verified disbursement stage
     */
    @PutMapping("/stages/{stageId}/release")
    public ResponseEntity<DisbursementStage> releaseStage(
            @PathVariable Long stageId) {

        return ResponseEntity.ok(
                disbursementStageService.releaseStage(stageId)
        );
    }

    @PutMapping("/stages/{stageId}/compliance")
    public ResponseEntity<DisbursementStage> updateComplianceStatus(
            @PathVariable Long stageId,
            @RequestParam ComplianceStatus status) {

        return ResponseEntity.ok(
                disbursementStageService.updateComplianceStatus(
                        stageId,
                        status
                )
        );
    }
}