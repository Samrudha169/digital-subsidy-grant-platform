package com.dsgp.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for submitting a new scheme application.
 *
 * <p>The calling client must have already run
 * {@code POST /api/v1/eligibility/check} for this
 * (beneficiaryId, schemeId) pair before submitting.
 * The service layer enforces this by requiring an ELIGIBLE
 * result to exist in {@code eligibility_results}.
 */
@Data
public class ApplicationRequest {

    @NotNull(message = "beneficiaryId is required")
    private Integer beneficiaryId;

    @NotNull(message = "schemeId is required")
    private Long schemeId;
}
