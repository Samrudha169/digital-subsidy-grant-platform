package com.dsgp.verification.entity;

import com.dsgp.application.entity.SchemeApplication;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing one recorded action taken by a verification officer
 * on a {@link SchemeApplication}.
 *
 * <p>One record is persisted for each action (approve / reject / escalate) at
 * every verification stage. This provides a complete, immutable audit trail of
 * the full verification chain for any application.
 *
 * <p>Mapped to the {@code verification_records} table (added to
 * {@code 01-schema.sql} as part of Milestone 2).
 *
 * <h3>Stage — Action — Application Status transitions</h3>
 * <pre>
 * FIELD  / APPROVE   → applicationStatus = FIELD_APPROVED
 * FIELD  / REJECT    → applicationStatus = REJECTED
 * FIELD  / ESCALATE  → applicationStatus = ESCALATED
 * DISTRICT / APPROVE → applicationStatus = DISTRICT_APPROVED
 * DISTRICT / REJECT  → applicationStatus = REJECTED
 * FINANCE / APPROVE  → applicationStatus = APPROVED
 * FINANCE / REJECT   → applicationStatus = REJECTED
 * </pre>
 *
 * <p><strong>Phase:</strong> Milestone 2 implementation.
 */
@Entity
@Table(name = "verification_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The application this record belongs to.
     * Cascade delete is NOT applied — records are retained for audit history.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "scheme_application_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_verification_application")
    )
    private SchemeApplication schemeApplication;

    /**
     * The verification stage at which this action was taken.
     * Stored as a string column for readability in the database.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 10)
    private VerificationStage stage;

    /**
     * The action taken by the officer at this stage.
     * One of APPROVE, REJECT, ESCALATE (ESCALATE is FIELD stage only).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_taken", nullable = false, length = 10)
    private VerificationAction actionTaken;

    /**
     * Username or identifier of the officer who performed this action.
     * Full RBAC and JWT officer identity are implemented in Milestone 5.
     * For Milestone 2, this is a free-text field supplied in the request.
     */
    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    /** Timestamp when this action was recorded. Set once on insert. */
    @Column(name = "performed_at", nullable = false, updatable = false)
    private LocalDateTime performedAt;

    /**
     * Free-text reason or notes from the officer.
     * Mandatory for REJECT actions; optional for APPROVE and ESCALATE.
     */
    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @PrePersist
    protected void onCreate() {
        performedAt = LocalDateTime.now();
    }
}
