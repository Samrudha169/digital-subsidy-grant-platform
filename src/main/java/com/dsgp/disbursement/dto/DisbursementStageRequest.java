package com.dsgp.disbursement.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class DisbursementStageRequest {

    private Integer stageNumber;

    private BigDecimal amount;

    private String milestone;

    private LocalDate dueDate;
}