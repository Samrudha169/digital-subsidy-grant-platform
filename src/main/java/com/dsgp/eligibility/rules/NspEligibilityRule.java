package com.dsgp.eligibility.rules;

import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.eligibility.dto.CriterionResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * NSP (National Scholarship Portal) eligibility rule.
 *
 * <p>Evaluates five criteria for the NSP scheme (no land criterion):
 * <ol>
 *   <li>Age (15–30, tiered scoring)</li>
 *   <li>Annual income (≤ ₹2,50,000, tiered scoring)</li>
 *   <li>Occupation (must be Student / Studying / Student/Learner)</li>
 *   <li>Category (scheme-defined, 10 points)</li>
 *   <li>Identity verification (10 points)</li>
 * </ol>
 *
 * <p>Mandatory conditions: age, income, and occupation must all pass.
 */
@Component
public class NspEligibilityRule extends AbstractEligibilityRule {

    @Override
    public Map<String, CriterionResult> evaluate(
            Beneficiary beneficiary,
            Scheme scheme) {

        CriterionResult age =
                evaluateNspAge(beneficiary);

        CriterionResult income =
                evaluateNspIncome(beneficiary);

        CriterionResult occupation =
                evaluateNspOccupation(beneficiary);

        CriterionResult category =
                evaluateCategory(beneficiary, scheme, 10);

        CriterionResult identity =
                evaluateIdentity(beneficiary, 10);

        Map<String, CriterionResult> criteria =
                new LinkedHashMap<>();

        criteria.put("ageCheck", age);
        criteria.put("incomeCheck", income);
        criteria.put("occupationCheck", occupation);
        criteria.put("categoryCheck", category);
        criteria.put("identityCheck", identity);

        return criteria;
    }

    @Override
    public boolean mandatoryConditionsPassed(
            Map<String, CriterionResult> criteria) {

        return criteria.get("ageCheck").isPassed()
                && criteria.get("incomeCheck").isPassed()
                && criteria.get("occupationCheck").isPassed();
    }

    // ========================================================================
    // NSP AGE
    // ========================================================================

    private CriterionResult evaluateNspAge(
            Beneficiary b) {

        Integer age = b.getAge();

        if (age == null) {
            return failed("Beneficiary age not recorded");
        }

        if (age < 15 || age > 30) {
            return failed(
                    "Age " + age
                            + " is outside NSP required range 15-30"
            );
        }

        if (age <= 18) {
            return passed(
                    20,
                    "Age " + age + " → 20/20 points"
            );
        }

        if (age <= 22) {
            return passed(
                    17,
                    "Age " + age + " → 17/20 points"
            );
        }

        if (age <= 26) {
            return passed(
                    14,
                    "Age " + age + " → 14/20 points"
            );
        }

        return passed(
                10,
                "Age " + age + " → 10/20 points"
        );
    }

    // ========================================================================
    // NSP INCOME
    // ========================================================================

    private CriterionResult evaluateNspIncome(
            Beneficiary b) {

        BigDecimal income =
                b.getAnnualIncome();

        if (income == null) {
            return failed(
                    "Beneficiary annual income not recorded"
            );
        }

        if (income.compareTo(
                new BigDecimal("250000")) > 0) {

            return failed(
                    "Income ₹" + income
                            + " exceeds NSP maximum ₹250000"
            );
        }

        if (income.compareTo(
                new BigDecimal("100000")) <= 0) {

            return passed(
                    30,
                    "Income ₹" + income
                            + " → 30/30 points"
            );
        }

        if (income.compareTo(
                new BigDecimal("150000")) <= 0) {

            return passed(
                    25,
                    "Income ₹" + income
                            + " → 25/30 points"
            );
        }

        if (income.compareTo(
                new BigDecimal("200000")) <= 0) {

            return passed(
                    20,
                    "Income ₹" + income
                            + " → 20/30 points"
            );
        }

        return passed(
                10,
                "Income ₹" + income
                        + " → 10/30 points"
        );
    }

    // ========================================================================
    // NSP OCCUPATION
    // ========================================================================

    private CriterionResult evaluateNspOccupation(
            Beneficiary b) {

        String occupation =
                normalise(b.getOccupation());

        if (occupation.isEmpty()) {
            return failed(
                    "Beneficiary occupation not recorded"
            );
        }

        if (occupation.equals("student")
                || occupation.equals("studying")
                || occupation.equals("student/learner")) {

            return passed(
                    30,
                    "Occupation Student → 30/30 points"
            );
        }

        return failed(
                "Occupation " + b.getOccupation()
                        + " does not satisfy NSP Student requirement"
        );
    }
}
