package com.dsgp.verification.entity;

/**
 * The stage in the multi-level verification chain at which a
 * {@link VerificationRecord} was recorded.
 *
 * <ul>
 *   <li>{@link #FIELD}    — Ground-level verification by a Field Officer.</li>
 *   <li>{@link #DISTRICT} — Second-level review by a District Officer
 *       (only reached after Field Officer escalation).</li>
 *   <li>{@link #FINANCE}  — Final financial authorisation by a Finance Approver.</li>
 * </ul>
 */
public enum VerificationStage {

    /** Stage 1 — Field Officer verification. */
    FIELD,

    /** Stage 2 — District Officer review (after escalation from FIELD). */
    DISTRICT,

    /** Stage 3 — Finance Approver authorisation. */
    FINANCE
}
