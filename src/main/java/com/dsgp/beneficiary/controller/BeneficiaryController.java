package com.dsgp.beneficiary.controller;

import com.dsgp.beneficiary.dto.BeneficiaryRegistrationRequest;
import com.dsgp.beneficiary.dto.BeneficiaryResponse;
import com.dsgp.beneficiary.dto.BeneficiaryUpdateRequest;
import com.dsgp.beneficiary.dto.DocumentResponse;
import com.dsgp.beneficiary.entity.DocumentType;
import com.dsgp.beneficiary.service.BeneficiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
 *
 * <p>Milestone 2 additions:
 * <ul>
 *   <li>{@code POST   /beneficiaries/{id}/documents}       — upload a document</li>
 *   <li>{@code GET    /beneficiaries/{id}/documents}       — list documents</li>
 *   <li>{@code PATCH  /beneficiaries/{id}/verify-identity} — Field Officer marks identity verified</li>
 * </ul>
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

    // ── POST /beneficiaries/{id}/documents ──────────────────────────────────

    /**
     * Uploads a document for a beneficiary.
     * Accepts multipart/form-data with the file and documentType parameter.
     *
     * @param id           beneficiary primary key
     * @param file         the file binary
     * @param documentType one of AADHAAR, PAN, LAND_RECORD, INCOME_CERTIFICATE, PHOTO, OTHER
     * @param uploadedBy   officer or beneficiary username performing the upload
     */
    @PostMapping(value = "/{id}/documents", consumes = "multipart/form-data")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam(value = "uploadedBy", required = false) String uploadedBy)
            throws IOException {

        DocumentResponse response = beneficiaryService.uploadDocument(id, file, documentType, uploadedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── GET /beneficiaries/{id}/documents ───────────────────────────────────

    /**
     * Returns all documents uploaded for a beneficiary.
     */
    @GetMapping("/{id}/documents")
    public ResponseEntity<List<DocumentResponse>> getDocuments(
            @PathVariable Integer id) {

        return ResponseEntity.ok(beneficiaryService.getDocuments(id));
    }

    // ── PATCH /beneficiaries/{id}/verify-identity ───────────────────────────

    /**
     * Marks a beneficiary's identity as verified by a Field Officer.
     * A beneficiary must NOT call this on themselves — only officers may.
     *
     * <p>After this call, the eligibility engine's {@code identityCheck}
     * criterion will award 10 points when re-run.
     *
     * @param id          beneficiary primary key
     * @param verifiedBy  the officer's username (request param — no JWT in M2)
     */
    @PatchMapping("/{id}/verify-identity")
    public ResponseEntity<BeneficiaryResponse> verifyIdentity(
            @PathVariable Integer id,
            @RequestParam("verifiedBy") String verifiedBy) {

        return ResponseEntity.ok(beneficiaryService.verifyIdentity(id, verifiedBy));
    }
}