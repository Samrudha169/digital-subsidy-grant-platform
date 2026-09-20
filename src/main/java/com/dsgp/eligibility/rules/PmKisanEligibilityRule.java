package com.dsgp.eligibility.rules;

import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.eligibility.dto.CriterionResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PM-KISAN eligibility rule.
 *
 * <p>Evaluates six criteria for the PM-KISAN scheme:
 * <ol>
 *   <li>Age (18–70, tiered scoring)</li>
 *   <li>Annual income (≤ ₹3,00,000, tiered scoring)</li>
 *   <li>Land holding (≤ 5 acres, tiered scoring)</li>
 *   <li>Occupation (must be Farmer / Agriculture / Agriculturist)</li>
 *   <li>Category (scheme-defined, 5 points)</li>
 *   <li>Identity verification (10 points)</li>
 * </ol>
 *
 * <p>Mandatory conditions: age, income, land holding, and occupation must
 * all pass.
 */
@Component
public class PmKisanEligibilityRule extends AbstractEligibilityRule {

    @Override
    public Map<String, CriterionResult> evaluate(
            Beneficiary beneficiary,
            Scheme scheme) {

        CriterionResult age =
                evaluatePmKisanAge(beneficiary);

        CriterionResult income =
                evaluatePmKisanIncome(beneficiary);

        CriterionResult land =
                evaluatePmKisanLand(beneficiary);

        CriterionResult occupation =
                evaluatePmKisanOccupation(beneficiary);

        CriterionResult category =
                evaluateCategory(beneficiary, scheme, 5);

        CriterionResult identity =
                evaluateIdentity(beneficiary, 10);

        Map<String, CriterionResult> criteria =
                new LinkedHashMap<>();

        criteria.put("ageCheck", age);
        criteria.put("incomeCheck", income);
        criteria.put("landCheck", land);
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
                && criteria.get("landCheck").isPassed()
                && criteria.get("occupationCheck").isPassed();
    }

    // ========================================================================
    // PM-KISAN AGE
    // ========================================================================

    private CriterionResult evaluatePmKisanAge(
            Beneficiary b) {

        Integer age = b.getAge();

        if (age == null) {
            return failed("Beneficiary age not recorded");
        }

        if (age < 18 || age > 70) {
            return failed(
                    "Age " + age
                            + " is outside PM-KISAN required range 18-70"
            );
        }

        if (age <= 30) {
            return passed(
                    15,
                    "Age " + age + " → 15/15 points"
            );
        }

        if (age <= 45) {
            return passed(
                    12,
                    "Age " + age + " → 12/15 points"
            );
        }

        if (age <= 60) {
            return passed(
                    9,
                    "Age " + age + " → 9/15 points"
            );
        }

        return passed(
                6,
                "Age " + age + " → 6/15 points"
        );
    }

    // ========================================================================
    // PM-KISAN INCOME
    // ========================================================================

    private CriterionResult evaluatePmKisanIncome(
            Beneficiary b) {

        BigDecimal income = b.getAnnualIncome();

        if (income == null) {
            return failed(
                    "Beneficiary annual income not recorded"
            );
        }

        if (income.compareTo(
                new BigDecimal("300000")) > 0) {

            return failed(
                    "Income ₹" + income
                            + " exceeds PM-KISAN maximum ₹300000"
            );
        }

        if (income.compareTo(
                new BigDecimal("100000")) <= 0) {

            return passed(
                    25,
                    "Income ₹" + income
                            + " → 25/25 points"
            );
        }

        if (income.compareTo(
                new BigDecimal("150000")) <= 0) {

            return passed(
                    20,
                    "Income ₹" + income
                            + " → 20/25 points"
            );
        }

        if (income.compareTo(
                new BigDecimal("200000")) <= 0) {

            return passed(
                    15,
                    "Income ₹" + income
                            + " → 15/25 points"
            );
        }

        if (income.compareTo(
                new BigDecimal("250000")) <= 0) {

            return passed(
                    10,
                    "Income ₹" + income
                            + " → 10/25 points"
            );
        }

        return passed(
                5,
                "Income ₹" + income
                        + " → 5/25 points"
        );
    }

    // ========================================================================
    // PM-KISAN LAND HOLDING
    // ========================================================================

    private CriterionResult evaluatePmKisanLand(
            Beneficiary b) {

        BigDecimal land = b.getLandHolding();

        if (land == null) {
            return failed(
                    "Beneficiary land holding not recorded"
            );
        }

        if (land.compareTo(
                new BigDecimal("5")) > 0) {

            return failed(
                    "Land holding " + land
                            + " acres exceeds PM-KISAN maximum 5 acres"
            );
        }

        if (land.compareTo(
                new BigDecimal("1")) <= 0) {

            return passed(
                    25,
                    "Land holding " + land
                            + " acres → 25/25 points"
            );
        }

        if (land.compareTo(
                new BigDecimal("2")) <= 0) {

            return passed(
                    20,
                    "Land holding " + land
                            + " acres → 20/25 points"
            );
        }

        if (land.compareTo(
                new BigDecimal("3")) <= 0) {

            return passed(
                    15,
                    "Land holding " + land
                            + " acres → 15/25 points"
            );
        }

        if (land.compareTo(
                new BigDecimal("4")) <= 0) {

            return passed(
                    10,
                    "Land holding " + land
                            + " acres → 10/25 points"
            );
        }

        return passed(
                5,
                "Land holding " + land
                        + " acres → 5/25 points"
        );
    }

    // ========================================================================
    // PM-KISAN OCCUPATION
    // ========================================================================

    private CriterionResult evaluatePmKisanOccupation(
            Beneficiary b) {

        String occupation =
                normalise(b.getOccupation());

        if (occupation.isEmpty()) {
            return failed(
                    "Beneficiary occupation not recorded"
            );
        }

        if (occupation.equals("farmer")
                || occupation.equals("agriculture")
                || occupation.equals("agriculturist")) {

            return passed(
                    20,
                    "Occupation Farmer → 20/20 points"
            );
        }

        return failed(
                "Occupation " + b.getOccupation()
                        + " does not satisfy PM-KISAN Farmer requirement"
        );
    }
}
