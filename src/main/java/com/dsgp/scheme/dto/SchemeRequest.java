package com.dsgp.scheme.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request DTO for creating or updating a government scheme.
 *
 * <p>All eligibility thresholds are optional; a {@code null} value
 * means "no restriction" — the engine will auto-award full points
 * for that criterion (per {@code eligibility-scoring.md §3.1}).
 */
@Data
public class SchemeRequest {

    @NotBlank(message = "Scheme name is required")
    @Size(max = 150, message = "Scheme name must not exceed 150 characters")
    private String schemeName;

    private String description;

    /** Minimum age (inclusive). Null = no restriction. */
    @Min(value = 0, message = "Minimum age must be >= 0")
    private Integer minAge;

    /** Maximum age (inclusive). Null = no restriction. */
    @Min(value = 0, message = "Maximum age must be >= 0")
    private Integer maxAge;

    /** Maximum annual income in INR. Null = no restriction. */
    @DecimalMin(value = "0.0", inclusive = false, message = "Max annual income must be positive")
    private BigDecimal maxAnnualIncome;

    /** Maximum land holding in acres. Null = no restriction. */
    @DecimalMin(value = "0.0", inclusive = false, message = "Max land holding must be positive")
    private BigDecimal maxLandHolding;

    /** Required social category (e.g. SC, ST, OBC). Null = no restriction. */
    @Size(max = 20, message = "Required category must not exceed 20 characters")
    private String requiredCategory;

    /** Grant amount in INR provided under this scheme. Null = not applicable. */
    @DecimalMin(value = "0.0", inclusive = false, message = "Grant amount must be positive")
    private BigDecimal grantAmount;

    /** Whether this scheme is currently accepting applications. Defaults to true. */
    private Boolean active = true;
}
