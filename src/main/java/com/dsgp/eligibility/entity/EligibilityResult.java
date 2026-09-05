package com.dsgp.eligibility.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity that records the outcome of one eligibility evaluation.
 *
 * <p>One record is created per (beneficiary, scheme) evaluation. If the same
 * pair is re-evaluated, the previous record is replaced by the new one
 * (handled by {@link com.dsgp.eligibility.service.EligibilityScoringEngine}).
 *
 * <p>Mapped to the {@code eligibility_results} table (added to
 * {@code 01-schema.sql} as part of Milestone 2).
 *
 * <p>The {@code criteriaJson} column stores the per-criterion breakdown
 * serialised as JSON for auditability and dashboard display, matching the
 * {@code EligibilityResultResponse.criteria} structure.
 */
@Entity
@Table(
    name = "eligibility_results",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_eligibility_beneficiary_scheme",
        columnNames = {"beneficiary_id", "scheme_id"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilityResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK to the beneficiary that was evaluated. */
    @Column(name = "beneficiary_id", nullable = false)
    private Integer beneficiaryId;

    /** FK to the scheme against which the beneficiary was evaluated. */
    @Column(name = "scheme_id", nullable = false)
    private Long schemeId;

    /** Human-readable scheme name — denormalised for fast display. */
    @Column(name = "scheme_name", nullable = false, length = 150)
    private String schemeName;

    /** Total weighted score out of 100. */
    @Column(name = "total_score", nullable = false)
    private int totalScore;

    /**
     * Whether the beneficiary is eligible for the scheme.
     * {@code true} iff {@code totalScore >= 60}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "eligibility_status", nullable = false, length = 15)
    private EligibilityStatus eligibilityStatus;

    /**
     * JSON representation of the per-criterion breakdown.
     * Stored as TEXT for full auditability; deserialised by the service
     * when constructing the {@code EligibilityResultResponse}.
     */
    @Column(name = "criteria_json", columnDefinition = "TEXT")
    private String criteriaJson;

    /** Timestamp when this evaluation was performed. */
    @Column(name = "evaluated_at", nullable = false)
    @Builder.Default
    private LocalDateTime evaluatedAt = LocalDateTime.now();
}
