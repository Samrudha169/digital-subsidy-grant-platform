package com.dsgp.beneficiary.service;

import com.dsgp.beneficiary.dto.*;
import com.dsgp.beneficiary.entity.RegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service interface for the Beneficiary Registration module.
 *
 * <p>Defines all business operations for beneficiary lifecycle management:
 * registration, retrieval, update, status changes, and document uploads.
 *
 * <p>Implemented by {@link BeneficiaryServiceImpl}.
 */
public interface BeneficiaryService {

    /**
     * Registers a new beneficiary. Validates uniqueness of Aadhaar and mobile number.
     *
     * @param request the registration request DTO
     * @return the full response DTO for the newly registered beneficiary
     * @throws com.dsgp.beneficiary.exception.DuplicateAadhaarException if Aadhaar already exists
     * @throws com.dsgp.beneficiary.exception.DuplicateMobileException  if mobile already exists
     */
    BeneficiaryResponse registerBeneficiary(BeneficiaryRegistrationRequest request);

    /**
     * Retrieves a beneficiary by their primary key.
     *
     * @param id the beneficiary ID
     * @return the full response DTO
     * @throws com.dsgp.beneficiary.exception.BeneficiaryNotFoundException if not found
     */
    BeneficiaryResponse getBeneficiaryById(Long id);

    /**
     * Retrieves a beneficiary by their 12-digit Aadhaar number.
     *
     * @param aadhaarNumber the Aadhaar number to search for
     * @return the full response DTO
     * @throws com.dsgp.beneficiary.exception.BeneficiaryNotFoundException if not found
     */
    BeneficiaryResponse getBeneficiaryByAadhaar(String aadhaarNumber);

    /**
     * Returns a paginated list of all beneficiaries as lightweight summaries.
     *
     * @param pageable pagination and sort parameters
     * @return a page of {@link BeneficiarySummaryResponse}
     */
    Page<BeneficiarySummaryResponse> getAllBeneficiaries(Pageable pageable);

    /**
     * Returns a paginated list of beneficiaries filtered by district.
     *
     * @param district the district name
     * @param pageable pagination and sort parameters
     * @return a page of {@link BeneficiarySummaryResponse}
     */
    Page<BeneficiarySummaryResponse> getBeneficiariesByDistrict(String district, Pageable pageable);

    /**
     * Updates an existing beneficiary's details. Aadhaar number cannot be changed.
     *
     * @param id      the beneficiary ID
     * @param request the update request DTO (all fields optional)
     * @return the updated full response DTO
     * @throws com.dsgp.beneficiary.exception.BeneficiaryNotFoundException if not found
     * @throws com.dsgp.beneficiary.exception.DuplicateMobileException     if new mobile already used
     */
    BeneficiaryResponse updateBeneficiary(Long id, BeneficiaryUpdateRequest request);

    /**
     * Changes the registration status of a beneficiary
     * (e.g., PENDING → ACTIVE, ACTIVE → SUSPENDED).
     *
     * @param id     the beneficiary ID
     * @param status the new status
     * @return the updated full response DTO
     * @throws com.dsgp.beneficiary.exception.BeneficiaryNotFoundException if not found
     */
    BeneficiaryResponse updateRegistrationStatus(Long id, RegistrationStatus status);

    /**
     * Uploads a supporting document for a beneficiary and stores it on the filesystem.
     *
     * @param beneficiaryId  the beneficiary ID
     * @param documentType   the document type string (e.g., "AADHAAR", "PAN")
     * @param file           the multipart file to store
     * @param uploadedBy     name/ID of the uploading officer
     * @return the document metadata response DTO
     * @throws com.dsgp.beneficiary.exception.BeneficiaryNotFoundException   if beneficiary not found
     * @throws com.dsgp.beneficiary.exception.InvalidDocumentTypeException   if type string is invalid
     * @throws com.dsgp.beneficiary.exception.DocumentUploadException        if file is invalid or I/O fails
     */
    DocumentResponse uploadDocument(Long beneficiaryId, String documentType,
                                    MultipartFile file, String uploadedBy);

    /**
     * Retrieves all uploaded documents for a beneficiary.
     *
     * @param beneficiaryId the beneficiary ID
     * @return list of document metadata response DTOs
     * @throws com.dsgp.beneficiary.exception.BeneficiaryNotFoundException if not found
     */
    List<DocumentResponse> getDocuments(Long beneficiaryId);

    /**
     * Deactivates a beneficiary by setting status to {@code SUSPENDED}.
     * Does not perform a hard delete.
     *
     * @param id the beneficiary ID
     * @throws com.dsgp.beneficiary.exception.BeneficiaryNotFoundException if not found
     */
    void deleteBeneficiary(Long id);

    /**
     * Marks a beneficiary's identity as verified.
     *
     * <p>Sets {@code identityVerified = true}. Used after a Field Officer
     * has confirmed the beneficiary's identity documents.
     *
     * @param id the beneficiary ID
     * @return the updated full response DTO
     * @throws com.dsgp.beneficiary.exception.BeneficiaryNotFoundException if not found
     */
    BeneficiaryResponse verifyIdentity(Long id);

    /**
     * Returns a paginated list of beneficiaries filtered by registration status.
     *
     * @param status   the registration status to filter by
     * @param pageable pagination and sort parameters
     * @return a page of {@link BeneficiarySummaryResponse}
     */
    Page<BeneficiarySummaryResponse> getBeneficiariesByStatus(RegistrationStatus status, Pageable pageable);
}
