package com.dsgp.eligibility.rules;

import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.eligibility.dto.CriterionResult;

/**
 * Abstract base class providing genuinely common helper behaviour that is
 * shared across all scheme-specific {@link EligibilityRule} implementations.
 *
 * <p>Only logic that is <em>identical</em> across every scheme lives here.
 * Scheme-specific scoring formulas must remain in their concrete subclasses.
 */
public abstract class AbstractEligibilityRule implements EligibilityRule {

    // ========================================================================
    // COMMON CRITERION BUILDERS
    // ========================================================================

    /**
     * Creates a {@link CriterionResult} representing a passed criterion with
     * the given points and detail message.
     */
    protected CriterionResult passed(int points, String detail) {
        return CriterionResult.builder()
                .points(points)
                .passed(true)
                .detail(detail)
                .build();
    }

    /**
     * Creates a {@link CriterionResult} representing a failed criterion.
     * Points are always zero for a failed criterion.
     */
    protected CriterionResult failed(String detail) {
        return CriterionResult.builder()
                .points(0)
                .passed(false)
                .detail(detail)
                .build();
    }

    // ========================================================================
    // COMMON STRING NORMALISATION
    // ========================================================================

    /**
     * Trims, lower-cases, and collapses internal whitespace in the given
     * value.  Returns an empty string when {@code value} is {@code null}.
     */
    protected String normalise(String value) {
        if (value == null) {
            return "";
        }
        return value
                .trim()
                .toLowerCase()
                .replaceAll("\\s+", " ");
    }

    // ========================================================================
    // COMMON CATEGORY EVALUATION
    // ========================================================================

    /**
     * Evaluates the beneficiary's category against the scheme's required
     * category.  The {@code maxPoints} parameter allows each scheme to award
     * a different number of points for this criterion while keeping the
     * evaluation logic itself identical.
     */
    protected CriterionResult evaluateCategory(
            Beneficiary b,
            Scheme s,
            int maxPoints) {

        String requiredCategory =
                normalise(s.getRequiredCategory());

        if (requiredCategory.isEmpty()
                || requiredCategory.equals("all")) {

            return passed(
                    maxPoints,
                    "No category restriction → "
                            + maxPoints + "/" + maxPoints + " points"
            );
        }

        if (b.getCategory() == null) {
            return failed(
                    "Beneficiary category not recorded; required: "
                            + s.getRequiredCategory()
            );
        }

        boolean matches =
                b.getCategory()
                        .name()
                        .equalsIgnoreCase(
                                s.getRequiredCategory().trim()
                        );

        if (matches) {
            return passed(
                    maxPoints,
                    "Category "
                            + b.getCategory().name()
                            + " matches required "
                            + s.getRequiredCategory()
            );
        }

        return failed(
                "Category "
                        + b.getCategory().name()
                        + "; required "
                        + s.getRequiredCategory()
        );
    }

    // ========================================================================
    // COMMON IDENTITY EVALUATION
    // ========================================================================

    /**
     * Evaluates whether the beneficiary's identity has been verified by a
     * Field Officer.  The {@code maxPoints} parameter allows each scheme to
     * award a different number of points while keeping the evaluation logic
     * identical.
     */
    protected CriterionResult evaluateIdentity(
            Beneficiary b,
            int maxPoints) {

        if (b.isIdentityVerified()) {

            return passed(
                    maxPoints,
                    "Identity verified → "
                            + maxPoints
                            + "/"
                            + maxPoints
                            + " points"
            );
        }

        return failed(
                "Identity not yet verified by a Field Officer"
        );
    }
}
