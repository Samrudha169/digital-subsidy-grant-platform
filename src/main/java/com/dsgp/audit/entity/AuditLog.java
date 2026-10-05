package com.dsgp.audit.entity;

import com.dsgp.authentication.entity.Officer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Officer who performed the action.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "officer_id", nullable = false)
    private Officer officer;

    /**
     * Role of the officer at the time of the action.
     */
    @Column(name = "role", nullable = false, length = 50)
    private String role;

    /**
     * Action performed by the officer.
     */
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    /**
     * Type of entity affected by the action.
     * Example: APPLICATION, DISBURSEMENT_STAGE
     */
    @Column(name = "entity_type", length = 100)
    private String entityType;

    /**
     * ID of the affected entity.
     */
    @Column(name = "entity_id")
    private Long entityId;

    /**
     * Additional information about the action.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Date and time when the action occurred.
     */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}