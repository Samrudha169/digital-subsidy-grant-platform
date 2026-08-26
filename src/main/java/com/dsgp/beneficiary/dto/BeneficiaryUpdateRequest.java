package com.dsgp.beneficiary.dto;

import com.dsgp.beneficiary.entity.Category;
import com.dsgp.beneficiary.entity.Gender;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for updating an existing beneficiary's details.
 *
 * <p>All fields are optional — only non-null values are applied in the
 * service layer (partial/patch-style update on a PUT endpoint).
 * Aadhaar number cannot be updated after registration.
 */
@Data
public class BeneficiaryUpdateRequest {

    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Past(message = "Date of birth must be a past date")
    private LocalDate dateOfBirth;

    private Gender gender;

    @Pattern(regexp = "[6-9]\\d{9}", message = "Mobile number must be a valid 10-digit Indian mobile number starting with 6-9")
    private String mobileNumber;

    @Email(message = "Email must be a valid address")
    private String email;

    private String address;

    private String village;

    private String taluka;

    private String district;

    private String state;

    @Pattern(regexp = "\\d{6}", message = "PIN code must be exactly 6 digits")
    private String pinCode;

    @DecimalMin(value = "0.0", inclusive = true, message = "Annual income must be zero or positive")
    private BigDecimal annualIncome;

    @DecimalMin(value = "0.0", inclusive = true, message = "Land holding must be zero or positive")
    private BigDecimal landHolding;

    private Category category;
}
