package com.dsgp.application.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO returned after a scheme application is submitted or retrieved.
 *
 * Includes application, beneficiary, scheme, eligibility,
 * sanction and disbursement information.
 */
@Data
@Builder
public class ApplicationResponse {

    /** Primary key of the created scheme_applications row. */
    private Long applicationId;

    /** Internal ID of the beneficiary. */
    private Integer beneficiaryId;

    /** Full name of the beneficiary. */
    private String beneficiaryName;

    /** Internal ID of the scheme. */
    private Long schemeId;

    /** Human-readable scheme name. */
    private String schemeName;

    /** Current workflow status. */
    private String applicationStatus;

    /** The eligibility score that qualified this application. */
    private int eligibilityScore;

    /** Timestamp when the application was persisted. */
    private LocalDateTime applicationDate;

    /** Amount sanctioned by the Finance Officer. */
    private BigDecimal sanctionedAmount;

    /**
     * Current disbursement stage for this application.
     *
     * Example:
     * 1 = Stage 1
     * 2 = Stage 2
     * 3 = Stage 3
     *
     * Null means that no disbursement stage has been created yet.
     */
    private Integer currentDisbursementStage;
}