package com.dsgp.beneficiary.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing a beneficiary's application for a specific scheme.
 *
 * <p>Maps to the {@code scheme_applications} table.  A beneficiary may apply
 * for multiple schemes, but only once per scheme (enforced by the
 * {@code uk_beneficiary_scheme} unique constraint).
 *
 * <p>The {@code applicationStatus} field tracks the progress of the
 * application through the verification and approval workflow.  Status
 * values align with the verification workflow design:
 * <ul>
 *   <li>{@code PENDING}     — Submitted, awaiting Field Officer review</li>
 *   <li>{@code UNDER_REVIEW} — Assigned to a Field Officer</li>
 *   <li>{@code APPROVED}    — Approved by Finance Approver; eligible for disbursement</li>
 *   <li>{@code REJECTED}    — Rejected at any verification stage</li>
 *   <li>{@code ESCALATED}   — Escalated from Field → District Officer</li>
 * </ul>
 *
 * <p><strong>Phase:</strong> Milestone 1 entity definition.
 * Full application-submission API and verification routing are implemented
 * in subsequent milestones.
 */
@Entity
@Table(
    name = "scheme_applications",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_beneficiary_scheme",
        columnNames = {"beneficiary_id", "scheme_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The beneficiary who submitted this application.
     * Cascade delete is NOT applied here — scheme applications are deleted
     * explicitly to preserve audit history.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "beneficiary_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_application_beneficiary")
    )
    private Beneficiary beneficiary;

    /**
     * The scheme being applied for.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "scheme_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_application_scheme")
    )
    private Scheme scheme;

    /**
     * Current status of the application in the verification workflow.
     * Defaults to {@code PENDING} on submission.
     */
    @Column(name = "application_status", nullable = false, length = 20)
    @Builder.Default
    private String applicationStatus = "PENDING";

    /** Timestamp when the application was submitted. Set once on insert. */
    @Column(name = "application_date", nullable = false, updatable = false)
    private LocalDateTime applicationDate;

    @PrePersist
    protected void onCreate() {
        applicationDate = LocalDateTime.now();
    }
}
