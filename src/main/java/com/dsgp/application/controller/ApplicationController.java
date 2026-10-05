package com.dsgp.application.controller;

import com.dsgp.application.dto.ApplicationRequest;
import com.dsgp.application.dto.ApplicationResponse;
import com.dsgp.application.entity.SchemeApplication;
import com.dsgp.application.service.ApplicationService;
import com.dsgp.application.service.SchemeApplicationService;
import com.dsgp.audit.service.AuditLogService;
import com.dsgp.beneficiary.dto.DocumentResponse;
import com.dsgp.beneficiary.service.BeneficiaryService;
import com.dsgp.eligibility.entity.EligibilityResult;
import com.dsgp.eligibility.repository.EligibilityResultRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for scheme application submission and retrieval.
 *
 * Base path: /applications
 *
 * Endpoints:
 * POST /applications
 * GET  /applications/{id}
 * GET  /applications/{id}/documents
 * GET  /applications/beneficiary/{id}
 * GET  /applications?status={status}
 * GET  /applications/all
 */
@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final SchemeApplicationService schemeApplicationService;
    private final EligibilityResultRepository eligibilityResultRepository;
    private final BeneficiaryService beneficiaryService;

    /*
     * AuditLogService is optional here because existing controller tests
     * use @WebMvcTest and do not create an AuditLogService bean.
     *
     * In the actual application, Spring will provide the AuditLogService bean.
     */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private AuditLogService auditLogService;

    // ── POST /applications ──────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApplicationResponse> submitApplication(
            @Valid @RequestBody ApplicationRequest request,
            @RequestParam(required = false) Long officerId) {

        ApplicationResponse response =
                applicationService.submitApplication(request);

        /*
         * Audit logging is performed only when an officerId is supplied.
         *
         * This keeps the existing application submission API working while
         * allowing officer actions to be logged when officerId is available.
         */
        if (officerId != null && auditLogService != null) {

            auditLogService.createAuditLog(
                    officerId,
                    "APPLICATION_SUBMITTED",
                    "APPLICATION",
                    response.getApplicationId(),
                    "New scheme application submitted"
            );
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ── GET /applications/{applicationId} ───────────────────────────────────────

    @GetMapping("/{applicationId}")
    public ResponseEntity<ApplicationResponse> getApplicationById(
            @PathVariable Long applicationId) {

        SchemeApplication application =
                schemeApplicationService.getApplicationById(applicationId);

        int eligibilityScore = eligibilityResultRepository
                .findByBeneficiaryIdAndSchemeId(
                        application.getBeneficiary().getId(),
                        application.getScheme().getId())
                .map(EligibilityResult::getTotalScore)
                .orElse(0);

        ApplicationResponse response = ApplicationResponse.builder()
                .applicationId(application.getId())
                .beneficiaryId(application.getBeneficiary().getId())
                .beneficiaryName(application.getBeneficiary().getFullName())
                .schemeId(application.getScheme().getId())
                .schemeName(application.getScheme().getSchemeName())
                .applicationStatus(application.getApplicationStatus())
                .eligibilityScore(eligibilityScore)
                .applicationDate(application.getApplicationDate())
                .sanctionedAmount(application.getSanctionedAmount())
                .build();

        return ResponseEntity.ok(response);
    }

    // ── GET /applications/beneficiary/{beneficiaryId} ─────────────────────────

    /**
     * Returns all applications for a specific beneficiary.
     * Used by the beneficiary's "My Applications" page.
     */
    @GetMapping("/beneficiary/{beneficiaryId}")
    public ResponseEntity<List<ApplicationResponse>> getApplicationsByBeneficiary(
            @PathVariable Integer beneficiaryId) {

        return ResponseEntity.ok(
                applicationService.getApplicationsByBeneficiary(beneficiaryId)
        );
    }

    // ── GET /applications?status=... ─────────────────────────────────────────

    /**
     * Returns all applications with the specified status.
     * Used by officer dashboards to build their work queues.
     */
    @GetMapping
    public ResponseEntity<List<ApplicationResponse>> getApplicationsByStatus(
            @RequestParam(required = false) String status) {

        if (status != null && !status.isBlank()) {

            return ResponseEntity.ok(
                    applicationService.getApplicationsByStatus(
                            status.toUpperCase()
                    )
            );
        }

        return ResponseEntity.ok(
                applicationService.getAllApplications()
        );
    }

    // ── GET /applications/all ───────────────────────────────────────────────

    /**
     * Returns all applications.
     * Used by administrator dashboard.
     */
    @GetMapping("/all")
    public ResponseEntity<List<ApplicationResponse>> getAllApplications() {

        return ResponseEntity.ok(
                applicationService.getAllApplications()
        );
    }

    // ── GET /applications/{applicationId}/documents ──────────────────────────

    /**
     * Returns all documents submitted for the beneficiary
     * who owns this application.
     *
     * Resolves:
     * applicationId → beneficiaryId → documents list
     */
    @GetMapping("/{applicationId}/documents")
    public ResponseEntity<List<DocumentResponse>> getApplicationDocuments(
            @PathVariable Long applicationId) {

        SchemeApplication application =
                schemeApplicationService.getApplicationById(applicationId);

        Integer beneficiaryId =
                application.getBeneficiary().getId();

        return ResponseEntity.ok(
                beneficiaryService.getDocuments(beneficiaryId)
        );
    }
}