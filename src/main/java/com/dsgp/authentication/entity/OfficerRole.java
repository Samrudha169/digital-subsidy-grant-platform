package com.dsgp.authentication.entity;

/**
 * Roles available to government officers on the DSGP platform.
 *
 * <ul>
 *   <li>{@link #FIELD_OFFICER}    — Ground-level verification and document checking.</li>
 *   <li>{@link #DISTRICT_OFFICER} — District-level review of escalated applications.</li>
 *   <li>{@link #FINANCE_APPROVER} — Final financial approval before disbursement.</li>
 * </ul>
 */
public enum OfficerRole {
    FIELD_OFFICER,
    DISTRICT_OFFICER,
    FINANCE_APPROVER
}
