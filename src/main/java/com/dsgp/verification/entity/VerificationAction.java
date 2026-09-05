package com.dsgp.verification.entity;

/**
 * The action taken by an officer during a verification stage.
 *
 * <ul>
 *   <li>{@link #APPROVE}   — The officer approves the application at their stage.</li>
 *   <li>{@link #REJECT}    — The officer rejects the application (terminal).</li>
 *   <li>{@link #ESCALATE}  — The Field Officer escalates to the District Officer.</li>
 * </ul>
 *
 * <p>Only a {@link VerificationStage#FIELD} officer may {@link #ESCALATE}.
 * District and Finance officers may only {@link #APPROVE} or {@link #REJECT}.
 */
public enum VerificationAction {

    /** Approve the application at the current stage, advancing it to the next. */
    APPROVE,

    /** Reject the application permanently (unless reopened by an Administrator). */
    REJECT,

    /**
     * Escalate to the District Officer (FIELD stage only).
     * Moves the application status to {@code ESCALATED}.
     */
    ESCALATE
}
