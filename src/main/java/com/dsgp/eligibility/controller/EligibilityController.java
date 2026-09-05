package com.dsgp.eligibility.controller;

import com.dsgp.eligibility.dto.EligibilityCheckRequest;
import com.dsgp.eligibility.dto.EligibilityResultResponse;
import com.dsgp.eligibility.service.EligibilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for eligibility evaluation operations.
 *
 * <p>Base path: {@code /eligibility}
 * (full path: {@code /api/v1/eligibility}).
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /eligibility/check} — run a new eligibility check</li>
 *   <li>{@code GET  /eligibility/{beneficiaryId}/{schemeId}} — get specific result</li>
 *   <li>{@code GET  /eligibility/beneficiary/{beneficiaryId}} — all results for a beneficiary</li>
 *   <li>{@code GET  /eligibility/scheme/{schemeId}} — all results for a scheme</li>
 * </ul>
 */
@RestController
@RequestMapping("/eligibility")
@RequiredArgsConstructor
public class EligibilityController {

    private final EligibilityService eligibilityService;

    // ── POST /eligibility/check ─────────────────────────────────────────────
    /**
     * Runs the weighted eligibility scoring engine for the given
     * (beneficiary, scheme) pair and persists the result.
     *
     * <p>If a result already exists for this pair it is replaced.
     *
     * @return 200 OK with the full {@link EligibilityResultResponse}
     */
    @PostMapping("/check")
    public ResponseEntity<EligibilityResultResponse> checkEligibility(
            @Valid @RequestBody EligibilityCheckRequest request) {

        return ResponseEntity.ok(eligibilityService.checkEligibility(request));
    }

    // ── GET /eligibility/{beneficiaryId}/{schemeId} ─────────────────────────
    /**
     * Returns the most recent eligibility result for a specific
     * beneficiary + scheme pair.
     */
    @GetMapping("/{beneficiaryId}/{schemeId}")
    public ResponseEntity<EligibilityResultResponse> getResult(
            @PathVariable Integer beneficiaryId,
            @PathVariable Long schemeId) {

        return ResponseEntity.ok(eligibilityService.getResult(beneficiaryId, schemeId));
    }

    // ── GET /eligibility/beneficiary/{beneficiaryId} ────────────────────────
    /**
     * Returns all eligibility results for a given beneficiary across
     * all schemes they have been evaluated against.
     */
    @GetMapping("/beneficiary/{beneficiaryId}")
    public ResponseEntity<List<EligibilityResultResponse>> getResultsForBeneficiary(
            @PathVariable Integer beneficiaryId) {

        return ResponseEntity.ok(eligibilityService.getResultsForBeneficiary(beneficiaryId));
    }

    // ── GET /eligibility/scheme/{schemeId} ──────────────────────────────────
    /**
     * Returns all eligibility results recorded for a given scheme.
     * Used by officers for scheme-level eligibility analysis.
     */
    @GetMapping("/scheme/{schemeId}")
    public ResponseEntity<List<EligibilityResultResponse>> getResultsForScheme(
            @PathVariable Long schemeId) {

        return ResponseEntity.ok(eligibilityService.getResultsForScheme(schemeId));
    }
}
