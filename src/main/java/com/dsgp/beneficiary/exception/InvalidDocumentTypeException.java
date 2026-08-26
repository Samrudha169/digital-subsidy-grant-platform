package com.dsgp.beneficiary.exception;

/**
 * Thrown when an unsupported or unrecognised document type string is
 * provided in the upload request.
 *
 * <p>Mapped to HTTP 400 Bad Request by {@code GlobalExceptionHandler}.
 */
public class InvalidDocumentTypeException extends RuntimeException {

    public InvalidDocumentTypeException(String message) {
        super(message);
    }
}
