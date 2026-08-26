package com.dsgp.beneficiary.entity;

/**
 * Lifecycle status of a beneficiary registration.
 *
 * <ul>
 *   <li>{@code PENDING}   — Registered but not yet verified/activated</li>
 *   <li>{@code ACTIVE}    — Verified and eligible to receive subsidies</li>
 *   <li>{@code SUSPENDED} — Temporarily or permanently deactivated</li>
 * </ul>
 */
public enum RegistrationStatus {
    PENDING,
    ACTIVE,
    SUSPENDED
}
