package com.dsgp.beneficiary.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class BeneficiaryRegistrationRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    @NotBlank(message = "Government ID is required")
    @Size(max = 20, message = "Government ID must not exceed 20 characters")
    private String govId;

    @NotBlank(message = "Contact number is required")
    @Pattern(
            regexp = "\\d{10}",
            message = "Contact number must be exactly 10 digits"
    )
    private String contact;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;

    @NotNull(message = "Age is required")
    @Min(value = 1, message = "Age must be at least 1")
    @Max(value = 120, message = "Age must not exceed 120")
    private Integer age;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @NotBlank(message = "Scheme name is required")
    @Size(max = 150, message = "Scheme name must not exceed 150 characters")
    private String schemeName;
}