package com.dsgp.application.controller;

import com.dsgp.application.dto.ApplicationRequest;
import com.dsgp.application.dto.ApplicationResponse;
import com.dsgp.application.service.ApplicationService;
import com.dsgp.application.service.SchemeApplicationService;
import com.dsgp.application.entity.SchemeApplication;
import com.dsgp.eligibility.entity.EligibilityResult;
import com.dsgp.eligibility.repository.EligibilityResultRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for scheme application submission and retrieval.
 *
 * <p>Base path: {@code /applications} (full path: {@code /api/v1/applications}).
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /applications}       — submit a new application (201 Created)</li>
 *   <li>{@code GET  /applications/{id}}  — get application by ID (200 OK)</li>
 * </ul>
 *
 * <p>GET /applications/{id} returns {@link ApplicationResponse} — NOT the raw
 * {@link SchemeApplication} entity — to avoid Hibernate ByteBuddy proxy
 * serialisation errors caused by LAZY-loaded JPA relationships.
 */
@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final SchemeApplicationService schemeApplicationService;
    private final EligibilityResultRepository eligibilityResultRepository;

    // ── POST /applications ────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApplicationResponse> submitApplication(
            @Valid @RequestBody ApplicationRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.submitApplication(request));
    }

    // ── GET /applications/{applicationId} ─────────────────────────────────────

    /**
     * Returns the current state of an application as an {@link ApplicationResponse}.
     *
     * <p>Uses the {@link SchemeApplicationService} for the entity lookup, then
     * maps to a DTO — accessing only the scalar fields and the already-initialised
     * {@code beneficiary} and {@code scheme} proxies within the same transaction.
     * The {@link EligibilityResult} for the (beneficiary, scheme) pair is fetched
     * separately to populate the real eligibility score; falls back to 0 if no
     * result is found (e.g., data was manually inserted without running the engine).
     *
     * @param applicationId primary key of the {@code scheme_applications} row
     * @return 200 OK with {@link ApplicationResponse}; 404 if not found
     */
    @GetMapping("/{applicationId}")
    public ResponseEntity<ApplicationResponse> getApplicationById(
            @PathVariable Long applicationId) {

        SchemeApplication application =
                schemeApplicationService.getApplicationById(applicationId);

        // Fetch the real eligibility score — 0 if no result exists yet.
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
}