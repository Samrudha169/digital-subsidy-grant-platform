package com.dsgp.beneficiary.dto;

import com.dsgp.beneficiary.entity.Category;
import com.dsgp.beneficiary.entity.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Partial update request for beneficiary profile.
 * Only non-null fields are updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiaryUpdateRequest {

    private String firstName;
    private String lastName;

    private LocalDate dateOfBirth;
    private Gender gender;

    private String mobileNumber;
    private String email;

    private String address;
    private String village;
    private String taluka;
    private String district;
    private String state;
    @Pattern(
            regexp = "^\\d{6}$",
            message = "Pin code must contain exactly 6 digits"
    )
    private String pinCode;

    private BigDecimal annualIncome;
    @DecimalMin(
            value = "0.0",
            inclusive = true,
            message = "Land holding cannot be negative"
    )
    private BigDecimal landHolding;
    private Category category;
    private String occupation;
}
