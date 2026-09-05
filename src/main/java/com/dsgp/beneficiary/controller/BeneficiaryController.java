package com.dsgp.beneficiary.controller;

import com.dsgp.beneficiary.dto.BeneficiaryRegistrationRequest;
import com.dsgp.beneficiary.dto.BeneficiaryResponse;
import com.dsgp.beneficiary.dto.BeneficiaryUpdateRequest;
import com.dsgp.beneficiary.service.BeneficiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for beneficiary management.
 *
 * <p>Base path: {@code /beneficiaries} (full path: {@code /api/v1/beneficiaries}).
 *
 * <p>Milestone 1 endpoints (POST, GET, DELETE) are preserved unchanged.
 * The PUT endpoint now accepts {@link BeneficiaryUpdateRequest} — a
 * patch-style DTO with optional fields — replacing the previous full-payload
 * requirement. This allows callers to update only the fields they need,
 * including extended eligibility fields.
 */
@RestController
@RequestMapping("/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    // ── POST /beneficiaries ─────────────────────────────────────────────────
    // Milestone 1 behaviour preserved. Request now also accepts optional
    // eligibility fields (annualIncome, landHolding, category, etc.).

    @PostMapping
    public ResponseEntity<BeneficiaryResponse> registerBeneficiary(
            @Valid @RequestBody BeneficiaryRegistrationRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(beneficiaryService.registerBeneficiary(request));
    }

    // ── GET /beneficiaries/{id} ─────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<BeneficiaryResponse> getBeneficiaryById(
            @PathVariable Integer id) {

        return ResponseEntity.ok(beneficiaryService.getBeneficiaryById(id));
    }

    // ── GET /beneficiaries/gov-id/{govId} ──────────────────────────────────

    @GetMapping("/gov-id/{govId}")
    public ResponseEntity<BeneficiaryResponse> getBeneficiaryByGovId(
            @PathVariable String govId) {

        return ResponseEntity.ok(beneficiaryService.getBeneficiaryByGovId(govId));
    }

    // ── GET /beneficiaries ──────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<BeneficiaryResponse>> getAllBeneficiaries() {

        return ResponseEntity.ok(beneficiaryService.getAllBeneficiaries());
    }

    // ── PUT /beneficiaries/{id} ─────────────────────────────────────────────
    // Changed from BeneficiaryRegistrationRequest to BeneficiaryUpdateRequest.
    // All fields optional — only non-null values are applied (patch semantics).

    @PutMapping("/{id}")
    public ResponseEntity<BeneficiaryResponse> updateBeneficiary(
            @PathVariable Integer id,
            @Valid @RequestBody BeneficiaryUpdateRequest request) {

        return ResponseEntity.ok(beneficiaryService.updateBeneficiary(id, request));
    }

    // ── DELETE /beneficiaries/{id} ──────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBeneficiary(
            @PathVariable Integer id) {

        beneficiaryService.deleteBeneficiary(id);
        return ResponseEntity.noContent().build();
    }
}