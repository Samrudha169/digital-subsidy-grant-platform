package com.dsgp.eligibility.dto;

import com.dsgp.eligibility.entity.EligibilityStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Full eligibility evaluation response returned to the caller.
 *
 * <p>Matches the JSON structure defined in {@code eligibility-scoring.md §5}.
 *
 * <p>Example:
 * <pre>{@code
 * {
 *   "resultId": 12,
 *   "beneficiaryId": 101,
 *   "schemeId": 5,
 *   "schemeName": "PM-KISAN Samman Nidhi",
 *   "totalScore": 80,
 *   "eligibilityStatus": "ELIGIBLE",
 *   "eligible": true,
 *   "criteria": {
 *     "ageCheck":      { "points": 20, "passed": true,  "detail": "Age 42 within range [18, 60]" },
 *     "incomeCheck":   { "points": 30, "passed": true,  "detail": "Income ₹85,000 ≤ threshold ₹1,50,000" },
 *     "landCheck":     { "points": 20, "passed": true,  "detail": "Land 1.5 ac ≤ threshold 2.0 ac" },
 *     "categoryCheck": { "points": 0,  "passed": false, "detail": "Category GENERAL; required SC/ST" },
 *     "identityCheck": { "points": 10, "passed": true,  "detail": "Identity verified" }
 *   },
 *   "evaluatedAt": "2025-04-15T10:32:00"
 * }
 * }</pre>
 */
@Data
@Builder
public class EligibilityResultResponse {

    /** Primary key of the persisted {@code EligibilityResult} record. */
    private Long resultId;

    private Integer beneficiaryId;
    private Long schemeId;
    private String schemeName;

    /** Weighted total score out of 100. */
    private int totalScore;

    /** ELIGIBLE (≥ 60) or INELIGIBLE (< 60). */
    private EligibilityStatus eligibilityStatus;

    /** Convenience boolean — {@code true} iff {@code eligibilityStatus == ELIGIBLE}. */
    private boolean eligible;

    /**
     * Per-criterion breakdown.
     * Keys: {@code ageCheck}, {@code incomeCheck}, {@code landCheck},
     *       {@code categoryCheck}, {@code identityCheck}.
     */
    private Map<String, CriterionResult> criteria;

    /** Timestamp of this evaluation. */
    private LocalDateTime evaluatedAt;
}
