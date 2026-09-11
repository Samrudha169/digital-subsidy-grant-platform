package com.dsgp.verification.dto;

import com.dsgp.verification.entity.VerificationCriterionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationCriterionUpdateRequest {

    @NotBlank
    private String performedBy;

    @NotNull
    private VerificationCriterionStatus status;

    private String remarks;
}