package com.dsgp.verification.exception;

/**
 * Thrown when a verification action is attempted on an application that is
 * not in the correct state for that action (i.e. an invalid state transition).
 *
 * <p>Examples:
 * <ul>
 *   <li>Attempting to run FIELD approval on an already {@code FIELD_APPROVED} application.</li>
 *   <li>Attempting to escalate from DISTRICT stage (escalation is FIELD-only).</li>
 *   <li>Attempting any action on a terminal ({@code APPROVED} or {@code REJECTED}) application.</li>
 * </ul>
 *
 * <p>Mapped to HTTP 400 Bad Request by
 * {@link com.dsgp.config.GlobalExceptionHandler}.
 */
public class InvalidVerificationTransitionException extends RuntimeException {

    public InvalidVerificationTransitionException(String message) {
        super(message);
    }
}
