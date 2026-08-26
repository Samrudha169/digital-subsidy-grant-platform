package com.dsgp.beneficiary.exception;

/**
 * Thrown when a beneficiary cannot be found by the given identifier
 * (ID, Aadhaar number, etc.).
 *
 * <p>Mapped to HTTP 404 Not Found by {@code GlobalExceptionHandler}.
 */
public class BeneficiaryNotFoundException extends RuntimeException {

    public BeneficiaryNotFoundException(String message) {
        super(message);
    }

    public BeneficiaryNotFoundException(Long id) {
        super("Beneficiary not found with ID: " + id);
    }
}
