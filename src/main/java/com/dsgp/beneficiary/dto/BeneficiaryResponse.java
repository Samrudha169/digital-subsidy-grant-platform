package com.dsgp.beneficiary.dto;

import com.dsgp.beneficiary.entity.Category;
import com.dsgp.beneficiary.entity.Gender;
import com.dsgp.beneficiary.entity.RegistrationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Full response DTO returned after beneficiary registration, retrieval,
 * or update operations.
 *
 * <p>Contains all beneficiary fields. Sensitive data (Aadhaar) is included
 * here; masking should be applied at the API gateway or frontend layer
 * if needed for display purposes.
 */
@Data
@Builder
public class BeneficiaryResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String aadhaarNumber;
    private String mobileNumber;
    private String email;
    private String address;
    private String village;
    private String taluka;
    private String district;
    private String state;
    private String pinCode;
    private BigDecimal annualIncome;
    private BigDecimal landHolding;
    private Category category;
    private RegistrationStatus registrationStatus;
    private LocalDateTime registrationDate;
    private String createdBy;
    private LocalDateTime updatedAt;
    private boolean identityVerified;
}
