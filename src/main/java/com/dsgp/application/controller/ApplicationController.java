package com.dsgp.application.controller;

import com.dsgp.application.dto.ApplicationRequest;
import com.dsgp.application.dto.ApplicationResponse;
import com.dsgp.application.service.ApplicationService;
import com.dsgp.application.service.SchemeApplicationService;
import com.dsgp.application.entity.SchemeApplication;
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
 * <p>Base path: {@code /applications} (full path: {@code /api/v1/applications}).
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /applications}                         — submit a new application (201 Created)</li>
 *   <li>{@code GET  /applications/{id}}                    — get application by ID (200 OK)</li>
 *   <li>{@code GET  /applications/{id}/documents}          — documents submitted for an application</li>
 *   <li>{@code GET  /applications/beneficiary/{id}}        — all applications for a beneficiary</li>
 *   <li>{@code GET  /applications?status={status}}         — applications by workflow status</li>
 *   <li>{@code GET  /applications/all}                     — all applications (officer/admin)</li>
 * </ul>
 */
@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final SchemeApplicationService schemeApplicationService;
    private final EligibilityResultRepository eligibilityResultRepository;
    private final BeneficiaryService beneficiaryService;

    // ── POST /applications ──────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApplicationResponse> submitApplication(
            @Valid @RequestBody ApplicationRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.submitApplication(request));
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
                applicationService.getApplicationsByBeneficiary(beneficiaryId));
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
                    applicationService.getApplicationsByStatus(status.toUpperCase()));
        }
        return ResponseEntity.ok(applicationService.getAllApplications());
    }

    // ── GET /applications/all ───────────────────────────────────────────────

    /**
     * Returns all applications. Used by administrator dashboard.
     */
    @GetMapping("/all")
    public ResponseEntity<List<ApplicationResponse>> getAllApplications() {
        return ResponseEntity.ok(applicationService.getAllApplications());
    }

    // ── GET /applications/{applicationId}/documents ──────────────────

    /**
     * Returns all documents submitted for the beneficiary who owns this application.
     * Used by officer dashboards to review supporting documents during verification.
     *
     * <p>Resolves: applicationId → beneficiaryId → documents list.
     *
     * @param applicationId the scheme application primary key
     * @return list of document metadata for the application's beneficiary
     */
    @GetMapping("/{applicationId}/documents")
    public ResponseEntity<List<DocumentResponse>> getApplicationDocuments(
            @PathVariable Long applicationId) {

        SchemeApplication application =
                schemeApplicationService.getApplicationById(applicationId);

        Integer beneficiaryId = application.getBeneficiary().getId();

        return ResponseEntity.ok(beneficiaryService.getDocuments(beneficiaryId));
    }
}