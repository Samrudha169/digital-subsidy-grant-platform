package com.dsgp.application.entity;

import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.Scheme;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity for the {@code scheme_applications} table.
 *
 * <p>This is the canonical, single SchemeApplication entity for the entire
 * platform. It is the result of merging:
 * <ul>
 *   <li>The original Milestone 2 entity (full JPA relationships, Lombok,
 *       eligibility-gated workflow, applicationStatus, applicationDate)</li>
 *   <li>Samrudha's application package move (entity lives under
 *       {@code com.dsgp.application.entity})</li>
 * </ul>
 *
 * <p><strong>Samrudha's verificationLevel / verifiedBy / verificationRemarks /
 * verifiedAt fields are NOT retained.</strong> These are redundant:
 * the full, immutable verification audit trail is stored in
 * {@code verification_records} via {@link com.dsgp.verification.entity.VerificationRecord}.
 * The authoritative workflow state is {@link #applicationStatus}.
 *
 * <h3>Workflow statuses (stored in applicationStatus)</h3>
 * <pre>
 * PENDING → UNDER_REVIEW → FIELD_APPROVED → APPROVED
 *                       ↘ ESCALATED → DISTRICT_APPROVED → APPROVED
 *                       ↘ REJECTED (at any stage)
 * </pre>
 */
@Entity
@Table(name = "scheme_applications")
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
     * Loaded lazily; never nullable.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiary_id", nullable = false)
    private Beneficiary beneficiary;

    /**
     * The government scheme this application is for.
     * Loaded lazily; never nullable.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheme_id", nullable = false)
    private Scheme scheme;

    /**
     * Current workflow status. One of:
     * PENDING, UNDER_REVIEW, FIELD_APPROVED, ESCALATED,
     * DISTRICT_APPROVED, APPROVED, REJECTED.
     *
     * <p>Default is {@code PENDING} (set by {@link #onCreate()} and Lombok
     * {@code @Builder.Default}).
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