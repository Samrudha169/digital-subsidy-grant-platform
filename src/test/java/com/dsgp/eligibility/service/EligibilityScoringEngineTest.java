package com.dsgp.eligibility.service;

import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.Category;
import com.dsgp.beneficiary.entity.RegistrationStatus;
import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.beneficiary.repository.BeneficiaryRepository;
import com.dsgp.beneficiary.repository.SchemeRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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

    private Beneficiary beneficiary;

    private Scheme pmKisan;

    private Scheme nsp;

    private Scheme pmegp;

    @BeforeEach
    void setUp() {

        beneficiary = Beneficiary.builder()
                .id(101)
                .fullName("Priya Sharma")
                .govId("PAN12345")
                .contact("9876543210")
                .email("priya@example.com")
                .password("test-password")
                .age(25)
                .address("Village Wai, Satara")
                .schemeName("PM-KISAN")
                .occupation("Farmer")
                .annualIncome(new BigDecimal("100000"))
                .landHolding(new BigDecimal("1.0"))
                .category(Category.OBC)
                .registrationStatus(RegistrationStatus.ACTIVE)
                .identityVerified(true)
                .build();

        // ------------------------------------------------------------
        // PM-KISAN
        // ------------------------------------------------------------

        pmKisan = new Scheme();

        pmKisan.setId(1L);
        pmKisan.setSchemeName("PM-KISAN");
        pmKisan.setMinAge(18);
        pmKisan.setMaxAge(70);
        pmKisan.setMaxAnnualIncome(new BigDecimal("300000"));
        pmKisan.setMaxLandHolding(new BigDecimal("5"));
        pmKisan.setRequiredCategory("ALL");
        pmKisan.setRequiredOccupation("Farmer");
        pmKisan.setGrantAmount(new BigDecimal("6000"));
        pmKisan.setActive(true);

        // ------------------------------------------------------------
        // NSP
        // ------------------------------------------------------------

        nsp = new Scheme();

        nsp.setId(2L);
        nsp.setSchemeName("NSP");
        nsp.setMinAge(15);
        nsp.setMaxAge(30);
        nsp.setMaxAnnualIncome(new BigDecimal("250000"));
        nsp.setRequiredCategory("ALL");
        nsp.setRequiredOccupation("Student");
        nsp.setGrantAmount(new BigDecimal("20000"));
        nsp.setActive(true);

        // ------------------------------------------------------------
        // PMEGP
        // ------------------------------------------------------------

        pmegp = new Scheme();

        pmegp.setId(3L);
        pmegp.setSchemeName("PMEGP");
        pmegp.setMinAge(18);
        pmegp.setMaxAge(55);
        pmegp.setMaxAnnualIncome(new BigDecimal("800000"));
        pmegp.setRequiredCategory("ALL");
        pmegp.setRequiredOccupation("ALL");
        pmegp.setGrantAmount(new BigDecimal("100000"));
        pmegp.setActive(true);
    }

    // ========================================================================
    // PM-KISAN TESTS
    // ========================================================================

    @Nested
    @DisplayName("PM-KISAN")
    class PmKisanTests {

        @Test
        @DisplayName("fully eligible PM-KISAN beneficiary gets 100 points")
        void fullyEligible_gets100Points() {

            EligibilityResultResponse response =
                    check(beneficiary, pmKisan);

            assertThat(response.getTotalScore())
                    .isEqualTo(100);

            assertThat(response.getEligibilityStatus())
                    .isEqualTo(EligibilityStatus.ELIGIBLE);

            assertThat(response.isEligible())
                    .isTrue();
        }

        @Test
        @DisplayName("PM-KISAN income scoring gives lower points for higher income")
        void incomeScoring() {

            beneficiary.setAnnualIncome(
                    new BigDecimal("100000"));

            EligibilityResultResponse lowIncome =
                    check(beneficiary, pmKisan);

            assertThat(
                    lowIncome.getCriteria()
                            .get("incomeCheck")
                            .getPoints()
            ).isEqualTo(25);

            beneficiary.setAnnualIncome(
                    new BigDecimal("150000"));

            EligibilityResultResponse mediumIncome =
                    check(beneficiary, pmKisan);

            assertThat(
                    mediumIncome.getCriteria()
                            .get("incomeCheck")
                            .getPoints()
            ).isEqualTo(20);

            beneficiary.setAnnualIncome(
                    new BigDecimal("250000"));

            EligibilityResultResponse higherIncome =
                    check(beneficiary, pmKisan);

            assertThat(
                    higherIncome.getCriteria()
                            .get("incomeCheck")
                            .getPoints()
            ).isEqualTo(10);
        }

        @Test
        @DisplayName("PM-KISAN income above 3 lakh fails mandatory condition")
        void incomeAboveLimit_isIneligible() {

            beneficiary.setAnnualIncome(
                    new BigDecimal("350000"));

            EligibilityResultResponse response =
                    check(beneficiary, pmKisan);

            assertThat(response.getCriteria()
                    .get("incomeCheck")
                    .isPassed())
                    .isFalse();

            assertThat(response.isEligible())
                    .isFalse();

            assertThat(response.getEligibilityStatus())
                    .isEqualTo(EligibilityStatus.INELIGIBLE);
        }

        @Test
        @DisplayName("PM-KISAN land scoring gives lower points for higher land holding")
        void landScoring() {

            beneficiary.setLandHolding(
                    new BigDecimal("1"));

            EligibilityResultResponse oneAcre =
                    check(beneficiary, pmKisan);

            assertThat(oneAcre.getCriteria()
                    .get("landCheck")
                    .getPoints())
                    .isEqualTo(25);

            beneficiary.setLandHolding(
                    new BigDecimal("3"));

            EligibilityResultResponse threeAcres =
                    check(beneficiary, pmKisan);

            assertThat(threeAcres.getCriteria()
                    .get("landCheck")
                    .getPoints())
                    .isEqualTo(15);

            beneficiary.setLandHolding(
                    new BigDecimal("5"));

            EligibilityResultResponse fiveAcres =
                    check(beneficiary, pmKisan);

            assertThat(fiveAcres.getCriteria()
                    .get("landCheck")
                    .getPoints())
                    .isEqualTo(5);
        }

        @Test
        @DisplayName("PM-KISAN land above 5 acres is ineligible")
        void landAboveLimit_isIneligible() {

            beneficiary.setLandHolding(
                    new BigDecimal("5.1"));

            EligibilityResultResponse response =
                    check(beneficiary, pmKisan);

            assertThat(response.getCriteria()
                    .get("landCheck")
                    .isPassed())
                    .isFalse();

            assertThat(response.isEligible())
                    .isFalse();
        }

        @Test
        @DisplayName("PM-KISAN requires Farmer occupation")
        void nonFarmer_isIneligible() {

            beneficiary.setOccupation("Student");

            EligibilityResultResponse response =
                    check(beneficiary, pmKisan);

            assertThat(response.getCriteria()
                    .get("occupationCheck")
                    .isPassed())
                    .isFalse();

            assertThat(response.isEligible())
                    .isFalse();
        }

        @Test
        @DisplayName("PM-KISAN age scoring changes according to age range")
        void ageScoring() {

            beneficiary.setAge(25);

            EligibilityResultResponse young =
                    check(beneficiary, pmKisan);

            assertThat(young.getCriteria()
                    .get("ageCheck")
                    .getPoints())
                    .isEqualTo(15);

            beneficiary.setAge(40);

            EligibilityResultResponse middle =
                    check(beneficiary, pmKisan);

            assertThat(middle.getCriteria()
                    .get("ageCheck")
                    .getPoints())
                    .isEqualTo(12);

            beneficiary.setAge(55);

            EligibilityResultResponse older =
                    check(beneficiary, pmKisan);

            assertThat(older.getCriteria()
                    .get("ageCheck")
                    .getPoints())
                    .isEqualTo(9);
        }
    }

    // ========================================================================
    // NSP TESTS
    // ========================================================================

    @Nested
    @DisplayName("NSP")
    class NspTests {

        @BeforeEach
        void configureNspBeneficiary() {

            beneficiary.setAge(18);
            beneficiary.setAnnualIncome(
                    new BigDecimal("100000"));
            beneficiary.setOccupation("Student");
            beneficiary.setLandHolding(null);
        }

        @Test
        @DisplayName("fully eligible NSP beneficiary gets 100 points")
        void fullyEligible_gets100Points() {

            EligibilityResultResponse response =
                    check(beneficiary, nsp);

            assertThat(response.getTotalScore())
                    .isEqualTo(100);

            assertThat(response.getEligibilityStatus())
                    .isEqualTo(EligibilityStatus.ELIGIBLE);

            assertThat(response.isEligible())
                    .isTrue();
        }

        @Test
        @DisplayName("NSP does not require land holding")
        void landIsNotRequired() {

            beneficiary.setLandHolding(null);

            EligibilityResultResponse response =
                    check(beneficiary, nsp);

            assertThat(response.getTotalScore())
                    .isEqualTo(100);

            assertThat(response.getCriteria())
                    .doesNotContainKey("landCheck");
        }

        @Test
        @DisplayName("NSP income scoring gives lower points for higher income")
        void incomeScoring() {

            beneficiary.setAnnualIncome(
                    new BigDecimal("100000"));

            EligibilityResultResponse lowIncome =
                    check(beneficiary, nsp);

            assertThat(lowIncome.getCriteria()
                    .get("incomeCheck")
                    .getPoints())
                    .isEqualTo(30);

            beneficiary.setAnnualIncome(
                    new BigDecimal("150000"));

            EligibilityResultResponse mediumIncome =
                    check(beneficiary, nsp);

            assertThat(mediumIncome.getCriteria()
                    .get("incomeCheck")
                    .getPoints())
                    .isEqualTo(25);

            beneficiary.setAnnualIncome(
                    new BigDecimal("220000"));

            EligibilityResultResponse higherIncome =
                    check(beneficiary, nsp);

            assertThat(higherIncome.getCriteria()
                    .get("incomeCheck")
                    .getPoints())
                    .isEqualTo(10);
        }

        @Test
        @DisplayName("NSP requires Student occupation")
        void nonStudent_isIneligible() {

            beneficiary.setOccupation("Farmer");

            EligibilityResultResponse response =
                    check(beneficiary, nsp);

            assertThat(response.getCriteria()
                    .get("occupationCheck")
                    .isPassed())
                    .isFalse();

            assertThat(response.isEligible())
                    .isFalse();
        }

        @Test
        @DisplayName("NSP age outside 15-30 is ineligible")
        void ageOutsideRange_isIneligible() {

            beneficiary.setAge(35);

            EligibilityResultResponse response =
                    check(beneficiary, nsp);

            assertThat(response.getCriteria()
                    .get("ageCheck")
                    .isPassed())
                    .isFalse();

            assertThat(response.isEligible())
                    .isFalse();
        }

        @Test
        @DisplayName("NSP income above 2.5 lakh is ineligible")
        void incomeAboveLimit_isIneligible() {

            beneficiary.setAnnualIncome(
                    new BigDecimal("300000"));

            EligibilityResultResponse response =
                    check(beneficiary, nsp);

            assertThat(response.getCriteria()
                    .get("incomeCheck")
                    .isPassed())
                    .isFalse();

            assertThat(response.isEligible())
                    .isFalse();
        }
    }

    // ========================================================================
    // PMEGP TESTS
    // ========================================================================

    @Nested
    @DisplayName("PMEGP")
    class PmegpTests {

        @BeforeEach
        void configurePmegpBeneficiary() {

            beneficiary.setAge(25);
            beneficiary.setAnnualIncome(
                    new BigDecimal("200000"));
            beneficiary.setOccupation("Business");
            beneficiary.setLandHolding(null);
        }

        @Test
        @DisplayName("fully eligible PMEGP beneficiary gets 100 points")
        void fullyEligible_gets100Points() {

            EligibilityResultResponse response =
                    check(beneficiary, pmegp);

            assertThat(response.getTotalScore())
                    .isEqualTo(100);

            assertThat(response.getEligibilityStatus())
                    .isEqualTo(EligibilityStatus.ELIGIBLE);

            assertThat(response.isEligible())
                    .isTrue();
        }

        @Test
        @DisplayName("PMEGP does not require land holding")
        void landIsNotRequired() {

            beneficiary.setLandHolding(null);

            EligibilityResultResponse response =
                    check(beneficiary, pmegp);

            assertThat(response.getCriteria())
                    .doesNotContainKey("landCheck");
        }

        @Test
        @DisplayName("PMEGP accepts different occupations")
        void differentOccupations_areAccepted() {

            beneficiary.setOccupation("Business");

            EligibilityResultResponse business =
                    check(beneficiary, pmegp);

            assertThat(business.getCriteria()
                    .get("occupationCheck")
                    .isPassed())
                    .isTrue();

            beneficiary.setOccupation("Farmer");

            EligibilityResultResponse farmer =
                    check(beneficiary, pmegp);

            assertThat(farmer.getCriteria()
                    .get("occupationCheck")
                    .isPassed())
                    .isTrue();

            beneficiary.setOccupation("Teacher");

            EligibilityResultResponse teacher =
                    check(beneficiary, pmegp);

            assertThat(teacher.getCriteria()
                    .get("occupationCheck")
                    .isPassed())
                    .isTrue();
        }

        @Test
        @DisplayName("PMEGP income scoring gives lower points for higher income")
        void incomeScoring() {

            beneficiary.setAnnualIncome(
                    new BigDecimal("200000"));

            EligibilityResultResponse lowIncome =
                    check(beneficiary, pmegp);

            assertThat(lowIncome.getCriteria()
                    .get("incomeCheck")
                    .getPoints())
                    .isEqualTo(30);

            beneficiary.setAnnualIncome(
                    new BigDecimal("400000"));

            EligibilityResultResponse mediumIncome =
                    check(beneficiary, pmegp);

            assertThat(mediumIncome.getCriteria()
                    .get("incomeCheck")
                    .getPoints())
                    .isEqualTo(25);

            beneficiary.setAnnualIncome(
                    new BigDecimal("700000"));

            EligibilityResultResponse higherIncome =
                    check(beneficiary, pmegp);

            assertThat(higherIncome.getCriteria()
                    .get("incomeCheck")
                    .getPoints())
                    .isEqualTo(10);
        }

        @Test
        @DisplayName("PMEGP income above 8 lakh is ineligible")
        void incomeAboveLimit_isIneligible() {

            beneficiary.setAnnualIncome(
                    new BigDecimal("900000"));

            EligibilityResultResponse response =
                    check(beneficiary, pmegp);

            assertThat(response.getCriteria()
                    .get("incomeCheck")
                    .isPassed())
                    .isFalse();

            assertThat(response.isEligible())
                    .isFalse();
        }

        @Test
        @DisplayName("PMEGP age outside 18-55 is ineligible")
        void ageOutsideRange_isIneligible() {

            beneficiary.setAge(60);

            EligibilityResultResponse response =
                    check(beneficiary, pmegp);

            assertThat(response.getCriteria()
                    .get("ageCheck")
                    .isPassed())
                    .isFalse();

            assertThat(response.isEligible())
                    .isFalse();
        }
    }

    // ========================================================================
    // COMMON TESTS
    // ========================================================================

    @Nested
    @DisplayName("Common eligibility behaviour")
    class CommonTests {

        @Test
        @DisplayName("unverified identity gives zero identity points")
        void unverifiedIdentity_givesZeroPoints() {

            beneficiary.setIdentityVerified(false);

            EligibilityResultResponse response =
                    check(beneficiary, pmKisan);

            assertThat(response.getCriteria()
                    .get("identityCheck")
                    .getPoints())
                    .isEqualTo(0);

            assertThat(response.getCriteria()
                    .get("identityCheck")
                    .isPassed())
                    .isFalse();
        }

        @Test
        @DisplayName("beneficiary not found throws exception")
        void beneficiaryNotFound_throwsException() {

            EligibilityCheckRequest request =
                    createRequest(101, 1L);

            given(beneficiaryRepository.findById(101))
                    .willReturn(Optional.empty());

            assertThatThrownBy(
                    () -> engine.checkEligibility(request)
            )
                    .isInstanceOf(
                            EligibilityCheckException.class
                    )
                    .hasMessageContaining(
                            "Beneficiary not found"
                    );
        }

        @Test
        @DisplayName("scheme not found throws exception")
        void schemeNotFound_throwsException() {

            EligibilityCheckRequest request =
                    createRequest(101, 1L);

            given(beneficiaryRepository.findById(101))
                    .willReturn(Optional.of(beneficiary));

            given(schemeRepository.findById(1L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(
                    () -> engine.checkEligibility(request)
            )
                    .isInstanceOf(
                            EligibilityCheckException.class
                    )
                    .hasMessageContaining(
                            "Scheme not found"
                    );
        }

        @Test
        @DisplayName("existing eligibility result is replaced during re-evaluation")
        void reEvaluation_replacesExistingResult() {

            EligibilityResult existing =
                    EligibilityResult.builder()
                            .id(10L)
                            .beneficiaryId(101)
                            .schemeId(1L)
                            .schemeName("PM-KISAN")
                            .totalScore(40)
                            .eligibilityStatus(
                                    EligibilityStatus.INELIGIBLE
                            )
                            .build();

            EligibilityCheckRequest request =
                    createRequest(101, 1L);

            given(beneficiaryRepository.findById(101))
                    .willReturn(Optional.of(beneficiary));

            given(schemeRepository.findById(1L))
                    .willReturn(Optional.of(pmKisan));

            // IMPORTANT:
            // Return the existing result so the engine updates
            // this same entity instead of creating a new one.
            given(resultRepository
                    .findByBeneficiaryIdAndSchemeId(101, 1L))
                    .willReturn(Optional.of(existing));

            given(resultRepository.save(any(EligibilityResult.class)))
                    .willAnswer(invocation ->
                            invocation.getArgument(0));

            EligibilityResultResponse response =
                    engine.checkEligibility(request);

            // The beneficiary is fully eligible for PM-KISAN.
            assertThat(response.getTotalScore())
                    .isEqualTo(100);

            assertThat(response.getEligibilityStatus())
                    .isEqualTo(EligibilityStatus.ELIGIBLE);

            assertThat(response.isEligible())
                    .isTrue();

            // The SAME existing entity must be updated and saved.
            assertThat(existing.getTotalScore())
                    .isEqualTo(100);

            assertThat(existing.getEligibilityStatus())
                    .isEqualTo(EligibilityStatus.ELIGIBLE);

            then(resultRepository)
                    .should()
                    .save(existing);
        }
    }

    // ========================================================================
    // TEST HELPERS
    // ========================================================================

    private EligibilityResultResponse check(
            Beneficiary beneficiary,
            Scheme scheme) {

        EligibilityCheckRequest request =
                createRequest(
                        beneficiary.getId(),
                        scheme.getId()
                );

        given(beneficiaryRepository
                .findById(beneficiary.getId()))
                .willReturn(Optional.of(beneficiary));

        given(schemeRepository
                .findById(scheme.getId()))
                .willReturn(Optional.of(scheme));

        given(resultRepository
                .findByBeneficiaryIdAndSchemeId(
                        beneficiary.getId(),
                        scheme.getId()
                ))
                .willReturn(Optional.empty());

        given(resultRepository.save(
                any(EligibilityResult.class)))
                .willAnswer(invocation -> {

                    EligibilityResult result =
                            invocation.getArgument(0);

                    if (result.getId() == null) {
                        result.setId(1L);
                    }

                    return result;
                });

        return engine.checkEligibility(request);
    }

    private EligibilityCheckRequest createRequest(
            Integer beneficiaryId,
            Long schemeId) {

        EligibilityCheckRequest request =
                new EligibilityCheckRequest();

        request.setBeneficiaryId(beneficiaryId);
        request.setSchemeId(schemeId);

        return request;
    }
}