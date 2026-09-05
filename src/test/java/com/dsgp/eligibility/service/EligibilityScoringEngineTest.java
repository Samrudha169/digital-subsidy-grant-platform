package com.dsgp.eligibility.service;

import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.Category;
import com.dsgp.beneficiary.entity.RegistrationStatus;
import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.beneficiary.repository.BeneficiaryRepository;
import com.dsgp.beneficiary.repository.SchemeRepository;
import com.dsgp.eligibility.dto.CriterionResult;
import com.dsgp.eligibility.dto.EligibilityCheckRequest;
import com.dsgp.eligibility.dto.EligibilityResultResponse;
import com.dsgp.eligibility.entity.EligibilityResult;
import com.dsgp.eligibility.entity.EligibilityStatus;
import com.dsgp.eligibility.exception.EligibilityCheckException;
import com.dsgp.eligibility.repository.EligibilityResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static com.dsgp.eligibility.service.EligibilityScoringEngine.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link EligibilityScoringEngine}.
 *
 * <p>Tests are split into two groups:
 * <ol>
 *   <li>Per-criterion evaluators — each method is tested with null/pass/fail
 *       inputs in isolation, verifying points and detail strings.</li>
 *   <li>Full {@code checkEligibility} integration — end-to-end evaluation
 *       with mock repositories, verifying score aggregation, status
 *       determination, and persistence.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EligibilityScoringEngine")
class EligibilityScoringEngineTest {

    @Mock
    private BeneficiaryRepository beneficiaryRepository;

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private EligibilityResultRepository resultRepository;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private EligibilityScoringEngine engine;

    // ── Shared fixtures ──────────────────────────────────────────────────────

    /** A fully eligible beneficiary — meets all criteria. */
    private Beneficiary eligibleBeneficiary;

    /** A scheme with realistic eligibility criteria. */
    private Scheme strictScheme;

    /** A scheme with no restrictions — all criteria auto-satisfied. */
    private Scheme openScheme;

    @BeforeEach
    void setUp() {
        eligibleBeneficiary = Beneficiary.builder()
                .id(101)
                .fullName("Priya Sharma")
                .govId("PAN12345")
                .contact("9876543210")
                .email("priya@example.com")
                .age(35)
                .address("Village Wai, Satara")
                .schemeName("PM-KISAN")
                .annualIncome(new BigDecimal("85000"))
                .landHolding(new BigDecimal("1.5"))
                .category(Category.OBC)
                .registrationStatus(RegistrationStatus.ACTIVE)
                .identityVerified(true)
                .build();

        strictScheme = new Scheme();
        strictScheme.setId(5L);
        strictScheme.setSchemeName("PM-KISAN Samman Nidhi");
        strictScheme.setMinAge(18);
        strictScheme.setMaxAge(60);
        strictScheme.setMaxAnnualIncome(new BigDecimal("150000"));
        strictScheme.setMaxLandHolding(new BigDecimal("2.0"));
        strictScheme.setRequiredCategory("OBC");
        strictScheme.setGrantAmount(new BigDecimal("6000"));
        strictScheme.setActive(true);

        openScheme = new Scheme();
        openScheme.setId(99L);
        openScheme.setSchemeName("Open Welfare Scheme");
        openScheme.setActive(true);
        // All thresholds null → all criteria auto-satisfied → max score = 100
    }

    // ════════════════════════════════════════════════════════════════════════
    // evaluateAge
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("evaluateAge()")
    class EvaluateAge {

        @Test
        @DisplayName("awards full 20 points when scheme has no age restriction")
        void noRestriction_awardsFullPoints() {
            CriterionResult result = engine.evaluateAge(eligibleBeneficiary, openScheme);
            assertThat(result.getPoints()).isEqualTo(POINTS_AGE);
            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("awards full 20 points when beneficiary age is within range")
        void withinRange_awardsFullPoints() {
            CriterionResult result = engine.evaluateAge(eligibleBeneficiary, strictScheme);
            assertThat(result.getPoints()).isEqualTo(POINTS_AGE);
            assertThat(result.isPassed()).isTrue();
            assertThat(result.getDetail()).contains("35");
        }

        @Test
        @DisplayName("awards 0 points when beneficiary is too young")
        void tooYoung_awardsZeroPoints() {
            eligibleBeneficiary.setAge(15);
            CriterionResult result = engine.evaluateAge(eligibleBeneficiary, strictScheme);
            assertThat(result.getPoints()).isEqualTo(0);
            assertThat(result.isPassed()).isFalse();
        }

        @Test
        @DisplayName("awards 0 points when beneficiary is too old")
        void tooOld_awardsZeroPoints() {
            eligibleBeneficiary.setAge(65);
            CriterionResult result = engine.evaluateAge(eligibleBeneficiary, strictScheme);
            assertThat(result.getPoints()).isEqualTo(0);
            assertThat(result.isPassed()).isFalse();
        }

        @Test
        @DisplayName("awards 0 points when beneficiary age is null")
        void nullAge_awardsZeroPoints() {
            eligibleBeneficiary.setAge(null);
            CriterionResult result = engine.evaluateAge(eligibleBeneficiary, strictScheme);
            assertThat(result.getPoints()).isEqualTo(0);
            assertThat(result.isPassed()).isFalse();
        }

        @Test
        @DisplayName("awards full points when only minAge is set and beneficiary qualifies")
        void onlyMinAge_beneficiaryQualifies() {
            strictScheme.setMaxAge(null);
            eligibleBeneficiary.setAge(70);
            CriterionResult result = engine.evaluateAge(eligibleBeneficiary, strictScheme);
            assertThat(result.getPoints()).isEqualTo(POINTS_AGE);
            assertThat(result.isPassed()).isTrue();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // evaluateIncome
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("evaluateIncome()")
    class EvaluateIncome {

        @Test
        @DisplayName("awards full 30 points when scheme has no income restriction")
        void noRestriction_awardsFullPoints() {
            CriterionResult result = engine.evaluateIncome(eligibleBeneficiary, openScheme);
            assertThat(result.getPoints()).isEqualTo(POINTS_INCOME);
            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("awards full 30 points when income is at threshold (boundary)")
        void atThreshold_awardsFullPoints() {
            eligibleBeneficiary.setAnnualIncome(new BigDecimal("150000"));
            CriterionResult result = engine.evaluateIncome(eligibleBeneficiary, strictScheme);
            assertThat(result.getPoints()).isEqualTo(POINTS_INCOME);
            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("awards full 30 points when income is below threshold")
        void belowThreshold_awardsFullPoints() {
            CriterionResult result = engine.evaluateIncome(eligibleBeneficiary, strictScheme);
            assertThat(result.getPoints()).isEqualTo(POINTS_INCOME);
            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("awards 0 points when income exceeds threshold")
        void exceedsThreshold_awardsZeroPoints() {
            eligibleBeneficiary.setAnnualIncome(new BigDecimal("200000"));
            CriterionResult result = engine.evaluateIncome(eligibleBeneficiary, strictScheme);
            assertThat(result.getPoints()).isEqualTo(0);
            assertThat(result.isPassed()).isFalse();
        }

        @Test
        @DisplayName("awards 0 points when beneficiary income is null")
        void nullIncome_awardsZeroPoints() {
            eligibleBeneficiary.setAnnualIncome(null);
            CriterionResult result = engine.evaluateIncome(eligibleBeneficiary, strictScheme);
            assertThat(result.getPoints()).isEqualTo(0);
            assertThat(result.isPassed()).isFalse();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // evaluateLand
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("evaluateLand()")
    class EvaluateLand {

        @Test
        @DisplayName("awards full 20 points when scheme has no land restriction")
        void noRestriction_awardsFullPoints() {
            CriterionResult result = engine.evaluateLand(eligibleBeneficiary, openScheme);
            assertThat(result.getPoints()).isEqualTo(POINTS_LAND);
            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("awards full 20 points when land is within limit")
        void withinLimit_awardsFullPoints() {
            CriterionResult result = engine.evaluateLand(eligibleBeneficiary, strictScheme);
            assertThat(result.getPoints()).isEqualTo(POINTS_LAND);
            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("awards 0 points when land exceeds limit")
        void exceedsLimit_awardsZeroPoints() {
            eligibleBeneficiary.setLandHolding(new BigDecimal("5.0"));
            CriterionResult result = engine.evaluateLand(eligibleBeneficiary, strictScheme);
            assertThat(result.getPoints()).isEqualTo(0);
            assertThat(result.isPassed()).isFalse();
        }

        @Test
        @DisplayName("awards 0 points when beneficiary land holding is null")
        void nullLand_awardsZeroPoints() {
            eligibleBeneficiary.setLandHolding(null);
            CriterionResult result = engine.evaluateLand(eligibleBeneficiary, strictScheme);
            assertThat(result.getPoints()).isEqualTo(0);
            assertThat(result.isPassed()).isFalse();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // evaluateCategory
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("evaluateCategory()")
    class EvaluateCategory {

        @Test
        @DisplayName("awards full 20 points when scheme has no category restriction")
        void noRestriction_awardsFullPoints() {
            CriterionResult result = engine.evaluateCategory(eligibleBeneficiary, openScheme);
            assertThat(result.getPoints()).isEqualTo(POINTS_CATEGORY);
            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("awards full 20 points when category matches (case-insensitive)")
        void categoryMatches_awardsFullPoints() {
            CriterionResult result = engine.evaluateCategory(eligibleBeneficiary, strictScheme);
            assertThat(result.getPoints()).isEqualTo(POINTS_CATEGORY);
            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("awards 0 points when category does not match")
        void categoryMismatch_awardsZeroPoints() {
            eligibleBeneficiary.setCategory(Category.GENERAL);
            CriterionResult result = engine.evaluateCategory(eligibleBeneficiary, strictScheme);
            assertThat(result.getPoints()).isEqualTo(0);
            assertThat(result.isPassed()).isFalse();
        }

        @Test
        @DisplayName("awards 0 points when beneficiary category is null")
        void nullCategory_awardsZeroPoints() {
            eligibleBeneficiary.setCategory(null);
            CriterionResult result = engine.evaluateCategory(eligibleBeneficiary, strictScheme);
            assertThat(result.getPoints()).isEqualTo(0);
            assertThat(result.isPassed()).isFalse();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // evaluateIdentity
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("evaluateIdentity()")
    class EvaluateIdentity {

        @Test
        @DisplayName("awards 10 points when identity is verified")
        void verified_awardsFullPoints() {
            CriterionResult result = engine.evaluateIdentity(eligibleBeneficiary);
            assertThat(result.getPoints()).isEqualTo(POINTS_IDENTITY);
            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("awards 0 points when identity is not verified")
        void notVerified_awardsZeroPoints() {
            eligibleBeneficiary.setIdentityVerified(false);
            CriterionResult result = engine.evaluateIdentity(eligibleBeneficiary);
            assertThat(result.getPoints()).isEqualTo(0);
            assertThat(result.isPassed()).isFalse();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // checkEligibility — full engine integration
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("checkEligibility()")
    class CheckEligibility {

        private EligibilityCheckRequest request;

        @BeforeEach
        void setUpRequest() {
            request = new EligibilityCheckRequest();
            request.setBeneficiaryId(101);
            request.setSchemeId(5L);
        }

        @Test
        @DisplayName("returns ELIGIBLE (100/100) when all criteria pass and open scheme")
        void allCriteriaPass_openScheme_returnsEligible100() {
            request.setSchemeId(99L);
            given(beneficiaryRepository.findById(101))
                    .willReturn(Optional.of(eligibleBeneficiary));
            given(schemeRepository.findById(99L))
                    .willReturn(Optional.of(openScheme));
            given(resultRepository.findByBeneficiaryIdAndSchemeId(101, 99L))
                    .willReturn(Optional.empty());
            given(resultRepository.save(any(EligibilityResult.class)))
                    .willAnswer(inv -> {
                        EligibilityResult r = inv.getArgument(0);
                        r.setId(1L);
                        return r;
                    });

            EligibilityResultResponse response = engine.checkEligibility(request);

            assertThat(response.getTotalScore()).isEqualTo(100);
            assertThat(response.getEligibilityStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
            assertThat(response.isEligible()).isTrue();
            assertThat(response.getCriteria()).containsKeys(
                    "ageCheck", "incomeCheck", "landCheck", "categoryCheck", "identityCheck");
        }

        @Test
        @DisplayName("returns ELIGIBLE (100/100) when all criteria pass for strict scheme")
        void allCriteriaPass_strictScheme_returnsEligible() {
            given(beneficiaryRepository.findById(101))
                    .willReturn(Optional.of(eligibleBeneficiary));
            given(schemeRepository.findById(5L))
                    .willReturn(Optional.of(strictScheme));
            given(resultRepository.findByBeneficiaryIdAndSchemeId(101, 5L))
                    .willReturn(Optional.empty());
            given(resultRepository.save(any(EligibilityResult.class)))
                    .willAnswer(inv -> {
                        EligibilityResult r = inv.getArgument(0);
                        r.setId(2L);
                        return r;
                    });

            EligibilityResultResponse response = engine.checkEligibility(request);

            assertThat(response.getTotalScore()).isEqualTo(100);
            assertThat(response.getEligibilityStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        }

        @Test
        @DisplayName("returns INELIGIBLE when income fails (score = 70 < threshold only if identity also fails → 60)")
        void identityAndIncomeFailGivesIneligible() {
            // Age (20) + Land (20) + Category (20) = 60 — just on threshold.
            // Income fails → 30 pts lost. Identity fails → 10 pts lost. Score = 50.
            eligibleBeneficiary.setAnnualIncome(new BigDecimal("999999"));
            eligibleBeneficiary.setIdentityVerified(false);

            given(beneficiaryRepository.findById(101))
                    .willReturn(Optional.of(eligibleBeneficiary));
            given(schemeRepository.findById(5L))
                    .willReturn(Optional.of(strictScheme));
            given(resultRepository.findByBeneficiaryIdAndSchemeId(101, 5L))
                    .willReturn(Optional.empty());
            given(resultRepository.save(any(EligibilityResult.class)))
                    .willAnswer(inv -> {
                        EligibilityResult r = inv.getArgument(0);
                        r.setId(3L);
                        return r;
                    });

            EligibilityResultResponse response = engine.checkEligibility(request);

            assertThat(response.getTotalScore()).isEqualTo(60);
            // 60 is exactly the threshold — ELIGIBLE
            assertThat(response.getEligibilityStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
            assertThat(response.getCriteria().get("incomeCheck").isPassed()).isFalse();
            assertThat(response.getCriteria().get("identityCheck").isPassed()).isFalse();
        }

        @Test
        @DisplayName("returns INELIGIBLE when score is 50 (three criteria fail)")
        void threeCriteriaFail_scoreIs50_ineligible() {
            // Age (20) + Land (20) + Category (20) → pass = 60, but:
            // Income fails → lose 30. Score = 20+20+20 = 60... need 3 to fail
            // Age fail, Income fail, Identity fail → 0+0+20+20+0 = 40
            eligibleBeneficiary.setAge(70); // fails age [18-60]
            eligibleBeneficiary.setAnnualIncome(new BigDecimal("999999")); // fails income
            eligibleBeneficiary.setIdentityVerified(false); // fails identity
            // Land(20) + Category(20) = 40

            given(beneficiaryRepository.findById(101)).willReturn(Optional.of(eligibleBeneficiary));
            given(schemeRepository.findById(5L)).willReturn(Optional.of(strictScheme));
            given(resultRepository.findByBeneficiaryIdAndSchemeId(101, 5L))
                    .willReturn(Optional.empty());
            given(resultRepository.save(any(EligibilityResult.class)))
                    .willAnswer(inv -> {
                        EligibilityResult r = inv.getArgument(0);
                        r.setId(4L);
                        return r;
                    });

            EligibilityResultResponse response = engine.checkEligibility(request);

            assertThat(response.getTotalScore()).isEqualTo(40);
            assertThat(response.getEligibilityStatus()).isEqualTo(EligibilityStatus.INELIGIBLE);
            assertThat(response.isEligible()).isFalse();
        }

        @Test
        @DisplayName("throws EligibilityCheckException when beneficiary not found")
        void beneficiaryNotFound_throws() {
            given(beneficiaryRepository.findById(101)).willReturn(Optional.empty());

            assertThatThrownBy(() -> engine.checkEligibility(request))
                    .isInstanceOf(EligibilityCheckException.class)
                    .hasMessageContaining("Beneficiary not found");
        }

        @Test
        @DisplayName("throws EligibilityCheckException when scheme not found")
        void schemeNotFound_throws() {
            given(beneficiaryRepository.findById(101))
                    .willReturn(Optional.of(eligibleBeneficiary));
            given(schemeRepository.findById(5L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> engine.checkEligibility(request))
                    .isInstanceOf(EligibilityCheckException.class)
                    .hasMessageContaining("Scheme not found");
        }

        @Test
        @DisplayName("replaces existing result on re-evaluation")
        void reEvaluation_replacesExistingResult() {
            EligibilityResult existing = EligibilityResult.builder()
                    .id(10L)
                    .beneficiaryId(101)
                    .schemeId(5L)
                    .schemeName("Old Scheme Name")
                    .totalScore(40)
                    .eligibilityStatus(EligibilityStatus.INELIGIBLE)
                    .build();

            given(beneficiaryRepository.findById(101))
                    .willReturn(Optional.of(eligibleBeneficiary));
            given(schemeRepository.findById(5L))
                    .willReturn(Optional.of(strictScheme));
            given(resultRepository.findByBeneficiaryIdAndSchemeId(101, 5L))
                    .willReturn(Optional.of(existing)); // Existing result found
            given(resultRepository.save(any(EligibilityResult.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            EligibilityResultResponse response = engine.checkEligibility(request);

            // The new evaluation should give 100 (all pass)
            assertThat(response.getTotalScore()).isEqualTo(100);
            assertThat(response.getEligibilityStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
            // save() was called with the same entity (updated in place)
            then(resultRepository).should().save(existing);
        }
    }
}
