package com.dsgp.application.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheme_applications")
public class SchemeApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer beneficiaryId;

    private Long schemeId;

    // ── Eligibility and Routing fields ─────────────────────────────

    /**
     * Eligibility score calculated for the beneficiary.
     * Range: 0 - 100
     */
    private Integer eligibilityScore;

    /**
     * Authority to which the application is routed
     * based on eligibility score and grant amount.
     *
     * Possible values:
     * FIELD_OFFICER
     * VERIFYING_OFFICER
     * DISTRICT_OFFICER
     * FINAL_AUTHORITY
     */
    private String routedTo;

    private String status;

    private LocalDateTime submittedAt;

    // ── Verification workflow fields ───────────────────────────────

    /**
     * Current verification level of the application.
     *
     * 1 = Field Officer
     * 2 = Verifying Officer
     * 3 = Final Authority
     */
    private Integer verificationLevel;

    /**
     * Name/ID of the officer who performed
     * the latest verification action.
     */
    private String verifiedBy;

    /**
     * Remarks provided by the verifier.
     */
    @Column(length = 500)
    private String verificationRemarks;

    /**
     * Time when the latest verification action was performed.
     */
    private LocalDateTime verifiedAt;


    // ── Getters and Setters ─────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getBeneficiaryId() {
        return beneficiaryId;
    }

    public void setBeneficiaryId(Integer beneficiaryId) {
        this.beneficiaryId = beneficiaryId;
    }

    public Long getSchemeId() {
        return schemeId;
    }

    public void setSchemeId(Long schemeId) {
        this.schemeId = schemeId;
    }

    public Integer getEligibilityScore() {
        return eligibilityScore;
    }

    public void setEligibilityScore(Integer eligibilityScore) {
        this.eligibilityScore = eligibilityScore;
    }

    public String getRoutedTo() {
        return routedTo;
    }

    public void setRoutedTo(String routedTo) {
        this.routedTo = routedTo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Integer getVerificationLevel() {
        return verificationLevel;
    }

    public void setVerificationLevel(Integer verificationLevel) {
        this.verificationLevel = verificationLevel;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public String getVerificationRemarks() {
        return verificationRemarks;
    }

    public void setVerificationRemarks(String verificationRemarks) {
        this.verificationRemarks = verificationRemarks;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}