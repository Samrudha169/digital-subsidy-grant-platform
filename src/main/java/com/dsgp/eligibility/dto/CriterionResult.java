package com.dsgp.eligibility.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Result for a single eligibility criterion.
 *
 * <p>Included in {@link EligibilityResultResponse#getCriteria()} to give a
 * per-criterion breakdown of the score for transparency and auditability.
 *
 * <p>Example:
 * <pre>{@code
 * {
 *   "points": 20,
 *   "passed": true,
 *   "detail": "Age 42 within range [18, 60]"
 * }
 * }</pre>
 */
@Data
@Builder
public class CriterionResult {

    /** Points awarded for this criterion (0 if failed, max points if passed). */
    private int points;

    /** Whether this criterion was satisfied. */
    private boolean passed;

    /** Human-readable explanation of the outcome. */
    private String detail;
}
