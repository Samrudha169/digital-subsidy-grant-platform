package com.dsgp.beneficiary.exception;

/**
 * Thrown when attempting to upload a document type that is already on record
 * for the same beneficiary (e.g., a second AADHAAR upload).
 *
 * <p>Mapped to HTTP 409 Conflict by {@code GlobalExceptionHandler}.
 */
public class DuplicateDocumentTypeException extends RuntimeException {

    public DuplicateDocumentTypeException(Long beneficiaryId, String documentType) {
        super("A document of type '" + documentType
                + "' already exists for beneficiary ID " + beneficiaryId
                + ". Delete the existing document before uploading a new one.");
    }
}
