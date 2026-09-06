package com.dsgp.beneficiary.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JPA entity representing a registered beneficiary.
 *
 * <p>The original fields are preserved for backward compatibility.
 * Password is stored as a BCrypt-hashed value and is never exposed
 * through BeneficiaryResponse.
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

    // ── Original fields ──────────────────────────────────────────────────────

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "gov_id", nullable = false, length = 20)
    private String govId;

    @Column(name = "contact", nullable = false, length = 10)
    private String contact;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    /**
     * BCrypt-hashed account password.
     * The plain-text password is never stored in the database.
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "age", nullable = false)
    private Integer age;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "scheme_name", nullable = false, length = 150)
    private String schemeName;

    // ── Extended canonical model fields ──────────────────────────────────────

    /**
     * 12-digit Aadhaar number.
     */
    @Column(name = "aadhaar_number", length = 12, unique = true)
    private String aadhaarNumber;

    /**
     * Validated 10-digit Indian mobile number.
     */
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

    @Column(name = "annual_income", precision = 15, scale = 2)
    private BigDecimal annualIncome;

    @Column(name = "land_holding", precision = 10, scale = 4)
    private BigDecimal landHolding;

    // ── Categorisation ───────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 10)
    private Category category;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", length = 15)
    @Builder.Default
    private RegistrationStatus registrationStatus =
            RegistrationStatus.PENDING;

    @Column(name = "identity_verified", nullable = false)
    @Builder.Default
    private boolean identityVerified = false;
}