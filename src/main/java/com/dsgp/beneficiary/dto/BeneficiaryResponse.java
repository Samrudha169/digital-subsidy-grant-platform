package com.dsgp.beneficiary.dto;

import com.dsgp.beneficiary.entity.Category;
import com.dsgp.beneficiary.entity.Gender;
import com.dsgp.beneficiary.entity.RegistrationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Full beneficiary response DTO returned by all read and write endpoints.
 *
 * <p>The original seven fields (id, fullName, govId, contact, email, age,
 * address, schemeName) are preserved for backward compatibility with
 * Milestone 1 clients.
 *
 * <p>Extended eligibility fields are included and may be {@code null} for
 * records created before the Milestone 2 registration form was deployed.
 * JSON consumers that ignore unknown / null fields will not be affected.
 */
@Data
@Builder
public class BeneficiaryResponse {

    // ── Original fields (Milestone 1 — backward-compatible) ────────────────

    private Integer id;
    private String fullName;
    private String govId;
    private String contact;
    private String email;
    private Integer age;
    private String address;
    private String schemeName;

    // ── Extended identity fields (Milestone 2) ──────────────────────────────

    private String aadhaarNumber;
    private String mobileNumber;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private Gender gender;

    // ── Structured address components ───────────────────────────────────────

    private String village;
    private String taluka;
    private String district;
    private String state;
    private String pinCode;

    // ── Financial eligibility fields ────────────────────────────────────────

    /** Annual household income in INR. */
    private BigDecimal annualIncome;

    /** Land holding in acres. */
    private BigDecimal landHolding;

    // ── Categorisation and lifecycle ────────────────────────────────────────

    private Category category;
    private RegistrationStatus registrationStatus;
    private boolean identityVerified;
}