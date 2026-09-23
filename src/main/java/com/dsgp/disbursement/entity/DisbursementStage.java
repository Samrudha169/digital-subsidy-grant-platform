package com.dsgp.disbursement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "disbursement_stages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisbursementStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "disbursement_plan_id", nullable = false)
    @JsonIgnore
    private DisbursementPlan disbursementPlan;

    @Column(name = "stage_number", nullable = false)
    private Integer stageNumber;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "milestone", nullable = false, length = 500)
    private String milestone;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DisbursementStageStatus status =
            DisbursementStageStatus.PENDING;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;
}