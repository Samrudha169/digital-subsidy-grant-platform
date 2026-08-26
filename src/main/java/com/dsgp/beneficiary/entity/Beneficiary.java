package com.dsgp.beneficiary.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA entity representing a registered beneficiary in the DSGP platform.
 *
 * <p>Maps to the {@code beneficiaries} database table. Stores all personal,
 * demographic, and geographic details required for eligibility scoring,
 * verification, and fund disbursement workflows.
 *
 * <p><strong>Inter-module references:</strong> Other modules (eligibility,
 * verification, disbursement, compliance) reference this entity by
 * {@code id} only — no direct JPA associations cross module boundaries.
 */
@Entity
@Table(
    name = "beneficiaries",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_beneficiary_aadhaar", columnNames = "aadhaar_number")
    }
)
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Personal Details ──────────────────────────────────────────────────────

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    private Gender gender;

    /** Unique 12-digit Aadhaar number — primary identity key. */
    @Column(name = "aadhaar_number", nullable = false, unique = true, length = 12)
    private String aadhaarNumber;

    @Column(name = "mobile_number", nullable = false, length = 10)
    private String mobileNumber;

    @Column(name = "email", length = 150)
    private String email;

    // ── Address Details ───────────────────────────────────────────────────────

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "village", length = 100)
    private String village;

    @Column(name = "taluka", length = 100)
    private String taluka;

    /** District — used by regional analytics and district officer routing. */
    @Column(name = "district", nullable = false, length = 100)
    private String district;

    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "pin_code", length = 6)
    private String pinCode;

    // ── Eligibility Scoring Fields ────────────────────────────────────────────

    /** Annual income in INR — read by the eligibility scoring engine. */
    @Column(name = "annual_income", precision = 15, scale = 2)
    private BigDecimal annualIncome;

    /** Land holding in acres — read by the eligibility scoring engine. */
    @Column(name = "land_holding", precision = 10, scale = 4)
    private BigDecimal landHolding;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private Category category;

    // ── Registration Lifecycle ────────────────────────────────────────────────

    /**
     * Lifecycle status — defaults to {@link RegistrationStatus#PENDING} via {@link #onCreate()}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false, length = 20)
    private RegistrationStatus registrationStatus;

    @Column(name = "registration_date", nullable = false, updatable = false)
    private LocalDateTime registrationDate;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Set to {@code true} after cross-verification with the external
     * Beneficiary Database integration (see {@code com.dsgp.integration.beneficiary}).
     * Defaults to {@code false} — boolean primitive default.
     */
    @Column(name = "identity_verified", nullable = false)
    private boolean identityVerified;

    // ── Lifecycle Callbacks ───────────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        registrationDate = LocalDateTime.now();
        if (registrationStatus == null) {
            registrationStatus = RegistrationStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
