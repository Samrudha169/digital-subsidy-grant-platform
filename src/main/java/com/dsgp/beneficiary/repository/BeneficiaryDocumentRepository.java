package com.dsgp.beneficiary.repository;

import com.dsgp.beneficiary.entity.BeneficiaryDocument;
import com.dsgp.beneficiary.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link BeneficiaryDocument} entities.
 *
 * <p>Provides document lookup operations scoped to a specific beneficiary.
 */
@Repository
public interface BeneficiaryDocumentRepository extends JpaRepository<BeneficiaryDocument, Long> {

    /**
     * Returns all documents uploaded for the given beneficiary.
     *
     * @param beneficiaryId the beneficiary's primary key
     * @return list of all documents for this beneficiary
     */
    List<BeneficiaryDocument> findByBeneficiaryId(Long beneficiaryId);

    /**
     * Finds a specific document type for a beneficiary.
     * Useful for checking whether a required document has already been uploaded.
     *
     * @param beneficiaryId the beneficiary's primary key
     * @param documentType  the type of document to look for
     * @return an {@link Optional} containing the document if it exists
     */
    Optional<BeneficiaryDocument> findByBeneficiaryIdAndDocumentType(
            Long beneficiaryId, DocumentType documentType);

    /**
     * Checks whether a document of the given type already exists for a beneficiary.
     *
     * @param beneficiaryId the beneficiary's primary key
     * @param documentType  the document type
     * @return {@code true} if the document already exists
     */
    boolean existsByBeneficiaryIdAndDocumentType(Long beneficiaryId, DocumentType documentType);
}
