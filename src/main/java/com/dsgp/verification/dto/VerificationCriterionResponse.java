package com.dsgp.verification.dto;

import com.dsgp.verification.entity.VerificationCriterionStatus;
import com.dsgp.verification.entity.VerificationStage;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationCriterionResponse {

    private Long id;
    private VerificationStage stage;
    private String criterionCode;
    private String criterionName;
    private VerificationCriterionStatus status;
    private String verifiedBy;
    private String remarks;
    private LocalDateTime verifiedAt;
}