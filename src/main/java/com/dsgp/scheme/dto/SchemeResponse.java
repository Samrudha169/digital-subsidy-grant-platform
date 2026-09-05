package com.dsgp.scheme.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Response DTO returned by all Scheme API endpoints.
 *
 * <p>Mirrors the JSON structure documented in {@code api-design.md §3}.
 *
 * <p>Example:
 * <pre>{@code
 * {
 *   "id":               1,
 *   "schemeName":       "PM-KISAN Samman Nidhi",
 *   "description":      "Income support to farmer families",
 *   "minAge":           18,
 *   "maxAge":           60,
 *   "maxAnnualIncome":  150000.00,
 *   "maxLandHolding":   2.0,
 *   "requiredCategory": "SC/ST",
 *   "grantAmount":      6000.00,
 *   "active":           true
 * }
 * }</pre>
 */
@Data
@Builder
public class SchemeResponse {

    private Long    id;
    private String  schemeName;
    private String  description;
    private Integer minAge;
    private Integer maxAge;

    /** Maximum annual income threshold in INR. Null means no restriction. */
    private BigDecimal maxAnnualIncome;

    /** Maximum land holding threshold in acres. Null means no restriction. */
    private BigDecimal maxLandHolding;

    /** Required social category. Null means any category is accepted. */
    private String requiredCategory;

    /** Grant amount in INR disbursed under this scheme. */
    private BigDecimal grantAmount;

    private Boolean active;
}
