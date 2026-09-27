package com.dsgp.beneficiary.service;

import com.dsgp.beneficiary.dto.BeneficiaryRegistrationRequest;
import com.dsgp.beneficiary.dto.BeneficiaryResponse;
import com.dsgp.beneficiary.dto.BeneficiaryUpdateRequest;
import com.dsgp.beneficiary.dto.DocumentResponse;
import com.dsgp.beneficiary.entity.DocumentType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface BeneficiaryService {

    // ============================================================
    // BENEFICIARY
    // ============================================================

    BeneficiaryResponse registerBeneficiary(
            BeneficiaryRegistrationRequest request
    );

    BeneficiaryResponse getBeneficiaryById(
            Integer id
    );

    BeneficiaryResponse getBeneficiaryByGovId(
            String govId
    );

    List<BeneficiaryResponse> getAllBeneficiaries();

    BeneficiaryResponse updateBeneficiary(
            Integer id,
            BeneficiaryRegistrationRequest request
    );

    BeneficiaryResponse updateBeneficiary(
            Integer id,
            BeneficiaryUpdateRequest request
    );

    void deleteBeneficiary(
            Integer id
    );

    // ============================================================
    // DOCUMENT UPLOAD
    // ============================================================

    /*
     * Existing document upload method.
     *
     * This is kept so existing code continues to work.
     */
    DocumentResponse uploadDocument(
            Integer beneficiaryId,
            MultipartFile file,
            DocumentType documentType,
            String uploadedBy
    ) throws IOException;


    /*
     * NEW:
     *
     * Upload a document for a specific application
     * and a specific disbursement stage.
     *
     * Example:
     *
     * applicationId = 42
     * stageNumber = 2
     * documentType = STAGE_2_INVOICE
     */
    DocumentResponse uploadDocument(
            Integer beneficiaryId,
            Long applicationId,
            Integer stageNumber,
            MultipartFile file,
            DocumentType documentType,
            String uploadedBy
    ) throws IOException;


    // ============================================================
    // DOCUMENT RETRIEVAL
    // ============================================================

    List<DocumentResponse> getDocuments(
            Integer beneficiaryId
    );


    /*
     * NEW:
     *
     * Get only the documents belonging to
     * one application and one disbursement stage.
     */
    List<DocumentResponse> getDocumentsByApplicationAndStage(
            Integer beneficiaryId,
            Long applicationId,
            Integer stageNumber
    );


    DocumentResponse getDocumentById(
            Integer beneficiaryId,
            Long documentId
    );


    byte[] downloadDocument(
            Integer beneficiaryId,
            Long documentId
    ) throws IOException;


    // ============================================================
    // IDENTITY VERIFICATION
    // ============================================================

    BeneficiaryResponse verifyIdentity(
            Integer beneficiaryId,
            String verifiedBy
    );
}