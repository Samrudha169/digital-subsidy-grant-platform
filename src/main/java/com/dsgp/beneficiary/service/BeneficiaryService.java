package com.dsgp.beneficiary.service;

import com.dsgp.beneficiary.dto.BeneficiaryRegistrationRequest;
import com.dsgp.beneficiary.dto.BeneficiaryResponse;
import com.dsgp.beneficiary.dto.BeneficiaryUpdateRequest;
import com.dsgp.beneficiary.dto.DocumentResponse;
import com.dsgp.beneficiary.entity.DocumentType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Service contract for beneficiary management operations.
 *
 * <p>Two update overloads are provided:
 * <ul>
 *   <li>{@link #updateBeneficiary(Integer, BeneficiaryRegistrationRequest)} —
 *       retained for Milestone 1 backward compatibility (all legacy fields).</li>
 *   <li>{@link #updateBeneficiary(Integer, BeneficiaryUpdateRequest)} —
 *       preferred Milestone 2 path (optional patch-style update with extended
 *       eligibility fields).</li>
 * </ul>
 */
public interface BeneficiaryService {

    BeneficiaryResponse registerBeneficiary(
            BeneficiaryRegistrationRequest request);

    BeneficiaryResponse getBeneficiaryById(Integer id);

    BeneficiaryResponse getBeneficiaryByGovId(String govId);

    List<BeneficiaryResponse> getAllBeneficiaries();

    /** Milestone 1 update path — kept for backward compatibility. */
    BeneficiaryResponse updateBeneficiary(
            Integer id,
            BeneficiaryRegistrationRequest request);

    /**
     * Milestone 2 update path — patch-style; only non-null fields are applied.
     * Used by {@code PUT /beneficiaries/{id}} from Milestone 2 onward.
     */
    BeneficiaryResponse updateBeneficiary(
            Integer id,
            BeneficiaryUpdateRequest request);

    void deleteBeneficiary(Integer id);

    // ── Milestone 2: Document upload ─────────────────────────────────────────

    /**
     * Stores a document file for a beneficiary and persists the metadata.
     *
     * @param beneficiaryId the beneficiary's primary key
     * @param file          the uploaded file
     * @param documentType  the type of document
     * @param uploadedBy    username of the uploader (officer or beneficiary)
     * @return the persisted document metadata
     */
    DocumentResponse uploadDocument(Integer beneficiaryId,
                                    MultipartFile file,
                                    DocumentType documentType,
                                    String uploadedBy) throws IOException;

    /**
     * Returns all documents uploaded for a beneficiary.
     *
     * @param beneficiaryId the beneficiary's primary key
     * @return list of document metadata (no file binaries)
     */
    List<DocumentResponse> getDocuments(Integer beneficiaryId);

    // ── Milestone 2: Identity verification ───────────────────────────────────

    /**
     * Marks a beneficiary's identity as verified by an authorized Field Officer.
     * Sets {@code identityVerified = true} on the {@link com.dsgp.beneficiary.entity.Beneficiary}.
     *
     * <p>After this call, re-running the eligibility engine will award
     * 10 identity points in the {@code identityCheck} criterion.
     *
     * @param beneficiaryId the beneficiary's primary key
     * @param verifiedBy    the officer's username (audit trail)
     * @return the updated beneficiary response
     */
    BeneficiaryResponse verifyIdentity(Integer beneficiaryId, String verifiedBy);
}