package com.dsgp.beneficiary.exception;

/**
 * Thrown when a document upload operation fails — either due to an
 * invalid/unsupported file, an empty file, or a filesystem I/O error.
 *
 * <p>Mapped to HTTP 400 Bad Request or 500 Internal Server Error
 * depending on the root cause, by {@code GlobalExceptionHandler}.
 */
public class DocumentUploadException extends RuntimeException {

    public DocumentUploadException(String message) {
        super(message);
    }

    public DocumentUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
