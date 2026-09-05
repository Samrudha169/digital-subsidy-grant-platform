package com.dsgp.application.exception;

/**
 * Thrown when an application submission attempt violates a business rule.
 *
 * <p>Covers:
 * <ul>
 *   <li>Beneficiary not yet scored as ELIGIBLE for the requested scheme</li>
 *   <li>Beneficiary scored as INELIGIBLE — submission blocked</li>
 *   <li>Duplicate submission for the same (beneficiary, scheme) pair</li>
 * </ul>
 *
 * <p>Mapped to HTTP 422 Unprocessable Entity by {@code GlobalExceptionHandler}
 * so the caller can distinguish business-rule violations from validation errors
 * (400) and not-found errors (404).
 */
public class ApplicationException extends RuntimeException {

    public ApplicationException(String message) {
        super(message);
    }
}
