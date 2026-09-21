package com.dsgp.beneficiary.controller;

import com.dsgp.beneficiary.dto.BeneficiaryRegistrationRequest;
import com.dsgp.beneficiary.dto.BeneficiaryResponse;
import com.dsgp.beneficiary.dto.BeneficiaryUpdateRequest;
import com.dsgp.beneficiary.dto.DocumentResponse;
import com.dsgp.beneficiary.entity.DocumentType;
import com.dsgp.beneficiary.service.BeneficiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * REST controller for beneficiary management.
 *
 * Base path: /api/v1/beneficiaries
 */
@RestController
@RequestMapping("/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    // ============================================================
    // REGISTER BENEFICIARY
    // ============================================================

    @PostMapping
    public ResponseEntity<BeneficiaryResponse> registerBeneficiary(
            @Valid @RequestBody BeneficiaryRegistrationRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(beneficiaryService.registerBeneficiary(request));
    }

    // ============================================================
    // GET BENEFICIARY BY ID
    // ============================================================

    @GetMapping("/{id}")
    public ResponseEntity<BeneficiaryResponse> getBeneficiaryById(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                beneficiaryService.getBeneficiaryById(id)
        );
    }

    // ============================================================
    // GET BENEFICIARY BY GOVERNMENT ID
    // ============================================================

    @GetMapping("/gov-id/{govId}")
    public ResponseEntity<BeneficiaryResponse> getBeneficiaryByGovId(
            @PathVariable String govId) {

        return ResponseEntity.ok(
                beneficiaryService.getBeneficiaryByGovId(govId)
        );
    }

    // ============================================================
    // GET ALL BENEFICIARIES
    // ============================================================

    @GetMapping
    public ResponseEntity<List<BeneficiaryResponse>> getAllBeneficiaries() {

        return ResponseEntity.ok(
                beneficiaryService.getAllBeneficiaries()
        );
    }

    // ============================================================
    // PUT PROFILE UPDATE
    // ============================================================

    @PutMapping("/{id}")
    public ResponseEntity<BeneficiaryResponse> updateBeneficiary(
            @PathVariable Integer id,
            @Valid @RequestBody BeneficiaryUpdateRequest request) {

        return ResponseEntity.ok(
                beneficiaryService.updateBeneficiary(id, request)
        );
    }

    // ============================================================
    // PATCH PROFILE UPDATE
    // ============================================================

    @PatchMapping("/{id}")
    public ResponseEntity<BeneficiaryResponse> patchBeneficiary(
            @PathVariable Integer id,
            @Valid @RequestBody BeneficiaryUpdateRequest request) {

        return ResponseEntity.ok(
                beneficiaryService.updateBeneficiary(id, request)
        );
    }

    // ============================================================
    // DELETE BENEFICIARY
    // ============================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBeneficiary(
            @PathVariable Integer id) {

        beneficiaryService.deleteBeneficiary(id);

        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // UPLOAD DOCUMENT
    // ============================================================

    @PostMapping(
            value = "/{id}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<DocumentResponse> uploadDocument(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam(value = "uploadedBy", required = false)
            String uploadedBy
    ) throws IOException {

        DocumentResponse response =
                beneficiaryService.uploadDocument(
                        id,
                        file,
                        documentType,
                        uploadedBy
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ============================================================
    // GET BENEFICIARY DOCUMENTS
    // ============================================================

    @GetMapping("/{id}/documents")
    public ResponseEntity<List<DocumentResponse>> getDocuments(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                beneficiaryService.getDocuments(id)
        );
    }

    // ============================================================
    // VERIFY IDENTITY
    // ============================================================

    @PatchMapping("/{id}/verify-identity")
    public ResponseEntity<BeneficiaryResponse> verifyIdentity(
            @PathVariable Integer id,
            @RequestParam("verifiedBy") String verifiedBy) {

        return ResponseEntity.ok(
                beneficiaryService.verifyIdentity(
                        id,
                        verifiedBy
                )
        );
    }

    // ============================================================
    // GET SINGLE DOCUMENT
    // ============================================================

    @GetMapping("/{id}/documents/{documentId}")
    public ResponseEntity<DocumentResponse> getDocumentById(
            @PathVariable Integer id,
            @PathVariable Long documentId) {

        return ResponseEntity.ok(
                beneficiaryService.getDocumentById(
                        id,
                        documentId
                )
        );
    }

    // ============================================================
    // DOWNLOAD DOCUMENT
    // ============================================================

    @GetMapping("/{id}/documents/{documentId}/download")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable Integer id,
            @PathVariable Long documentId
    ) throws IOException {

        DocumentResponse meta =
                beneficiaryService.getDocumentById(
                        id,
                        documentId
                );

        byte[] bytes =
                beneficiaryService.downloadDocument(
                        id,
                        documentId
                );

        String mimeType =
                meta.getMimeType() != null
                        ? meta.getMimeType()
                        : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        MediaType mediaType;

        try {
            mediaType = MediaType.parseMediaType(mimeType);
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        String originalFileName =
                meta.getOriginalFileName() != null
                        ? meta.getOriginalFileName()
                        : "document";

        boolean inline =
                mimeType.startsWith("image/")
                        || mimeType.equals("application/pdf");

        ContentDisposition disposition =
                inline
                        ? ContentDisposition
                        .inline()
                        .filename(originalFileName)
                        .build()
                        : ContentDisposition
                        .attachment()
                        .filename(originalFileName)
                        .build();

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(mediaType);
        headers.setContentDisposition(disposition);
        headers.setContentLength(bytes.length);

        return new ResponseEntity<>(
                bytes,
                headers,
                HttpStatus.OK
        );
    }
}