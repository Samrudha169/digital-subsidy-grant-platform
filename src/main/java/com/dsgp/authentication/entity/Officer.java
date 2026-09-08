package com.dsgp.authentication.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity representing a government officer account.
 *
 * <p>Officers are distinct from beneficiaries. They log in through
 * {@code POST /api/v1/auth/officer-login} using their username and password.
 * Their role determines which verification actions they may take.
 *
 * <p>Mapped to the {@code officers} table.
 */
@Entity
@Table(name = "officers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Officer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Login username — unique across all officers. */
    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    /** BCrypt-hashed password — never returned in responses. */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /** Display name shown in dashboards and audit records. */
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    /** Officer's work email (for notifications — future milestone). */
    @Column(name = "email", length = 150)
    private String email;

    /** The role that determines this officer's permissions. */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private OfficerRole role;

    /**
     * District this officer is responsible for.
     * Used for filtering applications in Field/District officer queues.
     * Nullable for Finance Approvers (finance is not district-specific).
     */
    @Column(name = "district", length = 100)
    private String district;

    /** {@code false} if the officer account has been deactivated. */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
