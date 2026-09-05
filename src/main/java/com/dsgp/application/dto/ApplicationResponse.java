package com.dsgp.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO returned after a scheme application is submitted.
 *
 * <p>Includes the application ID, status, eligibility score that
 * unlocked the submission, and the scheme / beneficiary details
 * required by the TrackApplication frontend view.
 */
@Data
@Builder
public class ApplicationResponse {

    /** Primary key of the created {@code scheme_applications} row. */
    private Long applicationId;

    /** Internal ID of the beneficiary. */
    private Integer beneficiaryId;

    /** Full name of the beneficiary (for display). */
    private String beneficiaryName;

    /** Internal ID of the scheme. */
    private Long schemeId;

    /** Human-readable scheme name (for display). */
    private String schemeName;

    /**
     * Current workflow status.
     * Always {@code "PENDING"} immediately after submission.
     */
    private String applicationStatus;

    /** The eligibility score that qualified this application (0-100). */
    private int eligibilityScore;

    /** Timestamp when the application was persisted. */
    private LocalDateTime applicationDate;
}
