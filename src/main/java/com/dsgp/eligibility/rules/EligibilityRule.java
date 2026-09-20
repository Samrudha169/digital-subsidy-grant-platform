package com.dsgp.eligibility.rules;

import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.eligibility.dto.CriterionResult;

import java.util.Map;

/**
 * Contract for a scheme-specific eligibility evaluation strategy.
 *
 * <p>Each implementation is responsible for evaluating a {@link Beneficiary}
 * against a particular {@link Scheme} and returning a per-criterion breakdown
 * together with a determination of whether the scheme's mandatory conditions
 * were satisfied.
 *
 * <p>Rule implementations must not contain any orchestration logic (loading
 * entities, persisting results, etc.). That responsibility belongs exclusively
 * to {@link com.dsgp.eligibility.service.EligibilityScoringEngine}.
 */
public interface EligibilityRule {

    /**
     * Evaluates the beneficiary against the scheme and returns a map of
     * criterion key → {@link CriterionResult}.
     *
     * <p>The returned map must preserve insertion order so that the JSON
     * serialisation presented to callers is stable and predictable.
     *
     * @param beneficiary the beneficiary being evaluated
     * @param scheme      the scheme against which the evaluation is performed
     * @return an ordered map of criterion results
     */
    Map<String, CriterionResult> evaluate(Beneficiary beneficiary, Scheme scheme);

    /**
     * Returns {@code true} if and only if every mandatory criterion for this
     * scheme was satisfied in the supplied criteria map.
     *
     * <p>This is evaluated separately from the total score so that a
     * beneficiary who meets the point threshold but fails a hard gate is
     * still correctly marked as ineligible.
     *
     * @param criteria the criteria map previously returned by {@link #evaluate}
     * @return {@code true} when all mandatory conditions passed
     */
    boolean mandatoryConditionsPassed(Map<String, CriterionResult> criteria);
}
