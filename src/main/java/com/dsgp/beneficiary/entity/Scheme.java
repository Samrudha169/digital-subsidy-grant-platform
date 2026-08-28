package com.dsgp.beneficiary.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "schemes")
public class Scheme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scheme_name", nullable = false)
    private String schemeName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Column(name = "max_annual_income")
    private BigDecimal maxAnnualIncome;

    @Column(name = "max_land_holding")
    private BigDecimal maxLandHolding;

    @Column(name = "required_category")
    private String requiredCategory;

    @Column(name = "grant_amount")
    private BigDecimal grantAmount;

    @Column(nullable = false)
    private Boolean active = true;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSchemeName() {
        return schemeName;
    }

    public void setSchemeName(String schemeName) {
        this.schemeName = schemeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getMinAge() {
        return minAge;
    }

    public void setMinAge(Integer minAge) {
        this.minAge = minAge;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Integer maxAge) {
        this.maxAge = maxAge;
    }

    public BigDecimal getMaxAnnualIncome() {
        return maxAnnualIncome;
    }

    public void setMaxAnnualIncome(BigDecimal maxAnnualIncome) {
        this.maxAnnualIncome = maxAnnualIncome;
    }

    public BigDecimal getMaxLandHolding() {
        return maxLandHolding;
    }

    public void setMaxLandHolding(BigDecimal maxLandHolding) {
        this.maxLandHolding = maxLandHolding;
    }

    public String getRequiredCategory() {
        return requiredCategory;
    }

    public void setRequiredCategory(String requiredCategory) {
        this.requiredCategory = requiredCategory;
    }

    public BigDecimal getGrantAmount() {
        return grantAmount;
    }

    public void setGrantAmount(BigDecimal grantAmount) {
        this.grantAmount = grantAmount;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}