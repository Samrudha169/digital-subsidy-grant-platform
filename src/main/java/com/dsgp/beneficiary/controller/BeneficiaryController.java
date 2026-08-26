package com.dsgp.beneficiary.controller;

import com.dsgp.beneficiary.dto.*;
import com.dsgp.beneficiary.entity.RegistrationStatus;
import com.dsgp.beneficiary.service.BeneficiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for the Beneficiary Registration module.
 *
 * <p>Base path: {@code /beneficiaries} (full path: {@code /api/v1/beneficiaries}
 * via the context path configured in {@code application.properties}).
 *
 * <p>Exposes endpoints for:
 * <ul>
 *   <li>Registering and managing beneficiaries (CRUD)</li>
 *   <li>Filtering beneficiaries by district with pagination</li>
 *   <li>Updating registration status</li>
 *   <li>Uploading and listing supporting documents</li>
 * </ul>
 *
 * <p><strong>Security note:</strong> During Module 1, all endpoints are
 * accessible without authentication (permit-all in {@code SecurityConfig}).
 * Role-based access control will be enforced in the Security module phase.
 */
@RestController
@RequestMapping("/beneficiaries")
@RequiredArgsConstructor
@Slf4j
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Registers a new beneficiary.
     *
     * <p>POST /api/v1/beneficiaries
     *
     * @param request validated registration request body
     * @return 201 Created with the full beneficiary response
     */
    @PostMapping
    public ResponseEntity<BeneficiaryResponse> registerBeneficiary(
            @Valid @RequestBody BeneficiaryRegistrationRequest request) {
        log.info("POST /beneficiaries — registering new beneficiary");
        BeneficiaryResponse response = beneficiaryService.registerBeneficiary(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Read Operations ───────────────────────────────────────────────────────

    /**
     * Returns a paginated list of all beneficiaries.
     *
     * <p>GET /api/v1/beneficiaries?page=0&size=20&sortBy=registrationDate&sortDir=desc
     */
    @GetMapping
    public ResponseEntity<Page<BeneficiarySummaryResponse>> getAllBeneficiaries(
            @RequestParam(defaultValue = "0")                int page,
            @RequestParam(defaultValue = "20")               int size,
            @RequestParam(defaultValue = "registrationDate") String sortBy,
            @RequestParam(defaultValue = "desc")             String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(beneficiaryService.getAllBeneficiaries(pageable));
    }

    /**
     * Returns the full details of a specific beneficiary by ID.
     *
     * <p>GET /api/v1/beneficiaries/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BeneficiaryResponse> getBeneficiaryById(@PathVariable Long id) {
        return ResponseEntity.ok(beneficiaryService.getBeneficiaryById(id));
    }

    /**
     * Returns the full details of a specific beneficiary by Aadhaar number.
     *
     * <p>GET /api/v1/beneficiaries/aadhaar/{aadhaar}
     */
    @GetMapping("/aadhaar/{aadhaar}")
    public ResponseEntity<BeneficiaryResponse> getBeneficiaryByAadhaar(
            @PathVariable String aadhaar) {
        return ResponseEntity.ok(beneficiaryService.getBeneficiaryByAadhaar(aadhaar));
    }

    /**
     * Returns a paginated list of beneficiaries belonging to the given district.
     *
     * <p>GET /api/v1/beneficiaries/district/{district}?page=0&size=20
     */
    @GetMapping("/district/{district}")
    public ResponseEntity<Page<BeneficiarySummaryResponse>> getBeneficiariesByDistrict(
            @PathVariable String district,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(beneficiaryService.getBeneficiariesByDistrict(district, pageable));
    }

    // ── Update Operations ─────────────────────────────────────────────────────

    /**
     * Updates an existing beneficiary's details. Aadhaar cannot be changed.
     *
     * <p>PUT /api/v1/beneficiaries/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<BeneficiaryResponse> updateBeneficiary(
            @PathVariable Long id,
            @Valid @RequestBody BeneficiaryUpdateRequest request) {
        return ResponseEntity.ok(beneficiaryService.updateBeneficiary(id, request));
    }

    /**
     * Updates the registration status of a beneficiary.
     *
     * <p>PATCH /api/v1/beneficiaries/{id}/status?status=ACTIVE
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<BeneficiaryResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam RegistrationStatus status) {
        return ResponseEntity.ok(beneficiaryService.updateRegistrationStatus(id, status));
    }

    /**
     * Deactivates a beneficiary (sets status to SUSPENDED). No hard delete.
     *
     * <p>DELETE /api/v1/beneficiaries/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBeneficiary(@PathVariable Long id) {
        beneficiaryService.deleteBeneficiary(id);
        return ResponseEntity.noContent().build();
    }

    // ── Document Operations ───────────────────────────────────────────────────

    /**
     * Uploads a supporting document for a beneficiary.
     *
     * <p>POST /api/v1/beneficiaries/{id}/documents
     * Content-Type: multipart/form-data
     *
     * @param id           the beneficiary ID
     * @param documentType the document type string (AADHAAR, PAN, LAND_RECORD, etc.)
     * @param file         the binary file (max 5MB; PDF, JPEG, or PNG)
     * @param uploadedBy   optional name/ID of the uploading officer
     * @return 201 Created with document metadata
     */
    @PostMapping("/{id}/documents")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @PathVariable Long id,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploadedBy", required = false) String uploadedBy) {
        DocumentResponse response = beneficiaryService.uploadDocument(id, documentType, file, uploadedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Returns all uploaded document metadata for a beneficiary.
     *
     * <p>GET /api/v1/beneficiaries/{id}/documents
     */
    @GetMapping("/{id}/documents")
    public ResponseEntity<List<DocumentResponse>> getDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(beneficiaryService.getDocuments(id));
    }

    // ── Identity Verification ────────────────────────────────────────────────

    /**
     * Marks a beneficiary's identity as verified.
     *
     * <p>PATCH /api/v1/beneficiaries/{id}/verify
     *
     * <p>Sets {@code identityVerified = true}. Intended for use after a
     * Field Officer has physically confirmed the beneficiary's identity documents.
     *
     * @param id the beneficiary ID
     * @return 200 OK with the updated full beneficiary response
     */
    @PatchMapping("/{id}/verify")
    public ResponseEntity<BeneficiaryResponse> verifyIdentity(@PathVariable Long id) {
        log.info("PATCH /beneficiaries/{}/verify — marking identity as verified", id);
        return ResponseEntity.ok(beneficiaryService.verifyIdentity(id));
    }

    // ── Status Filter ────────────────────────────────────────────────────────

    /**
     * Returns a paginated list of beneficiaries filtered by registration status.
     *
     * <p>GET /api/v1/beneficiaries/status/{status}?page=0&size=20
     *
     * @param status  the registration status (PENDING, ACTIVE, SUSPENDED)
     * @param page    zero-based page index (default 0)
     * @param size    page size (default 20)
     * @return 200 OK with a page of summary responses
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<BeneficiarySummaryResponse>> getBeneficiariesByStatus(
            @PathVariable RegistrationStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("registrationDate").descending());
        return ResponseEntity.ok(beneficiaryService.getBeneficiariesByStatus(status, pageable));
    }
}
