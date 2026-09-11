package com.dsgp.verification.entity;

import com.dsgp.application.entity.SchemeApplication;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_criteria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheme_application_id", nullable = false)
    private SchemeApplication schemeApplication;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationStage stage;

    @Column(name = "criterion_code", nullable = false, length = 100)
    private String criterionCode;

    @Column(name = "criterion_name", nullable = false, length = 255)
    private String criterionName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VerificationCriterionStatus status =
            VerificationCriterionStatus.PENDING;

    @Column(name = "verified_by", length = 100)
    private String verifiedBy;

    @Column(length = 500)
    private String remarks;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = VerificationCriterionStatus.PENDING;
        }
    }
}