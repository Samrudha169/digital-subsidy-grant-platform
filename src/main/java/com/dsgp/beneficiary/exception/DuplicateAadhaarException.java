package com.dsgp.beneficiary.exception;

/**
 * Thrown when attempting to register a beneficiary whose Aadhaar number
 * is already associated with an existing registration.
 *
 * <p>Mapped to HTTP 409 Conflict by {@code GlobalExceptionHandler}.
 */
public class DuplicateAadhaarException extends RuntimeException {

    public DuplicateAadhaarException(String aadhaarNumber) {
        super("A beneficiary with Aadhaar number ending in '"
                + maskAadhaar(aadhaarNumber) + "' is already registered");
    }

    private static String maskAadhaar(String aadhaar) {
        if (aadhaar == null || aadhaar.length() < 4) return "****";
        return "XXXX-XXXX-" + aadhaar.substring(aadhaar.length() - 4);
    }
}
