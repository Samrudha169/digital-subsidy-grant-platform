package com.dsgp.eligibility.rules;

import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.eligibility.dto.CriterionResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PMEGP (Prime Minister's Employment Generation Programme) eligibility rule.
 *
 * <p>Evaluates five criteria for the PMEGP scheme (no land criterion):
 * <ol>
 *   <li>Age (18–55, tiered scoring)</li>
 *   <li>Annual income (≤ ₹8,00,000, tiered scoring)</li>
 *   <li>Occupation (any non-blank occupation is accepted)</li>
 *   <li>Category (scheme-defined, 20 points)</li>
 *   <li>Identity verification (10 points)</li>
 * </ol>
 *
 * <p>Mandatory conditions: age and income must pass.
 */
@Component
public class PmegpEligibilityRule extends AbstractEligibilityRule {

    @Override
    public Map<String, CriterionResult> evaluate(
            Beneficiary beneficiary,
            Scheme scheme) {

        CriterionResult age =
                evaluatePmegpAge(beneficiary);

        CriterionResult income =
                evaluatePmegpIncome(beneficiary);

        CriterionResult occupation =
                evaluatePmegpOccupation(beneficiary);

        CriterionResult category =
                evaluateCategory(beneficiary, scheme, 20);

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
                && criteria.get("incomeCheck").isPassed();
    }

    // ========================================================================
    // PMEGP AGE
    // ========================================================================

    private CriterionResult evaluatePmegpAge(
            Beneficiary b) {

        Integer age = b.getAge();

        if (age == null) {
            return failed("Beneficiary age not recorded");
        }

        if (age < 18 || age > 55) {
            return failed(
                    "Age " + age
                            + " is outside PMEGP required range 18-55"
            );
        }

        if (age <= 25) {
            return passed(
                    20,
                    "Age " + age + " → 20/20 points"
            );
        }

        if (age <= 35) {
            return passed(
                    17,
                    "Age " + age + " → 17/20 points"
            );
        }

        if (age <= 45) {
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
    // PMEGP INCOME
    // ========================================================================

    private CriterionResult evaluatePmegpIncome(
            Beneficiary b) {

        BigDecimal income =
                b.getAnnualIncome();

        if (income == null) {
            return failed(
                    "Beneficiary annual income not recorded"
            );
        }

        if (income.compareTo(
                new BigDecimal("800000")) > 0) {

            return failed(
                    "Income ₹" + income
                            + " exceeds PMEGP maximum ₹800000"
            );
        }

        if (income.compareTo(
                new BigDecimal("200000")) <= 0) {

            return passed(
                    30,
                    "Income ₹" + income
                            + " → 30/30 points"
            );
        }

        if (income.compareTo(
                new BigDecimal("400000")) <= 0) {

            return passed(
                    25,
                    "Income ₹" + income
                            + " → 25/30 points"
            );
        }

        if (income.compareTo(
                new BigDecimal("600000")) <= 0) {

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
    // PMEGP OCCUPATION
    // ========================================================================

    private CriterionResult evaluatePmegpOccupation(
            Beneficiary b) {

        String occupation =
                normalise(b.getOccupation());

        if (occupation.isEmpty()) {
            return failed(
                    "Beneficiary occupation not recorded"
            );
        }

        return passed(
                20,
                "Occupation " + b.getOccupation()
                        + " → 20/20 points"
        );
    }
}
