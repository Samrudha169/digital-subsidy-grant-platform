package com.dsgp.verification.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response returned by any verification query or action endpoint.
 *
 * <p>Contains the full current state of the application plus the complete
 * chronological audit trail of all verification actions taken so far.
 *
 * <p>Matches the API contract specified in {@code api-design.md §5}.
 *
 * <p>Example:
 * <pre>{@code
 * {
 *   "applicationId":     101,
 *   "beneficiaryId":     42,
 *   "beneficiaryName":   "Ravi Kumar",
 *   "schemeId":          5,
 *   "schemeName":        "PM-KISAN Samman Nidhi",
 *   "applicationStatus": "FIELD_APPROVED",
 *   "applicationDate":   "2025-04-10T09:15:00",
 *   "history": [
 *     {
 *       "stage":       "FIELD",
 *       "action":      "APPROVE",
 *       "performedBy": "officer_rajan",
 *       "performedAt": "2025-04-14T14:20:00",
 *       "remarks":     "All documents verified."
 *     }
 *   ]
 * }
 * }</pre>
 */
@Data
@Builder
public class VerificationStatusResponse {

    /** Primary key of the {@code scheme_applications} row. */
    private Long applicationId;

    /** Internal ID of the beneficiary. */
    private Integer beneficiaryId;

    /** Full name of the beneficiary. */
    private String beneficiaryName;

    /** Internal ID of the scheme. */
    private Long schemeId;

    /** Human-readable scheme name. */
    private String schemeName;

    /**
     * Current application status in the workflow.
     * One of: PENDING, UNDER_REVIEW, FIELD_APPROVED, ESCALATED,
     *         DISTRICT_APPROVED, APPROVED, REJECTED.
     */
    private String applicationStatus;

    /** Timestamp when the application was originally submitted. */
    private LocalDateTime applicationDate;

    /**
     * Chronological list of all verification actions taken so far.
     * Empty list if no action has been taken yet.
     */
    private List<VerificationHistoryEntry> history;
}
