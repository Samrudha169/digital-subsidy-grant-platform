package com.dsgp.beneficiary.exception;

/**
 * Thrown when attempting to register a beneficiary whose mobile number
 * is already associated with an existing registration.
 *
 * <p>Mapped to HTTP 409 Conflict by {@code GlobalExceptionHandler}.
 */
public class DuplicateMobileException extends RuntimeException {

    public DuplicateMobileException(String mobileNumber) {
        super("A beneficiary with mobile number '" + mobileNumber + "' is already registered");
    }
}
