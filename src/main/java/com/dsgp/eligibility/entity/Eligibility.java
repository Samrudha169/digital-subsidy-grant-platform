package com.dsgp.eligibility.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "eligibility")
public class Eligibility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID of the beneficiary being evaluated
    private Integer beneficiaryId;

    // ID of the scheme for which eligibility is being checked
    private Long schemeId;

    // Calculated eligibility score (0–100)
    private Integer eligibilityScore;

    // Whether the beneficiary is eligible
    private Boolean eligible;

    // Getters and Setters

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

    public Boolean getEligible() {
        return eligible;
    }

    public void setEligible(Boolean eligible) {
        this.eligible = eligible;
    }
}