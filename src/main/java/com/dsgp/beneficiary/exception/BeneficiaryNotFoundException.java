package com.dsgp.beneficiary.exception;

public class BeneficiaryNotFoundException extends RuntimeException {

    public BeneficiaryNotFoundException(Integer id) {
        super("Beneficiary not found with ID: " + id);
    }

    public BeneficiaryNotFoundException(String message) {
        super(message);
    }
}