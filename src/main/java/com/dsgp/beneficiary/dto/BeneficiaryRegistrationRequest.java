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
 * <p>The original seven fields (fullName, govId, contact, email, age, address,
 * schemeName) are preserved as mandatory fields for backward compatibility
 * with Milestone 1 clients.
 *
 * <p>The extended eligibility fields (aadhaarNumber, mobileNumber, dateOfBirth,
 * gender, village, taluka, district, state, pinCode, annualIncome, landHolding,
 * category) are <strong>optional</strong>. Callers that omit them will continue
 * to work; the eligibility scoring engine will treat missing values as
 * "criterion not applicable" per the scoring specification.
 */
@Data
public class BeneficiaryRegistrationRequest {

    // ── Original mandatory fields (Milestone 1 — backward-compatible) ──────

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

    // ── Extended eligibility fields (Milestone 2 — all optional/nullable) ──

    /**
     * 12-digit Aadhaar number used as the canonical government identity anchor.
     * Optional at registration — may be provided later via the update endpoint.
     */
    @Pattern(
            regexp = "\\d{12}",
            message = "Aadhaar number must be exactly 12 digits"
    )
    private String aadhaarNumber;

    /**
     * 10-digit Indian mobile number (must start with 6–9).
     * Optional at registration.
     */
    @Pattern(
            regexp = "[6-9]\\d{9}",
            message = "Mobile number must be a valid 10-digit Indian mobile number starting with 6-9"
    )
    private String mobileNumber;

    /** Date of birth. Used to compute age for eligibility scoring when {@code age} is not provided. */
    @Past(message = "Date of birth must be a past date")
    private LocalDate dateOfBirth;

    /** Gender of the beneficiary — used for gender-based scheme eligibility. */
    private Gender gender;

    // ── Structured address components ───────────────────────────────────────

    /** Village name within the beneficiary's residential area. */
    @Size(max = 150, message = "Village must not exceed 150 characters")
    private String village;

    /** Taluka (sub-district administrative unit). */
    @Size(max = 100, message = "Taluka must not exceed 100 characters")
    private String taluka;

    /** District name — used for regional routing of the verification workflow. */
    @Size(max = 100, message = "District must not exceed 100 characters")
    private String district;

    /** State name. */
    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    /** 6-digit PIN code of the residential area. */
    @Pattern(
            regexp = "\\d{6}",
            message = "PIN code must be exactly 6 digits"
    )
    private String pinCode;

    // ── Financial eligibility fields ────────────────────────────────────────

    /**
     * Annual household income in INR.
     * Used by the eligibility scoring engine for income-based criterion.
     */
    @DecimalMin(value = "0.0", inclusive = true,
            message = "Annual income must be zero or positive")
    private BigDecimal annualIncome;

    /**
     * Land holding in acres.
     * Used by the eligibility scoring engine for land-based criterion.
     */
    @DecimalMin(value = "0.0", inclusive = true,
            message = "Land holding must be zero or positive")
    private BigDecimal landHolding;

    // ── Categorisation ──────────────────────────────────────────────────────

    /**
     * Social category (GENERAL / OBC / SC / ST).
     * Used for category-based eligibility and subsidy rate determination.
     */
    private Category category;
}