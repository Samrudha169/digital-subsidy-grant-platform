package com.dsgp.scheme.exception;

/**
 * Thrown when a requested {@code Scheme} cannot be found in the database.
 *
 * <p>Mapped to HTTP 404 by {@code GlobalExceptionHandler}.
 */
public class SchemeNotFoundException extends RuntimeException {

    public SchemeNotFoundException(Long id) {
        super("Scheme not found with ID: " + id);
    }

    public SchemeNotFoundException(String message) {
        super(message);
    }
}
