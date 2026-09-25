package com.dsgp.disbursement.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Immutable snapshot of disbursement analytics metrics.
 * Built and returned by {@link com.dsgp.disbursement.service.DisbursementAnalyticsService}.
 */
@Getter
@Builder
public class DisbursementAnalyticsResponse {

    /** Sum of totalAmount across all disbursement plans. */
    private final BigDecimal totalSanctioned;

    /** Sum of all disbursement stage amounts (i.e. what has been broken into stages). */
    private final BigDecimal totalPlanned;

    /** Sum of releasedAmount across all disbursement plans. */
    private final BigDecimal totalReleased;

    /** Sum of remainingAmount across all disbursement plans. */
    private final BigDecimal totalRemaining;

    /**
     * Released amount grouped by scheme name.
     * Key   → scheme name (e.g. "PM-KISAN")
     * Value → total released for that scheme
     */
    private final Map<String, BigDecimal> releasedByScheme;

    /**
     * Released amount grouped by beneficiary state.
     * Key   → state name (e.g. "Maharashtra")
     * Value → total released for beneficiaries in that state
     */
    private final Map<String, BigDecimal> releasedByState;
}
