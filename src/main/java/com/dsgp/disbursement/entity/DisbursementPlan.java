package com.dsgp.disbursement.entity;

import com.dsgp.application.entity.SchemeApplication;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "disbursement_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisbursementPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    @JsonIgnore
    private SchemeApplication application;

    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "released_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal releasedAmount = BigDecimal.ZERO;

    @Column(name = "remaining_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal remainingAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "disbursement_type", nullable = false, length = 20)
    private DisbursementType disbursementType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DisbursementStatus status = DisbursementStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if (releasedAmount == null) {
            releasedAmount = BigDecimal.ZERO;
        }

        if (remainingAmount == null && totalAmount != null) {
            remainingAmount = totalAmount;
        }
    }
}