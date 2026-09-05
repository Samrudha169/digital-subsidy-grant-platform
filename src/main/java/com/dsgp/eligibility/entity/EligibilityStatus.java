package com.dsgp.eligibility.entity;

/**
 * Outcome of an eligibility evaluation for a beneficiary against a scheme.
 *
 * <ul>
 *   <li>{@link #ELIGIBLE}   — Total score ≥ 60; application proceeds to verification.</li>
 *   <li>{@link #INELIGIBLE} — Total score < 60; application rejected at scoring stage.</li>
 * </ul>
 */
public enum EligibilityStatus {
    ELIGIBLE,
    INELIGIBLE
}
