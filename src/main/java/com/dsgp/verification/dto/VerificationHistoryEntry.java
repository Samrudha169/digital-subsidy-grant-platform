package com.dsgp.verification.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Represents a single recorded action in the verification history.
 *
 * <p>Included as an element of {@link VerificationStatusResponse#getHistory()}.
 */
@Data
@Builder
public class VerificationHistoryEntry {

    /** The verification stage: FIELD, DISTRICT, or FINANCE. */
    private String stage;

    /** The action taken: APPROVE, REJECT, or ESCALATE. */
    private String action;

    /** Username or identifier of the officer who performed this action. */
    private String performedBy;

    /** Timestamp when the action was recorded. */
    private LocalDateTime performedAt;

    /** Free-text notes provided by the officer. */
    private String remarks;
}
