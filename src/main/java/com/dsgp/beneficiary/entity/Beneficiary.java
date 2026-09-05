package com.dsgp.beneficiary.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JPA entity representing a registered beneficiary.
 *
 * <p>The original 7 fields (fullName, govId, contact, email, age, address,
 * schemeName) are preserved for backward compatibility with existing service
 * and controller code.  The extended fields (aadhaarNumber, mobileNumber,
 * firstName/lastName, dateOfBirth, gender, full address components,
 * annualIncome, landHolding, category, registrationStatus,
 * identityVerified) represent the canonical Milestone 1 data model.
 * All extended fields are nullable so that existing records and the current
 * registration flow continue to work while the richer model is phased in.
 */
@Entity
@Table(name = "beneficiary")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ── Original fields (preserved — used by current service/controller) ────

    /** Combined full name — used by the current registration flow. */
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /** Government / Aadhaar ID stored as entered during initial registration. */
    @Column(name = "gov_id", nullable = false, length = 20)
    private String govId;

    /** 10-digit contact number stored during initial registration. */
    @Column(name = "contact", nullable = false, length = 10)
    private String contact;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "age", nullable = false)
    private Integer age;

    /** Flat address string — used by the current registration flow. */
    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "scheme_name", nullable = false, length = 150)
    private String schemeName;

    // ── Extended canonical model fields (Milestone 1 — nullable) ────────────

    /**
     * 12-digit Aadhaar number — the primary government identity anchor.
     * Uniquely identifies the beneficiary across all schemes.
     */
    @Column(name = "aadhaar_number", length = 12, unique = true)
    private String aadhaarNumber;

    /** Validated 10-digit Indian mobile number (must start with 6–9). */
    @Column(name = "mobile_number", length = 10, unique = true)
    private String mobileNumber;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    // ── Structured address fields ────────────────────────────────────────────

    @Column(name = "village", length = 150)
    private String village;

    @Column(name = "taluka", length = 100)
    private String taluka;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "occupation", length = 100)
    private String occupation;

    @Column(name = "pin_code", length = 6)
    private String pinCode;

    // ── Financial eligibility fields ─────────────────────────────────────────

    /** Annual household income in INR — used for income-based eligibility scoring. */
    @Column(name = "annual_income", precision = 15, scale = 2)
    private BigDecimal annualIncome;

    /** Land holding in acres — used for land-based eligibility scoring. */
    @Column(name = "land_holding", precision = 10, scale = 4)
    private BigDecimal landHolding;

    // ── Categorisation ───────────────────────────────────────────────────────

    /**
     * Social category — used for category-based eligibility and subsidy rate
     * determination (General / OBC / SC / ST).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 10)
    private Category category;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    /**
     * Registration lifecycle status.
     * PENDING → identity not yet verified.
     * ACTIVE  → verified and eligible for scheme processing.
     * SUSPENDED → deactivated due to discrepancy or non-compliance.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", length = 15)
    @Builder.Default
    private RegistrationStatus registrationStatus = RegistrationStatus.PENDING;

    /**
     * True once a Field Officer has confirmed the beneficiary's identity
     * documents in person and marked them as verified.
     */
    @Column(name = "identity_verified", nullable = false)
    @Builder.Default
    private boolean identityVerified = false;
}