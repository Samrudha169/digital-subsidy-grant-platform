package com.dsgp.eligibility.exception;

/**
 * Thrown when the requested beneficiary or scheme is not found
 * during an eligibility check.
 *
 * <p>Mapped to HTTP 404 Not Found by {@code GlobalExceptionHandler}.
 */
public class EligibilityCheckException extends RuntimeException {

    public EligibilityCheckException(String message) {
        super(message);
    }
}
