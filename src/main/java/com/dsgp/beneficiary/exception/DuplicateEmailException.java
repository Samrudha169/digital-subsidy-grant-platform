package com.dsgp.beneficiary.exception;

/**
 * Thrown when attempting to register a beneficiary whose email address
 * is already associated with an existing registration.
 *
 * <p>Mapped to HTTP 409 Conflict by {@code GlobalExceptionHandler}.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("An account with this email address is already registered. "
                + "Please log in or use a different email.");
    }
}
