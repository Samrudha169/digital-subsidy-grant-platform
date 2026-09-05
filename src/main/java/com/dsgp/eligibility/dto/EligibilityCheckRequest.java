package com.dsgp.eligibility.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for triggering an eligibility check.
 *
 * <p>The engine looks up the beneficiary and scheme by their IDs and evaluates
 * the beneficiary's registered profile against the scheme's eligibility rules.
 */
@Data
public class EligibilityCheckRequest {

    @NotNull(message = "Beneficiary ID is required")
    private Integer beneficiaryId;

    @NotNull(message = "Scheme ID is required")
    private Long schemeId;
}
