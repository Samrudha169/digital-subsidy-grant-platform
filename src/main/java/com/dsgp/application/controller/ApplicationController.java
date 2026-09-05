package com.dsgp.application.controller;

import com.dsgp.application.dto.ApplicationRequest;
import com.dsgp.application.dto.ApplicationResponse;
import com.dsgp.application.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for scheme application submission.
 *
 * <p>Base path: {@code /applications}
 * (full path: {@code /api/v1/applications}).
 *
 * <p>Milestone 2 — Application Submission phase implements:
 * <ul>
 *   <li>{@code POST /applications} — submit a new application</li>
 * </ul>
 *
 * <p>Application management (list, status queries) and verification routing
 * are implemented in subsequent phases.
 */
@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    // ── POST /applications ───────────────────────────────────────────────────

    /**
     * Submits a new scheme application for a beneficiary.
     *
     * <p>The request must include a valid {@code beneficiaryId} and
     * {@code schemeId}. The service layer enforces:
     * <ul>
     *   <li>Beneficiary exists (404 if not)</li>
     *   <li>Scheme exists (404 if not)</li>
     *   <li>Eligibility has been checked (422 if missing)</li>
     *   <li>Eligibility status is ELIGIBLE (422 if INELIGIBLE)</li>
     *   <li>No duplicate application exists (422 if duplicate)</li>
     * </ul>
     *
     * @param request {@code beneficiaryId} + {@code schemeId}
     * @return 201 Created with the new {@link ApplicationResponse}
     */
    @PostMapping
    public ResponseEntity<ApplicationResponse> submitApplication(
            @Valid @RequestBody ApplicationRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.submitApplication(request));
    }
}
