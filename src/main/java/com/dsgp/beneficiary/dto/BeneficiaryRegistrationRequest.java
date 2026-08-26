package com.dsgp.beneficiary.dto;

import com.dsgp.beneficiary.entity.Category;
import com.dsgp.beneficiary.entity.Gender;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for registering a new beneficiary.
 *
 * <p>All required fields are annotated with Bean Validation constraints.
 * The controller applies {@code @Valid} to trigger validation before
 * the service layer is invoked.
 */
@Data
public class BeneficiaryRegistrationRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be a past date")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotBlank(message = "Aadhaar number is required")
    @Pattern(regexp = "\\d{12}", message = "Aadhaar number must be exactly 12 digits")
    private String aadhaarNumber;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "[6-9]\\d{9}", message = "Mobile number must be a valid 10-digit Indian mobile number starting with 6-9")
    private String mobileNumber;

    @Email(message = "Email must be a valid address")
    private String email;

    private String address;

    private String village;

    private String taluka;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "State is required")
    private String state;

    @Pattern(regexp = "\\d{6}", message = "PIN code must be exactly 6 digits")
    private String pinCode;

    @DecimalMin(value = "0.0", inclusive = true, message = "Annual income must be zero or positive")
    private BigDecimal annualIncome;

    @DecimalMin(value = "0.0", inclusive = true, message = "Land holding must be zero or positive")
    private BigDecimal landHolding;

    @NotNull(message = "Category is required")
    private Category category;

    /** Name or ID of the officer performing the registration. */
    private String createdBy;
}
