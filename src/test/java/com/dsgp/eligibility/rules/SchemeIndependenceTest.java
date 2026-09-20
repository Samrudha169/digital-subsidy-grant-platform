package com.dsgp.eligibility.rules;

import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.Category;
import com.dsgp.beneficiary.entity.RegistrationStatus;
import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.eligibility.dto.CriterionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that each scheme's {@link EligibilityRule} is evaluated
 * <em>independently</em>: PM-KISAN criteria are never applied when the NSP or
 * PMEGP rule is selected, and vice-versa.
 *
 * <p>These tests work directly against the three concrete rule implementations
 * to eliminate any orchestration noise and assert criterion-level isolation.
 */
@DisplayName("Scheme eligibility independence")
class SchemeIndependenceTest {

    // ── Rules under test ──────────────────────────────────────────────────────

    private final EligibilityRule pmKisanRule = new PmKisanEligibilityRule();
    private final EligibilityRule nspRule     = new NspEligibilityRule();
    private final EligibilityRule pmegpRule   = new PmegpEligibilityRule();

    // ── Scheme stubs ──────────────────────────────────────────────────────────

    private Scheme pmKisanScheme;
    private Scheme nspScheme;
    private Scheme pmegpScheme;

    @BeforeEach
    void buildSchemes() {

        pmKisanScheme = new Scheme();
        pmKisanScheme.setId(1L);
        pmKisanScheme.setSchemeName("PM-KISAN");
        pmKisanScheme.setRequiredCategory("ALL");

        nspScheme = new Scheme();
        nspScheme.setId(2L);
        nspScheme.setSchemeName("NSP");
        nspScheme.setRequiredCategory("ALL");

        pmegpScheme = new Scheme();
        pmegpScheme.setId(3L);
        pmegpScheme.setSchemeName("PMEGP");
        pmegpScheme.setRequiredCategory("ALL");
    }

    // ── Beneficiary factory ───────────────────────────────────────────────────

    /**
     * Builds a base beneficiary with all fields populated.
     * Individual tests override only the fields relevant to their assertion.
     */
    private Beneficiary baseBeneficiary() {
        return Beneficiary.builder()
                .id(1)
                .fullName("Test User")
                .govId("GOV123")
                .contact("9000000000")
                .email("test@example.com")
                .password("secret")
                .age(25)
                .address("Test Address")
                .schemeName("TEST")
                .occupation("Farmer")
                .annualIncome(new BigDecimal("100000"))
                .landHolding(new BigDecimal("1.0"))
                .category(Category.OBC)
                .registrationStatus(RegistrationStatus.ACTIVE)
                .identityVerified(true)
                .build();
    }

    // =========================================================================
    // CRITERION KEY ISOLATION
    // =========================================================================

    @Nested
    @DisplayName("Criterion key isolation")
    class CriterionKeyIsolation {

        @Test
        @DisplayName("PM-KISAN criteria map contains landCheck; NSP and PMEGP do not")
        void pmKisanHasLandCheck_nspAndPmegpDoNot() {

            Beneficiary b = baseBeneficiary();

            Map<String, CriterionResult> pmKisanCriteria =
                    pmKisanRule.evaluate(b, pmKisanScheme);

            Map<String, CriterionResult> nspCriteria =
                    nspRule.evaluate(b, nspScheme);

            Map<String, CriterionResult> pmegpCriteria =
                    pmegpRule.evaluate(b, pmegpScheme);

            assertThat(pmKisanCriteria).containsKey("landCheck");
            assertThat(nspCriteria).doesNotContainKey("landCheck");
            assertThat(pmegpCriteria).doesNotContainKey("landCheck");
        }

        @Test
        @DisplayName("All three rules produce ageCheck, incomeCheck, occupationCheck, categoryCheck, identityCheck")
        void allRulesProduceSharedCriterionKeys() {

            Beneficiary b = baseBeneficiary();
            b.setAge(20);
            b.setOccupation("Farmer");
            b.setAnnualIncome(new BigDecimal("100000"));

            for (EligibilityRule rule : new EligibilityRule[]{pmKisanRule, nspRule, pmegpRule}) {
                Scheme scheme = rule == pmKisanRule ? pmKisanScheme
                        : rule == nspRule ? nspScheme
                        : pmegpScheme;

                Map<String, CriterionResult> criteria = rule.evaluate(b, scheme);

                assertThat(criteria)
                        .containsKeys("ageCheck", "incomeCheck",
                                "occupationCheck", "categoryCheck", "identityCheck");
            }
        }

        @Test
        @DisplayName("PM-KISAN criteria map contains exactly 6 keys")
        void pmKisanProducesSixCriteria() {

            Map<String, CriterionResult> criteria =
                    pmKisanRule.evaluate(baseBeneficiary(), pmKisanScheme);

            assertThat(criteria).hasSize(6);
            assertThat(criteria).containsKeys(
                    "ageCheck", "incomeCheck", "landCheck",
                    "occupationCheck", "categoryCheck", "identityCheck"
            );
        }

        @Test
        @DisplayName("NSP criteria map contains exactly 5 keys (no land)")
        void nspProducesFiveCriteria() {

            Beneficiary b = baseBeneficiary();
            b.setAge(20);
            b.setOccupation("Student");
            b.setAnnualIncome(new BigDecimal("100000"));

            Map<String, CriterionResult> criteria =
                    nspRule.evaluate(b, nspScheme);

            assertThat(criteria).hasSize(5);
            assertThat(criteria).doesNotContainKey("landCheck");
        }

        @Test
        @DisplayName("PMEGP criteria map contains exactly 5 keys (no land)")
        void pmegpProducesFiveCriteria() {

            Beneficiary b = baseBeneficiary();
            b.setAge(30);
            b.setOccupation("Business");
            b.setAnnualIncome(new BigDecimal("200000"));

            Map<String, CriterionResult> criteria =
                    pmegpRule.evaluate(b, pmegpScheme);

            assertThat(criteria).hasSize(5);
            assertThat(criteria).doesNotContainKey("landCheck");
        }
    }

    // =========================================================================
    // OCCUPATION INDEPENDENCE
    // =========================================================================

    @Nested
    @DisplayName("Occupation criterion independence")
    class OccupationIndependence {

        @Test
        @DisplayName("Farmer occupation: eligible for PM-KISAN, ineligible for NSP")
        void farmer_eligibleForPmKisan_ineligibleForNsp() {

            Beneficiary farmer = baseBeneficiary();
            farmer.setAge(25);
            farmer.setOccupation("Farmer");
            farmer.setAnnualIncome(new BigDecimal("100000"));
            farmer.setLandHolding(new BigDecimal("1.0"));

            // PM-KISAN should pass occupationCheck
            Map<String, CriterionResult> pmKisanResult =
                    pmKisanRule.evaluate(farmer, pmKisanScheme);
            assertThat(pmKisanResult.get("occupationCheck").isPassed())
                    .as("Farmer must pass PM-KISAN occupation check")
                    .isTrue();

            // NSP must fail occupationCheck — NSP requires Student
            farmer.setAge(20);           // put inside NSP age range
            farmer.setAnnualIncome(new BigDecimal("100000")); // inside NSP income range
            Map<String, CriterionResult> nspResult =
                    nspRule.evaluate(farmer, nspScheme);
            assertThat(nspResult.get("occupationCheck").isPassed())
                    .as("Farmer must FAIL NSP occupation check (NSP requires Student)")
                    .isFalse();
        }

        @Test
        @DisplayName("Student occupation: eligible for NSP, ineligible for PM-KISAN")
        void student_eligibleForNsp_ineligibleForPmKisan() {

            Beneficiary student = baseBeneficiary();
            student.setAge(20);
            student.setOccupation("Student");
            student.setAnnualIncome(new BigDecimal("100000"));
            student.setLandHolding(new BigDecimal("1.0"));

            // NSP should pass occupationCheck
            Map<String, CriterionResult> nspResult =
                    nspRule.evaluate(student, nspScheme);
            assertThat(nspResult.get("occupationCheck").isPassed())
                    .as("Student must pass NSP occupation check")
                    .isTrue();

            // PM-KISAN must fail occupationCheck — PM-KISAN requires Farmer
            Map<String, CriterionResult> pmKisanResult =
                    pmKisanRule.evaluate(student, pmKisanScheme);
            assertThat(pmKisanResult.get("occupationCheck").isPassed())
                    .as("Student must FAIL PM-KISAN occupation check (requires Farmer)")
                    .isFalse();
        }

        @Test
        @DisplayName("PMEGP accepts any non-blank occupation; PM-KISAN and NSP do not")
        void pmegpAcceptsAnyOccupation() {

            Beneficiary b = baseBeneficiary();
            b.setAge(30);
            b.setAnnualIncome(new BigDecimal("200000"));

            for (String occupation : new String[]{"Teacher", "Doctor", "Trader", "Mechanic"}) {
                b.setOccupation(occupation);

                Map<String, CriterionResult> pmegpResult =
                        pmegpRule.evaluate(b, pmegpScheme);
                assertThat(pmegpResult.get("occupationCheck").isPassed())
                        .as("PMEGP must accept occupation: " + occupation)
                        .isTrue();

                // PM-KISAN must reject all non-farmer occupations
                b.setLandHolding(new BigDecimal("1.0"));
                Map<String, CriterionResult> pmKisanResult =
                        pmKisanRule.evaluate(b, pmKisanScheme);
                assertThat(pmKisanResult.get("occupationCheck").isPassed())
                        .as("PM-KISAN must reject occupation: " + occupation)
                        .isFalse();

                // NSP must reject all non-student occupations
                b.setAge(20);
                Map<String, CriterionResult> nspResult =
                        nspRule.evaluate(b, nspScheme);
                assertThat(nspResult.get("occupationCheck").isPassed())
                        .as("NSP must reject occupation: " + occupation)
                        .isFalse();
            }
        }
    }

    // =========================================================================
    // AGE THRESHOLD INDEPENDENCE
    // =========================================================================

    @Nested
    @DisplayName("Age threshold independence")
    class AgeThresholdIndependence {

        @Test
        @DisplayName("Age 35: valid for PM-KISAN and PMEGP, invalid for NSP")
        void age35_pmKisanPmegpEligible_nspIneligible() {

            Beneficiary b = baseBeneficiary();
            b.setAge(35);

            // PM-KISAN range 18-70 → 35 should pass
            b.setOccupation("Farmer");
            b.setLandHolding(new BigDecimal("1.0"));
            Map<String, CriterionResult> pmKisanResult =
                    pmKisanRule.evaluate(b, pmKisanScheme);
            assertThat(pmKisanResult.get("ageCheck").isPassed())
                    .as("Age 35 must pass PM-KISAN age check (range 18-70)")
                    .isTrue();

            // PMEGP range 18-55 → 35 should pass
            b.setOccupation("Business");
            Map<String, CriterionResult> pmegpResult =
                    pmegpRule.evaluate(b, pmegpScheme);
            assertThat(pmegpResult.get("ageCheck").isPassed())
                    .as("Age 35 must pass PMEGP age check (range 18-55)")
                    .isTrue();

            // NSP range 15-30 → 35 must fail
            b.setOccupation("Student");
            Map<String, CriterionResult> nspResult =
                    nspRule.evaluate(b, nspScheme);
            assertThat(nspResult.get("ageCheck").isPassed())
                    .as("Age 35 must FAIL NSP age check (range 15-30)")
                    .isFalse();
        }

        @Test
        @DisplayName("Age 58: valid for PM-KISAN only (PMEGP max 55, NSP max 30)")
        void age58_onlyPmKisanEligible() {

            Beneficiary b = baseBeneficiary();
            b.setAge(58);
            b.setOccupation("Farmer");
            b.setLandHolding(new BigDecimal("1.0"));
            b.setAnnualIncome(new BigDecimal("100000"));

            Map<String, CriterionResult> pmKisanResult =
                    pmKisanRule.evaluate(b, pmKisanScheme);
            assertThat(pmKisanResult.get("ageCheck").isPassed())
                    .as("Age 58 must pass PM-KISAN age check (range 18-70)")
                    .isTrue();

            b.setOccupation("Business");
            Map<String, CriterionResult> pmegpResult =
                    pmegpRule.evaluate(b, pmegpScheme);
            assertThat(pmegpResult.get("ageCheck").isPassed())
                    .as("Age 58 must FAIL PMEGP age check (max 55)")
                    .isFalse();

            b.setOccupation("Student");
            Map<String, CriterionResult> nspResult =
                    nspRule.evaluate(b, nspScheme);
            assertThat(nspResult.get("ageCheck").isPassed())
                    .as("Age 58 must FAIL NSP age check (max 30)")
                    .isFalse();
        }
    }

    // =========================================================================
    // INCOME THRESHOLD INDEPENDENCE
    // =========================================================================

    @Nested
    @DisplayName("Income threshold independence")
    class IncomeThresholdIndependence {

        @Test
        @DisplayName("Income 3 lakh: eligible for PMEGP, ineligible for PM-KISAN and NSP")
        void income3Lakh_onlyPmegpEligible() {

            BigDecimal income = new BigDecimal("300000");

            // PM-KISAN max 3 lakh → exactly 3L fails (> 300000)
            Beneficiary b = baseBeneficiary();
            b.setOccupation("Farmer");
            b.setLandHolding(new BigDecimal("1.0"));
            b.setAnnualIncome(new BigDecimal("350000"));

            Map<String, CriterionResult> pmKisanResult =
                    pmKisanRule.evaluate(b, pmKisanScheme);
            assertThat(pmKisanResult.get("incomeCheck").isPassed())
                    .as("Income 3.5L must FAIL PM-KISAN income check (max 3L)")
                    .isFalse();

            // NSP max 2.5 lakh → 3L fails
            b.setAge(20);
            b.setOccupation("Student");
            b.setAnnualIncome(income);
            Map<String, CriterionResult> nspResult =
                    nspRule.evaluate(b, nspScheme);
            assertThat(nspResult.get("incomeCheck").isPassed())
                    .as("Income 3L must FAIL NSP income check (max 2.5L)")
                    .isFalse();

            // PMEGP max 8 lakh → 3L passes
            b.setAge(30);
            b.setOccupation("Business");
            b.setAnnualIncome(income);
            Map<String, CriterionResult> pmegpResult =
                    pmegpRule.evaluate(b, pmegpScheme);
            assertThat(pmegpResult.get("incomeCheck").isPassed())
                    .as("Income 3L must pass PMEGP income check (max 8L)")
                    .isTrue();
        }

        @Test
        @DisplayName("Income 5 lakh: eligible for PMEGP only")
        void income5Lakh_onlyPmegpEligible() {

            BigDecimal income = new BigDecimal("500000");

            Beneficiary b = baseBeneficiary();
            b.setOccupation("Farmer");
            b.setLandHolding(new BigDecimal("1.0"));
            b.setAnnualIncome(income);

            Map<String, CriterionResult> pmKisanResult =
                    pmKisanRule.evaluate(b, pmKisanScheme);
            assertThat(pmKisanResult.get("incomeCheck").isPassed())
                    .as("Income 5L must FAIL PM-KISAN (max 3L)")
                    .isFalse();

            b.setAge(20);
            b.setOccupation("Student");
            Map<String, CriterionResult> nspResult =
                    nspRule.evaluate(b, nspScheme);
            assertThat(nspResult.get("incomeCheck").isPassed())
                    .as("Income 5L must FAIL NSP (max 2.5L)")
                    .isFalse();

            b.setAge(30);
            b.setOccupation("Trader");
            Map<String, CriterionResult> pmegpResult =
                    pmegpRule.evaluate(b, pmegpScheme);
            assertThat(pmegpResult.get("incomeCheck").isPassed())
                    .as("Income 5L must pass PMEGP (max 8L)")
                    .isTrue();
        }
    }

    // =========================================================================
    // LAND HOLDING INDEPENDENCE
    // =========================================================================

    @Nested
    @DisplayName("Land holding independence")
    class LandHoldingIndependence {

        @Test
        @DisplayName("Null land holding: fails PM-KISAN mandatory, irrelevant to NSP and PMEGP")
        void nullLand_failsPmKisan_irrelevantToNspAndPmegp() {

            Beneficiary b = baseBeneficiary();
            b.setLandHolding(null);
            b.setOccupation("Farmer");

            Map<String, CriterionResult> pmKisanResult =
                    pmKisanRule.evaluate(b, pmKisanScheme);

            // PM-KISAN must fail — land is a mandatory criterion
            assertThat(pmKisanResult.get("landCheck").isPassed())
                    .as("Null land must FAIL PM-KISAN landCheck")
                    .isFalse();
            assertThat(pmKisanRule.mandatoryConditionsPassed(pmKisanResult))
                    .as("Null land must fail PM-KISAN mandatory conditions")
                    .isFalse();

            // NSP has no landCheck at all
            b.setAge(20);
            b.setOccupation("Student");
            b.setAnnualIncome(new BigDecimal("100000"));
            Map<String, CriterionResult> nspResult =
                    nspRule.evaluate(b, nspScheme);
            assertThat(nspResult).doesNotContainKey("landCheck");
            assertThat(nspRule.mandatoryConditionsPassed(nspResult))
                    .as("NSP mandatory conditions must not depend on land holding")
                    .isTrue();

            // PMEGP has no landCheck at all
            b.setAge(30);
            b.setOccupation("Business");
            b.setAnnualIncome(new BigDecimal("200000"));
            Map<String, CriterionResult> pmegpResult =
                    pmegpRule.evaluate(b, pmegpScheme);
            assertThat(pmegpResult).doesNotContainKey("landCheck");
            assertThat(pmegpRule.mandatoryConditionsPassed(pmegpResult))
                    .as("PMEGP mandatory conditions must not depend on land holding")
                    .isTrue();
        }

        @Test
        @DisplayName("Large land holding: fails PM-KISAN, irrelevant to NSP and PMEGP")
        void largeLand_failsPmKisan_irrelevantToNspAndPmegp() {

            Beneficiary b = baseBeneficiary();
            b.setLandHolding(new BigDecimal("10.0"));  // over PM-KISAN 5-acre limit
            b.setOccupation("Farmer");
            b.setAnnualIncome(new BigDecimal("100000"));

            Map<String, CriterionResult> pmKisanResult =
                    pmKisanRule.evaluate(b, pmKisanScheme);
            assertThat(pmKisanResult.get("landCheck").isPassed())
                    .as("10 acres must FAIL PM-KISAN landCheck (max 5 acres)")
                    .isFalse();

            // NSP: land is totally ignored
            b.setAge(20);
            b.setOccupation("Student");
            Map<String, CriterionResult> nspResult =
                    nspRule.evaluate(b, nspScheme);
            assertThat(nspResult).doesNotContainKey("landCheck");

            // PMEGP: land is totally ignored
            b.setAge(30);
            b.setOccupation("Business");
            Map<String, CriterionResult> pmegpResult =
                    pmegpRule.evaluate(b, pmegpScheme);
            assertThat(pmegpResult).doesNotContainKey("landCheck");
        }
    }

    // =========================================================================
    // MANDATORY CONDITIONS INDEPENDENCE
    // =========================================================================

    @Nested
    @DisplayName("Mandatory conditions independence")
    class MandatoryConditionsIndependence {

        @Test
        @DisplayName("PM-KISAN mandatory: age AND income AND land AND occupation")
        void pmKisanMandatoryRequiresFourConditions() {

            // All pass
            Beneficiary allPass = baseBeneficiary();
            allPass.setOccupation("Farmer");
            Map<String, CriterionResult> criteria =
                    pmKisanRule.evaluate(allPass, pmKisanScheme);
            assertThat(pmKisanRule.mandatoryConditionsPassed(criteria)).isTrue();

            // Fail land only
            allPass.setLandHolding(new BigDecimal("6.0"));
            criteria = pmKisanRule.evaluate(allPass, pmKisanScheme);
            assertThat(pmKisanRule.mandatoryConditionsPassed(criteria))
                    .as("PM-KISAN mandatory fails when land > 5 acres")
                    .isFalse();

            // Fail occupation only
            allPass.setLandHolding(new BigDecimal("1.0"));
            allPass.setOccupation("Student");
            criteria = pmKisanRule.evaluate(allPass, pmKisanScheme);
            assertThat(pmKisanRule.mandatoryConditionsPassed(criteria))
                    .as("PM-KISAN mandatory fails when occupation is not Farmer")
                    .isFalse();
        }

        @Test
        @DisplayName("NSP mandatory: age AND income AND occupation (land is never mandatory)")
        void nspMandatoryRequiresThreeConditions_notLand() {

            Beneficiary student = baseBeneficiary();
            student.setAge(20);
            student.setOccupation("Student");
            student.setAnnualIncome(new BigDecimal("100000"));
            student.setLandHolding(null);  // land irrelevant

            Map<String, CriterionResult> criteria =
                    nspRule.evaluate(student, nspScheme);
            assertThat(nspRule.mandatoryConditionsPassed(criteria))
                    .as("NSP mandatory must pass without land holding")
                    .isTrue();

            // Fail occupation only
            student.setOccupation("Farmer");
            criteria = nspRule.evaluate(student, nspScheme);
            assertThat(nspRule.mandatoryConditionsPassed(criteria))
                    .as("NSP mandatory fails when occupation is not Student")
                    .isFalse();

            // Fail income only
            student.setOccupation("Student");
            student.setAnnualIncome(new BigDecimal("300000")); // > NSP max 2.5L
            criteria = nspRule.evaluate(student, nspScheme);
            assertThat(nspRule.mandatoryConditionsPassed(criteria))
                    .as("NSP mandatory fails when income > 2.5L")
                    .isFalse();
        }

        @Test
        @DisplayName("PMEGP mandatory: age AND income only (occupation and land are NOT mandatory gates)")
        void pmegpMandatoryRequiresTwoConditions() {

            Beneficiary b = baseBeneficiary();
            b.setAge(30);
            b.setOccupation("Trader");
            b.setAnnualIncome(new BigDecimal("200000"));
            b.setLandHolding(null);

            Map<String, CriterionResult> criteria =
                    pmegpRule.evaluate(b, pmegpScheme);
            assertThat(pmegpRule.mandatoryConditionsPassed(criteria))
                    .as("PMEGP mandatory must pass for any non-blank occupation")
                    .isTrue();

            // Fail age
            b.setAge(60);  // > PMEGP max 55
            criteria = pmegpRule.evaluate(b, pmegpScheme);
            assertThat(pmegpRule.mandatoryConditionsPassed(criteria))
                    .as("PMEGP mandatory fails when age > 55")
                    .isFalse();

            // Fail income
            b.setAge(30);
            b.setAnnualIncome(new BigDecimal("900000")); // > PMEGP max 8L
            criteria = pmegpRule.evaluate(b, pmegpScheme);
            assertThat(pmegpRule.mandatoryConditionsPassed(criteria))
                    .as("PMEGP mandatory fails when income > 8L")
                    .isFalse();
        }
    }

    // =========================================================================
    // ELIGIBLE vs INELIGIBLE — per scheme
    // =========================================================================

    @Nested
    @DisplayName("Eligible and ineligible beneficiaries per scheme")
    class EligibleAndIneligible {

        @Test
        @DisplayName("PM-KISAN: ideal farmer is eligible; non-farmer is ineligible")
        void pmKisan_eligibleAndIneligible() {

            // ELIGIBLE
            Beneficiary eligible = baseBeneficiary();
            eligible.setAge(25);
            eligible.setOccupation("Farmer");
            eligible.setAnnualIncome(new BigDecimal("100000"));
            eligible.setLandHolding(new BigDecimal("1.0"));

            Map<String, CriterionResult> eligibleCriteria =
                    pmKisanRule.evaluate(eligible, pmKisanScheme);
            assertThat(pmKisanRule.mandatoryConditionsPassed(eligibleCriteria)).isTrue();
            int eligibleScore = eligibleCriteria.values().stream()
                    .mapToInt(CriterionResult::getPoints).sum();
            assertThat(eligibleScore).isGreaterThanOrEqualTo(60);

            // INELIGIBLE — non-farmer
            Beneficiary ineligible = baseBeneficiary();
            ineligible.setAge(25);
            ineligible.setOccupation("Shopkeeper");
            ineligible.setAnnualIncome(new BigDecimal("100000"));
            ineligible.setLandHolding(new BigDecimal("1.0"));

            Map<String, CriterionResult> ineligibleCriteria =
                    pmKisanRule.evaluate(ineligible, pmKisanScheme);
            assertThat(pmKisanRule.mandatoryConditionsPassed(ineligibleCriteria))
                    .as("Non-farmer must fail PM-KISAN mandatory conditions")
                    .isFalse();
        }

        @Test
        @DisplayName("NSP: qualifying student is eligible; over-age student is ineligible")
        void nsp_eligibleAndIneligible() {

            // ELIGIBLE — 18-year-old student, income 1L
            Beneficiary eligible = baseBeneficiary();
            eligible.setAge(18);
            eligible.setOccupation("Student");
            eligible.setAnnualIncome(new BigDecimal("100000"));
            eligible.setLandHolding(null);

            Map<String, CriterionResult> eligibleCriteria =
                    nspRule.evaluate(eligible, nspScheme);
            assertThat(nspRule.mandatoryConditionsPassed(eligibleCriteria)).isTrue();
            int eligibleScore = eligibleCriteria.values().stream()
                    .mapToInt(CriterionResult::getPoints).sum();
            assertThat(eligibleScore).isGreaterThanOrEqualTo(60);

            // INELIGIBLE — age 40 (outside NSP range 15-30)
            Beneficiary ineligible = baseBeneficiary();
            ineligible.setAge(40);
            ineligible.setOccupation("Student");
            ineligible.setAnnualIncome(new BigDecimal("100000"));
            ineligible.setLandHolding(null);

            Map<String, CriterionResult> ineligibleCriteria =
                    nspRule.evaluate(ineligible, nspScheme);
            assertThat(nspRule.mandatoryConditionsPassed(ineligibleCriteria))
                    .as("Age 40 student must fail NSP mandatory conditions")
                    .isFalse();
        }

        @Test
        @DisplayName("PMEGP: qualifying entrepreneur is eligible; over-income applicant is ineligible")
        void pmegp_eligibleAndIneligible() {

            // ELIGIBLE — 30, income 2L, any occupation
            Beneficiary eligible = baseBeneficiary();
            eligible.setAge(30);
            eligible.setOccupation("Tailor");
            eligible.setAnnualIncome(new BigDecimal("200000"));
            eligible.setLandHolding(null);

            Map<String, CriterionResult> eligibleCriteria =
                    pmegpRule.evaluate(eligible, pmegpScheme);
            assertThat(pmegpRule.mandatoryConditionsPassed(eligibleCriteria)).isTrue();
            int eligibleScore = eligibleCriteria.values().stream()
                    .mapToInt(CriterionResult::getPoints).sum();
            assertThat(eligibleScore).isGreaterThanOrEqualTo(60);

            // INELIGIBLE — income > 8L
            Beneficiary ineligible = baseBeneficiary();
            ineligible.setAge(30);
            ineligible.setOccupation("Trader");
            ineligible.setAnnualIncome(new BigDecimal("1000000")); // 10 lakh
            ineligible.setLandHolding(null);

            Map<String, CriterionResult> ineligibleCriteria =
                    pmegpRule.evaluate(ineligible, pmegpScheme);
            assertThat(pmegpRule.mandatoryConditionsPassed(ineligibleCriteria))
                    .as("Income > 8L must fail PMEGP mandatory conditions")
                    .isFalse();
        }
    }
}
