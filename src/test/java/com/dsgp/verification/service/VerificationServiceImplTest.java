package com.dsgp.verification.service;

import com.dsgp.application.entity.SchemeApplication;
import com.dsgp.authentication.entity.Officer;
import com.dsgp.authentication.entity.OfficerRole;
import com.dsgp.authentication.repository.OfficerRepository;
import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.beneficiary.repository.SchemeApplicationRepository;
import com.dsgp.eligibility.entity.EligibilityResult;
import com.dsgp.eligibility.entity.EligibilityStatus;
import com.dsgp.eligibility.repository.EligibilityResultRepository;
import com.dsgp.verification.dto.VerificationActionRequest;
import com.dsgp.verification.dto.VerificationCriterionResponse;
import com.dsgp.verification.dto.VerificationStatusResponse;
import com.dsgp.verification.entity.VerificationCriterion;
import com.dsgp.verification.entity.VerificationCriterionStatus;
import com.dsgp.verification.entity.VerificationStage;
import com.dsgp.verification.exception.InvalidVerificationTransitionException;
import com.dsgp.verification.dto.VerificationCriterionUpdateRequest;
import com.dsgp.verification.repository.VerificationCriterionRepository;
import com.dsgp.verification.repository.VerificationRecordRepository;
import com.dsgp.beneficiary.repository.BeneficiaryDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import com.dsgp.disbursement.service.DisbursementPlanService;

@ExtendWith(MockitoExtension.class)
@DisplayName("VerificationServiceImpl")
class VerificationServiceImplTest {

    // ========================================================================
    // MOCKS
    // ========================================================================


    @Mock
    private DisbursementPlanService disbursementPlanService;

    @Mock
    private SchemeApplicationRepository applicationRepository;

    @Mock
    private OfficerRepository officerRepository;

    @Mock
    private EligibilityResultRepository eligibilityResultRepository;

    @Mock
    private VerificationRecordRepository verificationRecordRepository;

    @Mock
    private VerificationCriterionRepository verificationCriterionRepository;

    @Mock
    private BeneficiaryDocumentRepository documentRepository;

    @InjectMocks
    private VerificationServiceImpl service;

    // ========================================================================
    // TEST CONSTANTS
    // ========================================================================

    private static final Long APP_ID = 1L;
    private static final Integer BENEFICIARY_ID = 101;
    private static final Long SCHEME_ID = 1L;

    // ========================================================================
    // SETUP
    // ========================================================================

    // No @BeforeEach field injection needed: routing threshold is a compile-time
    // constant (HIGH_SCORE_THRESHOLD = 80) in VerificationServiceImpl.

    // ========================================================================
    // FIXTURES
    // ========================================================================

    private Beneficiary beneficiary() {

        Beneficiary beneficiary = new Beneficiary();

        beneficiary.setId(BENEFICIARY_ID);
        beneficiary.setFullName("Ravi Kumar");

        return beneficiary;
    }

    private Scheme scheme() {

        Scheme scheme = new Scheme();

        scheme.setId(SCHEME_ID);
        scheme.setSchemeName("PM-KISAN");

        return scheme;
    }

    private Scheme scheme(java.math.BigDecimal grantAmount) {

        Scheme scheme = scheme();
        scheme.setGrantAmount(grantAmount);
        return scheme;
    }

    private SchemeApplication application(String status) {

        SchemeApplication application =
                SchemeApplication.builder()
                        .beneficiary(beneficiary())
                        .scheme(scheme())
                        .applicationStatus(status)
                        .build();

        application.setId(APP_ID);

        return application;
    }

    private SchemeApplication application(
            String status,
            java.math.BigDecimal grantAmount) {

        SchemeApplication application =
                SchemeApplication.builder()
                        .beneficiary(beneficiary())
                        .scheme(scheme(grantAmount))
                        .applicationStatus(status)
                        .build();

        application.setId(APP_ID);

        return application;
    }

    private EligibilityResult eligibleResult() {

        return EligibilityResult.builder()
                .beneficiaryId(BENEFICIARY_ID)
                .schemeId(SCHEME_ID)
                .schemeName("PM-KISAN")
                .totalScore(100)
                .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                .evaluatedAt(LocalDateTime.now())
                .build();
    }

    private EligibilityResult ineligibleResult() {

        return EligibilityResult.builder()
                .beneficiaryId(BENEFICIARY_ID)
                .schemeId(SCHEME_ID)
                .schemeName("PM-KISAN")
                .totalScore(40)
                .eligibilityStatus(EligibilityStatus.INELIGIBLE)
                .evaluatedAt(LocalDateTime.now())
                .build();
    }

    private Officer officer(
            String username,
            OfficerRole role) {

        return Officer.builder()
                .id(1L)
                .username(username)
                .fullName("Test Officer")
                .role(role)
                .active(true)
                .build();
    }

    private VerificationActionRequest request(
            String performedBy,
            String remarks) {

        VerificationActionRequest request =
                new VerificationActionRequest();

        request.setPerformedBy(performedBy);
        request.setRemarks(remarks);

        return request;
    }

    /** Builds a request pre-loaded with a sanctionedAmount — for Finance approve tests. */
    private VerificationActionRequest financeRequest(
            String performedBy,
            java.math.BigDecimal sanctionedAmount) {

        VerificationActionRequest request =
                new VerificationActionRequest();

        request.setPerformedBy(performedBy);
        request.setSanctionedAmount(sanctionedAmount);

        return request;
    }

    private VerificationCriterion criterion(
            SchemeApplication application,
            VerificationStage stage,
            int id,
            VerificationCriterionStatus status) {

        return VerificationCriterion.builder()
                .id((long) id)
                .schemeApplication(application)
                .stage(stage)
                .criterionCode(
                        stage.name()
                                + "_CRITERION_"
                                + id
                )
                .criterionName(
                        stage.name()
                                + " Criterion "
                                + id
                )
                .status(status)
                .verifiedBy(
                        status == VerificationCriterionStatus.VERIFIED
                                ? "test.officer"
                                : null
                )
                .remarks(null)
                .verifiedAt(
                        status == VerificationCriterionStatus.VERIFIED
                                ? LocalDateTime.now()
                                : null
                )
                .build();
    }

    private void mockCriteria(
            SchemeApplication application,
            VerificationStage stage,
            int total,
            int verified) {

        List<VerificationCriterion> criteria =
                new ArrayList<>();

        for (int i = 1; i <= total; i++) {

            VerificationCriterionStatus status =
                    i <= verified
                            ? VerificationCriterionStatus.VERIFIED
                            : VerificationCriterionStatus.PENDING;

            criteria.add(
                    criterion(
                            application,
                            stage,
                            i,
                            status
                    )
            );
        }

        lenient()
                .when(
                        verificationCriterionRepository
                                .findBySchemeApplicationIdAndStageOrderByIdAsc(
                                        APP_ID,
                                        stage
                                )
                )
                .thenReturn(criteria);

        lenient()
                .when(
                        verificationCriterionRepository
                                .countBySchemeApplicationIdAndStage(
                                        APP_ID,
                                        stage
                                )
                )
                .thenReturn((long) total);

        lenient()
                .when(
                        verificationCriterionRepository
                                .countBySchemeApplicationIdAndStageAndStatus(
                                        APP_ID,
                                        stage,
                                        VerificationCriterionStatus.VERIFIED
                                )
                )
                .thenReturn((long) verified);
    }

    private void mockOfficer(
            String username,
            OfficerRole role) {

        lenient()
                .when(
                        officerRepository.findByUsername(username)
                )
                .thenReturn(
                        Optional.of(
                                officer(
                                        username,
                                        role
                                )
                        )
                );
    }

    private void mockHistory() {

        lenient()
                .when(
                        verificationRecordRepository
                                .findBySchemeApplicationIdOrderByPerformedAtAsc(
                                        APP_ID
                                )
                )
                .thenReturn(List.of());
    }

    private void mockSave() {

        lenient()
                .when(
                        applicationRepository.save(any())
                )
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0)
                );

        lenient()
                .when(
                        verificationRecordRepository.save(any())
                )
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0)
                );

        lenient()
                .when(
                        verificationCriterionRepository.save(any())
                )
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0)
                );

        lenient()
                .when(
                        verificationCriterionRepository.saveAll(any())
                )
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0)
                );
    }

    // ========================================================================
    // START VERIFICATION
    // ========================================================================

    @Nested
    @DisplayName("Start Verification")
    class StartVerification {

        @Test
        @DisplayName("score <= 80: Field Officer starts verification -> UNDER_REVIEW")
        void lowScorePendingApplicationMovesToUnderReview() {

            // score=40, which is <= 80 threshold -> standard Field path
            SchemeApplication application =
                    application("PENDING");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            EligibilityResult lowScore = EligibilityResult.builder()
                    .beneficiaryId(BENEFICIARY_ID)
                    .schemeId(SCHEME_ID)
                    .schemeName("PM-KISAN")
                    .totalScore(40)
                    .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                    .evaluatedAt(LocalDateTime.now())
                    .build();

            given(
                    eligibilityResultRepository
                            .findByBeneficiaryIdAndSchemeId(
                                    BENEFICIARY_ID,
                                    SCHEME_ID
                            )
            ).willReturn(Optional.of(lowScore));

            mockOfficer(
                    "field.officer",
                    OfficerRole.FIELD_OFFICER
            );

            mockSave();
            mockHistory();

            VerificationStatusResponse response =
                    service.startVerification(
                            APP_ID,
                            request(
                                    "field.officer",
                                    null
                            )
                    );

            assertThat(
                    application.getApplicationStatus()
            ).isEqualTo(
                    "UNDER_REVIEW"
            );

            assertThat(
                    response.getApplicationStatus()
            ).isEqualTo(
                    "UNDER_REVIEW"
            );

            verify(
                    applicationRepository
            ).save(application);
        }

        @Test
        @DisplayName("score > 80: startVerification routes directly to DISTRICT_REVIEW (no Field Officer)")
        void highScorePendingApplicationMovesToDistrictReview() {

            // score=100, which is > 80 threshold -> skip Field Officer
            SchemeApplication application =
                    application("PENDING");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            given(
                    eligibilityResultRepository
                            .findByBeneficiaryIdAndSchemeId(
                                    BENEFICIARY_ID,
                                    SCHEME_ID
                            )
            ).willReturn(
                    Optional.of(eligibleResult())   // score = 100
            );

            // ANY officer role is acceptable for high-score path
            mockOfficer(
                    "district.officer",
                    OfficerRole.DISTRICT_OFFICER
            );

            mockSave();
            mockHistory();

            VerificationStatusResponse response =
                    service.startVerification(
                            APP_ID,
                            request(
                                    "district.officer",
                                    null
                            )
                    );

            assertThat(
                    application.getApplicationStatus()
            ).isEqualTo(
                    "DISTRICT_REVIEW"
            );

            assertThat(
                    response.getApplicationStatus()
            ).isEqualTo(
                    "DISTRICT_REVIEW"
            );

            verify(
                    applicationRepository
            ).save(application);
        }

        @Test
        @DisplayName("score > 80: non-FieldOfficer caller is accepted (role not required)")
        void highScoreStartVerificationAcceptsAnyOfficerRole() {

            SchemeApplication application =
                    application("PENDING");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            given(
                    eligibilityResultRepository
                            .findByBeneficiaryIdAndSchemeId(
                                    BENEFICIARY_ID,
                                    SCHEME_ID
                            )
            ).willReturn(
                    Optional.of(eligibleResult())  // score = 100
            );

            // Finance officer submitting — must NOT be rejected for wrong role
            mockOfficer(
                    "finance.officer",
                    OfficerRole.FINANCE_APPROVER
            );

            mockSave();
            mockHistory();

            assertThatCode(
                    () -> service.startVerification(
                            APP_ID,
                            request("finance.officer", null)
                    )
            ).doesNotThrowAnyException();

            assertThat(application.getApplicationStatus())
                    .isEqualTo("DISTRICT_REVIEW");
        }

        @Test
        @DisplayName("score <= 80: non-FieldOfficer caller is rejected")
        void lowScoreStartVerificationRejectsNonFieldOfficer() {

            SchemeApplication application =
                    application("PENDING");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            EligibilityResult lowScore = EligibilityResult.builder()
                    .beneficiaryId(BENEFICIARY_ID)
                    .schemeId(SCHEME_ID)
                    .schemeName("PM-KISAN")
                    .totalScore(40)
                    .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                    .evaluatedAt(LocalDateTime.now())
                    .build();

            given(
                    eligibilityResultRepository
                            .findByBeneficiaryIdAndSchemeId(
                                    BENEFICIARY_ID,
                                    SCHEME_ID
                            )
            ).willReturn(Optional.of(lowScore));

            // District Officer trying to start a low-score application -> rejected
            mockOfficer(
                    "district.officer",
                    OfficerRole.DISTRICT_OFFICER
            );

            assertThatThrownBy(
                    () -> service.startVerification(
                            APP_ID,
                            request("district.officer", null)
                    )
            ).isInstanceOf(InvalidVerificationTransitionException.class);
        }

        @Test
        void ineligibleApplicationCannotStart() {

            SchemeApplication application =
                    application("PENDING");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            given(
                    eligibilityResultRepository
                            .findByBeneficiaryIdAndSchemeId(
                                    BENEFICIARY_ID,
                                    SCHEME_ID
                            )
            ).willReturn(
                    Optional.of(
                            ineligibleResult()
                    )
            );

            mockOfficer(
                    "field.officer",
                    OfficerRole.FIELD_OFFICER
            );

            assertThatThrownBy(
                    () ->
                            service.startVerification(
                                    APP_ID,
                                    request(
                                            "field.officer",
                                            null
                                    )
                            )
            )
                    .isInstanceOf(
                            InvalidVerificationTransitionException.class
                    )
                    .hasMessageContaining(
                            "INELIGIBLE"
                    );
        }

        @Test
        void nonPendingApplicationCannotStart() {

            SchemeApplication application =
                    application("UNDER_REVIEW");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            mockOfficer(
                    "field.officer",
                    OfficerRole.FIELD_OFFICER
            );

            assertThatThrownBy(
                    () ->
                            service.startVerification(
                                    APP_ID,
                                    request(
                                            "field.officer",
                                            null
                                    )
                            )
            )
                    .isInstanceOf(
                            InvalidVerificationTransitionException.class
                    );
        }

        @Test
        void applicationNotFoundThrows() {

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.empty()
            );

            assertThatThrownBy(
                    () ->
                            service.startVerification(
                                    APP_ID,
                                    request(
                                            "field.officer",
                                            null
                                    )
                            )
            )
                    .isInstanceOf(
                            com.dsgp.application.exception.ApplicationException.class
                    );
        }
    }

    // ========================================================================
    // FIELD OFFICER
    // ========================================================================

    @Nested
    @DisplayName("Field Officer")
    class FieldOfficer {

        @Test
        @DisplayName("Field approval with all criteria verified -> ESCALATED (District queue)")
        void fieldApprovalWithAllCriteriaMovesToEscalated() {

            // score=40 (<= 80) -> standard path; after Field approve -> ESCALATED
            SchemeApplication application =
                    application("UNDER_REVIEW");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            EligibilityResult lowScore = EligibilityResult.builder()
                    .beneficiaryId(BENEFICIARY_ID)
                    .schemeId(SCHEME_ID)
                    .schemeName("PM-KISAN")
                    .totalScore(40)
                    .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                    .evaluatedAt(LocalDateTime.now())
                    .build();

            given(
                    eligibilityResultRepository
                            .findByBeneficiaryIdAndSchemeId(
                                    BENEFICIARY_ID,
                                    SCHEME_ID
                            )
            ).willReturn(Optional.of(lowScore));

            mockOfficer(
                    "field.officer",
                    OfficerRole.FIELD_OFFICER
            );

            mockCriteria(
                    application,
                    VerificationStage.FIELD,
                    7,
                    7
            );

            mockSave();
            mockHistory();

            VerificationStatusResponse response =
                    service.approveAtField(
                            APP_ID,
                            request(
                                    "field.officer",
                                    "All field verification criteria verified."
                            )
                    );

            assertThat(
                    application.getApplicationStatus()
            ).isEqualTo(
                    "ESCALATED"
            );

            assertThat(
                    response.getApplicationStatus()
            ).isEqualTo(
                    "ESCALATED"
            );

            verify(
                    applicationRepository
            ).save(application);
        }

        @Test
        void fieldApprovalWithoutAllCriteriaThrows() {

            SchemeApplication application =
                    application("UNDER_REVIEW");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            mockOfficer(
                    "field.officer",
                    OfficerRole.FIELD_OFFICER
            );

            mockCriteria(
                    application,
                    VerificationStage.FIELD,
                    7,
                    6
            );

            assertThatThrownBy(
                    () ->
                            service.approveAtField(
                                    APP_ID,
                                    request(
                                            "field.officer",
                                            null
                                    )
                            )
            )
                    .isInstanceOf(
                            InvalidVerificationTransitionException.class
                    )
                    .hasMessageContaining(
                            "cannot approve until all verification criteria are VERIFIED"
                    );
        }

        @Test
        void fieldRejectMovesToRejected() {

            SchemeApplication application =
                    application("UNDER_REVIEW");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            mockOfficer(
                    "field.officer",
                    OfficerRole.FIELD_OFFICER
            );

            mockSave();
            mockHistory();

            VerificationStatusResponse response =
                    service.rejectAtField(
                            APP_ID,
                            request(
                                    "field.officer",
                                    "Documents are invalid."
                            )
                    );

            assertThat(
                    application.getApplicationStatus()
            ).isEqualTo(
                    "REJECTED"
            );

            assertThat(
                    response.getApplicationStatus()
            ).isEqualTo(
                    "REJECTED"
            );
        }

        @Test
        void fieldRejectWithoutRemarksThrows() {

            SchemeApplication application =
                    application("UNDER_REVIEW");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            mockOfficer(
                    "field.officer",
                    OfficerRole.FIELD_OFFICER
            );

            assertThatThrownBy(
                    () ->
                            service.rejectAtField(
                                    APP_ID,
                                    request(
                                            "field.officer",
                                            null
                                    )
                            )
            )
                    .isInstanceOf(
                            InvalidVerificationTransitionException.class
                    )
                    .hasMessageContaining(
                            "remarks"
                    );
        }

        @Test
        @DisplayName("completeFieldVerification with all criteria verified -> ESCALATED")
        void completeFieldWithAllCriteriaMovesToEscalated() {

            // score=40 (<= 80) -> after field completion -> ESCALATED
            SchemeApplication application =
                    application("UNDER_REVIEW");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            EligibilityResult lowScore = EligibilityResult.builder()
                    .beneficiaryId(BENEFICIARY_ID)
                    .schemeId(SCHEME_ID)
                    .schemeName("PM-KISAN")
                    .totalScore(40)
                    .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                    .evaluatedAt(LocalDateTime.now())
                    .build();

            given(
                    eligibilityResultRepository
                            .findByBeneficiaryIdAndSchemeId(
                                    BENEFICIARY_ID,
                                    SCHEME_ID
                            )
            ).willReturn(Optional.of(lowScore));

            mockOfficer(
                    "field.officer",
                    OfficerRole.FIELD_OFFICER
            );

            mockCriteria(
                    application,
                    VerificationStage.FIELD,
                    7,
                    7
            );

            mockSave();
            mockHistory();

            service.completeFieldVerification(
                    APP_ID,
                    "field.officer",
                    "Field verification completed."
            );

            assertThat(
                    application.getApplicationStatus()
            ).isEqualTo(
                    "ESCALATED"
            );

            verify(
                    applicationRepository
            ).save(application);
        }

        @Test
        void completeFieldWithoutAllCriteriaThrows() {

            SchemeApplication application =
                    application("UNDER_REVIEW");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            mockOfficer(
                    "field.officer",
                    OfficerRole.FIELD_OFFICER
            );

            mockCriteria(
                    application,
                    VerificationStage.FIELD,
                    7,
                    5
            );

            assertThatThrownBy(
                    () ->
                            service.completeFieldVerification(
                                    APP_ID,
                                    "field.officer",
                                    null
                            )
            )
                    .isInstanceOf(
                            InvalidVerificationTransitionException.class
                    )
                    .hasMessageContaining(
                            "cannot approve until all verification criteria are VERIFIED"
                    );
        }
    }

    // ========================================================================
    // FIELD OFFICER — ROUTING (score-based)
    // ========================================================================

    @Nested
    @DisplayName("Field Officer Routing")
    class FieldOfficerRouting {

        // Routing threshold: HIGH_SCORE_THRESHOLD = 80 (compile-time constant)
        // score > 80 -> DISTRICT_REVIEW (Field skipped)
        // score <= 80 -> after Field approve -> ESCALATED

        @Test
        @DisplayName("score <= 80: Field approve routes to ESCALATED (District queue)")
        void lowScoreFieldApproveRoutesToEscalated() {

            SchemeApplication application =
                    application("UNDER_REVIEW");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            EligibilityResult lowScore = EligibilityResult.builder()
                    .beneficiaryId(BENEFICIARY_ID)
                    .schemeId(SCHEME_ID)
                    .schemeName("PM-KISAN")
                    .totalScore(40)   // <= 80
                    .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                    .evaluatedAt(LocalDateTime.now())
                    .build();

            given(
                    eligibilityResultRepository
                            .findByBeneficiaryIdAndSchemeId(
                                    BENEFICIARY_ID,
                                    SCHEME_ID
                            )
            ).willReturn(Optional.of(lowScore));

            mockOfficer(
                    "field.officer",
                    OfficerRole.FIELD_OFFICER
            );

            mockCriteria(
                    application,
                    VerificationStage.FIELD,
                    4,
                    4
            );

            mockSave();
            mockHistory();

            service.approveAtField(
                    APP_ID,
                    request("field.officer", null)
            );

            assertThat(
                    application.getApplicationStatus()
            ).isEqualTo(
                    "ESCALATED"
            );
        }

        @Test
        @DisplayName("boundary score = 80: Field approve routes to ESCALATED")
        void boundaryScoreFieldApproveRoutesToEscalated() {

            SchemeApplication application =
                    application("UNDER_REVIEW");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            EligibilityResult boundaryScore = EligibilityResult.builder()
                    .beneficiaryId(BENEFICIARY_ID)
                    .schemeId(SCHEME_ID)
                    .schemeName("PM-KISAN")
                    .totalScore(80)  // exactly 80 -> NOT > 80 -> ESCALATED path
                    .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                    .evaluatedAt(LocalDateTime.now())
                    .build();

            given(
                    eligibilityResultRepository
                            .findByBeneficiaryIdAndSchemeId(
                                    BENEFICIARY_ID,
                                    SCHEME_ID
                            )
            ).willReturn(Optional.of(boundaryScore));

            mockOfficer(
                    "field.officer",
                    OfficerRole.FIELD_OFFICER
            );

            mockCriteria(
                    application,
                    VerificationStage.FIELD,
                    4,
                    4
            );

            mockSave();
            mockHistory();

            service.approveAtField(
                    APP_ID,
                    request("field.officer", null)
            );

            assertThat(
                    application.getApplicationStatus()
            ).isEqualTo(
                    "ESCALATED"
            );
        }
    }

    // ========================================================================
    // DISTRICT OFFICER
    // ========================================================================

    @Nested
    @DisplayName("District Officer")
    class DistrictOfficer {

        @Test
        void districtApproveWithAllCriteriaMovesToDistrictApproved() {

            SchemeApplication application =
                    application("ESCALATED");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            mockOfficer(
                    "district.officer",
                    OfficerRole.DISTRICT_OFFICER
            );

            mockCriteria(
                    application,
                    VerificationStage.DISTRICT,
                    4,
                    4
            );

            mockSave();
            mockHistory();

            VerificationStatusResponse response =
                    service.approveAtDistrict(
                            APP_ID,
                            request(
                                    "district.officer",
                                    null
                            )
                    );

            assertThat(
                    application.getApplicationStatus()
            ).isEqualTo(
                    "DISTRICT_APPROVED"
            );

            assertThat(
                    response.getApplicationStatus()
            ).isEqualTo(
                    "DISTRICT_APPROVED"
            );
        }

        @Test
        void districtApproveWithoutAllCriteriaThrows() {

            SchemeApplication application =
                    application("ESCALATED");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            mockOfficer(
                    "district.officer",
                    OfficerRole.DISTRICT_OFFICER
            );

            mockCriteria(
                    application,
                    VerificationStage.DISTRICT,
                    4,
                    3
            );

            assertThatThrownBy(
                    () ->
                            service.approveAtDistrict(
                                    APP_ID,
                                    request(
                                            "district.officer",
                                            null
                                    )
                            )
            )
                    .isInstanceOf(
                            InvalidVerificationTransitionException.class
                    );
        }

        @Test
        void districtRejectMovesToRejected() {

            // UPDATED:
            // District Officer rejects only when the application is ESCALATED.
            SchemeApplication application =
                    application("ESCALATED");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            mockOfficer(
                    "district.officer",
                    OfficerRole.DISTRICT_OFFICER
            );

            mockSave();
            mockHistory();

            VerificationStatusResponse response =
                    service.rejectAtDistrict(
                            APP_ID,
                            request(
                                    "district.officer",
                                    "District validation failed."
                            )
                    );

            assertThat(
                    application.getApplicationStatus()
            ).isEqualTo(
                    "REJECTED"
            );

            assertThat(
                    response.getApplicationStatus()
            ).isEqualTo(
                    "REJECTED"
            );
        }

        @Test
        void districtRejectWithoutRemarksThrows() {

            // UPDATED:
            // The request must reach the remarks validation first.
            SchemeApplication application =
                    application("ESCALATED");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            mockOfficer(
                    "district.officer",
                    OfficerRole.DISTRICT_OFFICER
            );

            assertThatThrownBy(
                    () ->
                            service.rejectAtDistrict(
                                    APP_ID,
                                    request(
                                            "district.officer",
                                            null
                                    )
                            )
            )
                    .isInstanceOf(
                            InvalidVerificationTransitionException.class
                    )
                    .hasMessageContaining(
                            "remarks"
                    );
        }

        @Test
        void districtCannotApproveWrongStatus() {

            SchemeApplication application =
                    application("UNDER_REVIEW");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            mockOfficer(
                    "district.officer",
                    OfficerRole.DISTRICT_OFFICER
            );

            assertThatThrownBy(
                    () ->
                            service.approveAtDistrict(
                                    APP_ID,
                                    request(
                                            "district.officer",
                                            null
                                    )
                            )
            )
                    .isInstanceOf(
                            InvalidVerificationTransitionException.class
                    );
        }

        @Test
        @DisplayName("DISTRICT_REVIEW (high-score path) -> District approve -> DISTRICT_APPROVED")
        void districtApproveFromDistrictReviewMovesToDistrictApproved() {

            // High-score applications skip Field Officer and arrive at District
            // with status DISTRICT_REVIEW.
            SchemeApplication application =
                    application("DISTRICT_REVIEW");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            mockOfficer(
                    "district.officer",
                    OfficerRole.DISTRICT_OFFICER
            );

            mockCriteria(
                    application,
                    VerificationStage.DISTRICT,
                    4,
                    4
            );

            mockSave();
            mockHistory();

            VerificationStatusResponse response =
                    service.approveAtDistrict(
                            APP_ID,
                            request("district.officer", null)
                    );

            assertThat(
                    application.getApplicationStatus()
            ).isEqualTo(
                    "DISTRICT_APPROVED"
            );

            assertThat(
                    response.getApplicationStatus()
            ).isEqualTo(
                    "DISTRICT_APPROVED"
            );
        }

        @Test
        @DisplayName("DISTRICT_REVIEW (high-score path) -> District reject -> REJECTED")
        void districtRejectFromDistrictReviewMovesToRejected() {

            SchemeApplication application =
                    application("DISTRICT_REVIEW");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            mockOfficer(
                    "district.officer",
                    OfficerRole.DISTRICT_OFFICER
            );

            mockSave();
            mockHistory();

            VerificationStatusResponse response =
                    service.rejectAtDistrict(
                            APP_ID,
                            request("district.officer", "Does not meet district criteria.")
                    );

            assertThat(
                    application.getApplicationStatus()
            ).isEqualTo(
                    "REJECTED"
            );

            assertThat(
                    response.getApplicationStatus()
            ).isEqualTo(
                    "REJECTED"
            );
        }
    }

    // ========================================================================
    // GET CRITERIA — ROUTING (auto-creation conditions)
    // ========================================================================

    @Nested
    @DisplayName("Get Criteria Routing")
    class GetCriteriaRouting {

        /**
         * After automatic routing sends a low-score or high-grant application
         * to the District Officer, the status is ESCALATED.
         * getCriteria(DISTRICT) must auto-create District criteria for
         * ESCALATED applications (not only FIELD_APPROVED).
         */
        @Test
        @DisplayName("ESCALATED application gets District criteria created on first getCriteria call")
        void escalatedApplicationGetsDistrictCriteriaCreated() {

            SchemeApplication application =
                    application("ESCALATED");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            // First call returns empty; second call (after saveAll) returns criteria.
            given(
                    verificationCriterionRepository
                            .findBySchemeApplicationIdAndStageOrderByIdAsc(
                                    APP_ID,
                                    VerificationStage.DISTRICT
                            )
            ).willReturn(List.of())
             .willReturn(
                    List.of(
                            criterion(
                                    application,
                                    VerificationStage.DISTRICT,
                                    1,
                                    VerificationCriterionStatus.PENDING
                            )
                    )
            );

            List<VerificationCriterionResponse> result =
                    service.getCriteria(
                            APP_ID,
                            VerificationStage.DISTRICT
                    );

            assertThat(result)
                    .isNotNull()
                    .isNotEmpty();
        }

        /**
         * DISTRICT_REVIEW is the high-score direct path.
         * getCriteria(DISTRICT) must auto-create District criteria for
         * DISTRICT_REVIEW applications (same as ESCALATED).
         */
        @Test
        @DisplayName("DISTRICT_REVIEW application gets District criteria created on first getCriteria call")
        void districtReviewApplicationGetsDistrictCriteriaCreated() {

            SchemeApplication application =
                    application("DISTRICT_REVIEW");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            given(
                    verificationCriterionRepository
                            .findBySchemeApplicationIdAndStageOrderByIdAsc(
                                    APP_ID,
                                    VerificationStage.DISTRICT
                            )
            ).willReturn(List.of())
             .willReturn(
                    List.of(
                            criterion(
                                    application,
                                    VerificationStage.DISTRICT,
                                    1,
                                    VerificationCriterionStatus.PENDING
                            )
                    )
            );

            List<VerificationCriterionResponse> result =
                    service.getCriteria(
                            APP_ID,
                            VerificationStage.DISTRICT
                    );

            assertThat(result)
                    .isNotNull()
                    .isNotEmpty();
        }

        /**
         * UNDER_REVIEW applications are still at the Field stage.
         * getCriteria(DISTRICT) must NOT auto-create District criteria yet.
         */
        @Test
        @DisplayName("UNDER_REVIEW application does NOT get District criteria auto-created")
        void underReviewApplicationDoesNotGetDistrictCriteriaCreated() {

            SchemeApplication application =
                    application("UNDER_REVIEW");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            given(
                    verificationCriterionRepository
                            .findBySchemeApplicationIdAndStageOrderByIdAsc(
                                    APP_ID,
                                    VerificationStage.DISTRICT
                            )
            ).willReturn(List.of());

            List<VerificationCriterionResponse> result =
                    service.getCriteria(
                            APP_ID,
                            VerificationStage.DISTRICT
                    );

            assertThat(result)
                    .isNotNull()
                    .isEmpty();
        }

        /**
         * DISTRICT_APPROVED still triggers Finance criteria creation
         * (existing behaviour must not have regressed).
         */
        @Test
        @DisplayName("DISTRICT_APPROVED application gets Finance criteria created on first getCriteria call")
        void districtApprovedApplicationGetsFinanceCriteriaCreated() {

            SchemeApplication application =
                    application("DISTRICT_APPROVED");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            given(
                    verificationCriterionRepository
                            .findBySchemeApplicationIdAndStageOrderByIdAsc(
                                    APP_ID,
                                    VerificationStage.FINANCE
                            )
            ).willReturn(List.of())
             .willReturn(
                    List.of(
                            criterion(
                                    application,
                                    VerificationStage.FINANCE,
                                    1,
                                    VerificationCriterionStatus.PENDING
                            )
                    )
            );

            List<VerificationCriterionResponse> result =
                    service.getCriteria(
                            APP_ID,
                            VerificationStage.FINANCE
                    );

            assertThat(result)
                    .isNotNull()
                    .isNotEmpty();
        }
    }

    // ========================================================================
    // FINANCE APPROVER
    // ========================================================================

    @Nested
    @DisplayName("Finance Approver")
    class FinanceApprover {

        @Test
        void financeApproveWithAllCriteriaMovesToApproved() {

            SchemeApplication application =
                    application("DISTRICT_APPROVED", new java.math.BigDecimal("6000"));

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            mockOfficer(
                    "finance.officer",
                    OfficerRole.FINANCE_APPROVER
            );

            mockCriteria(
                    application,
                    VerificationStage.FINANCE,
                    4,
                    4
            );

            mockSave();
            mockHistory();

            VerificationStatusResponse response =
                    service.approveAtFinance(
                            APP_ID,
                            financeRequest(
                                    "finance.officer",
                                    new java.math.BigDecimal("5000")
                            )
                    );

            assertThat(
                    application.getApplicationStatus()
            ).isEqualTo(
                    "APPROVED"
            );

            assertThat(
                    response.getApplicationStatus()
            ).isEqualTo(
                    "APPROVED"
            );
        }

        @Test
        void financeApproveWithoutAllCriteriaThrows() {

            SchemeApplication application =
                    application("DISTRICT_APPROVED");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            mockOfficer(
                    "finance.officer",
                    OfficerRole.FINANCE_APPROVER
            );

            mockCriteria(
                    application,
                    VerificationStage.FINANCE,
                    4,
                    2
            );

            assertThatThrownBy(
                    () ->
                            service.approveAtFinance(
                                    APP_ID,
                                    request(
                                            "finance.officer",
                                            null
                                    )
                            )
            )
                    .isInstanceOf(
                            InvalidVerificationTransitionException.class
                    );
        }

        @Test
        void financeRejectMovesToRejected() {

            SchemeApplication application =
                    application("DISTRICT_APPROVED");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            mockOfficer(
                    "finance.officer",
                    OfficerRole.FINANCE_APPROVER
            );

            mockSave();
            mockHistory();

            VerificationStatusResponse response =
                    service.rejectAtFinance(
                            APP_ID,
                            request(
                                    "finance.officer",
                                    "Budget validation failed."
                            )
                    );

            assertThat(
                    application.getApplicationStatus()
            ).isEqualTo(
                    "REJECTED"
            );

            assertThat(
                    response.getApplicationStatus()
            ).isEqualTo(
                    "REJECTED"
            );
        }

        @Test
        void financeRejectWithoutRemarksThrows() {

            SchemeApplication application =
                    application("DISTRICT_APPROVED");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            mockOfficer(
                    "finance.officer",
                    OfficerRole.FINANCE_APPROVER
            );

            assertThatThrownBy(
                    () ->
                            service.rejectAtFinance(
                                    APP_ID,
                                    request(
                                            "finance.officer",
                                            null
                                    )
                            )
            )
                    .isInstanceOf(
                            InvalidVerificationTransitionException.class
                    )
                    .hasMessageContaining(
                            "remarks"
                    );
        }

        @Test
        @DisplayName("UNDER_REVIEW -> Finance approval rejected")
        void financeCannotApproveUnderReview() {

            SchemeApplication application =
                    application("UNDER_REVIEW");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            mockOfficer(
                    "finance.officer",
                    OfficerRole.FINANCE_APPROVER
            );

            assertThatThrownBy(
                    () ->
                            service.approveAtFinance(
                                    APP_ID,
                                    request(
                                            "finance.officer",
                                            null
                                    )
                            )
            )
                    .isInstanceOf(
                            InvalidVerificationTransitionException.class
                    );
        }

        @Test
        @DisplayName("ESCALATED -> Finance approval rejected (must go through District first)")
        void financeCannotApproveEscalated() {

            SchemeApplication application =
                    application("ESCALATED");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            mockOfficer(
                    "finance.officer",
                    OfficerRole.FINANCE_APPROVER
            );

            assertThatThrownBy(
                    () ->
                            service.approveAtFinance(
                                    APP_ID,
                                    request(
                                            "finance.officer",
                                            null
                                    )
                            )
            )
                    .isInstanceOf(
                            InvalidVerificationTransitionException.class
                    );
        }

        @Test
        @DisplayName("DISTRICT_REVIEW -> Finance approval rejected (must go through District first)")
        void financeCannotApproveDistrictReview() {

            // High-score applications reach DISTRICT_REVIEW but still must pass
            // through District Officer before Finance can act.
            SchemeApplication application =
                    application("DISTRICT_REVIEW");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            mockOfficer(
                    "finance.officer",
                    OfficerRole.FINANCE_APPROVER
            );

            assertThatThrownBy(
                    () ->
                            service.approveAtFinance(
                                    APP_ID,
                                    request(
                                            "finance.officer",
                                            null
                                    )
                            )
            )
                    .isInstanceOf(
                            InvalidVerificationTransitionException.class
                    );
        }


        @Test
        void financeCannotApproveAlreadyApprovedApplication() {

            SchemeApplication application =
                    application("APPROVED");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            mockOfficer(
                    "finance.officer",
                    OfficerRole.FINANCE_APPROVER
            );

            assertThatThrownBy(
                    () ->
                            service.approveAtFinance(
                                    APP_ID,
                                    request(
                                            "finance.officer",
                                            null
                                    )
                            )
            )
                    .isInstanceOf(
                            InvalidVerificationTransitionException.class
                    );
        }

        // ── sanctionedAmount validation ─────────────────────────────────

        @Test
        @DisplayName("null sanctionedAmount → throws")
        void financeApprove_nullSanctionedAmount_throws() {

            SchemeApplication application =
                    application("DISTRICT_APPROVED", new java.math.BigDecimal("6000"));

            given(applicationRepository.findById(APP_ID)).willReturn(Optional.of(application));
            mockOfficer("finance.officer", OfficerRole.FINANCE_APPROVER);
            mockCriteria(application, VerificationStage.FINANCE, 4, 4);

            assertThatThrownBy(
                    () -> service.approveAtFinance(APP_ID, financeRequest("finance.officer", null))
            )
                    .isInstanceOf(InvalidVerificationTransitionException.class)
                    .hasMessageContaining("sanctionedAmount is required");
        }

        @Test
        @DisplayName("zero sanctionedAmount → throws")
        void financeApprove_zeroSanctionedAmount_throws() {

            SchemeApplication application =
                    application("DISTRICT_APPROVED", new java.math.BigDecimal("6000"));

            given(applicationRepository.findById(APP_ID)).willReturn(Optional.of(application));
            mockOfficer("finance.officer", OfficerRole.FINANCE_APPROVER);
            mockCriteria(application, VerificationStage.FINANCE, 4, 4);

            assertThatThrownBy(
                    () -> service.approveAtFinance(
                            APP_ID,
                            financeRequest("finance.officer", java.math.BigDecimal.ZERO))
            )
                    .isInstanceOf(InvalidVerificationTransitionException.class)
                    .hasMessageContaining("greater than zero");
        }

        @Test
        @DisplayName("negative sanctionedAmount → throws")
        void financeApprove_negativeSanctionedAmount_throws() {

            SchemeApplication application =
                    application("DISTRICT_APPROVED", new java.math.BigDecimal("6000"));

            given(applicationRepository.findById(APP_ID)).willReturn(Optional.of(application));
            mockOfficer("finance.officer", OfficerRole.FINANCE_APPROVER);
            mockCriteria(application, VerificationStage.FINANCE, 4, 4);

            assertThatThrownBy(
                    () -> service.approveAtFinance(
                            APP_ID,
                            financeRequest("finance.officer", new java.math.BigDecimal("-1")))
            )
                    .isInstanceOf(InvalidVerificationTransitionException.class)
                    .hasMessageContaining("greater than zero");
        }

        @Test
        @DisplayName("sanctionedAmount exceeds grantAmount → throws")
        void financeApprove_amountExceedsGrantAmount_throws() {

            java.math.BigDecimal grantAmount = new java.math.BigDecimal("6000");
            SchemeApplication application =
                    application("DISTRICT_APPROVED", grantAmount);

            given(applicationRepository.findById(APP_ID)).willReturn(Optional.of(application));
            mockOfficer("finance.officer", OfficerRole.FINANCE_APPROVER);
            mockCriteria(application, VerificationStage.FINANCE, 4, 4);

            java.math.BigDecimal tooHigh = new java.math.BigDecimal("6001");

            assertThatThrownBy(
                    () -> service.approveAtFinance(APP_ID, financeRequest("finance.officer", tooHigh))
            )
                    .isInstanceOf(InvalidVerificationTransitionException.class)
                    .hasMessageContaining("must not exceed");
        }

        @Test
        @DisplayName("valid sanctionedAmount ≤ grantAmount → persisted on application")
        void financeApprove_validAmount_persistedOnApplication() {

            java.math.BigDecimal grantAmount = new java.math.BigDecimal("6000");
            SchemeApplication application =
                    application("DISTRICT_APPROVED", grantAmount);

            given(applicationRepository.findById(APP_ID)).willReturn(Optional.of(application));
            mockOfficer("finance.officer", OfficerRole.FINANCE_APPROVER);
            mockCriteria(application, VerificationStage.FINANCE, 4, 4);
            mockSave();
            mockHistory();

            java.math.BigDecimal entered = new java.math.BigDecimal("4500");

            service.approveAtFinance(APP_ID, financeRequest("finance.officer", entered));

            assertThat(application.getSanctionedAmount())
                    .isEqualByComparingTo(entered);
            assertThat(application.getApplicationStatus())
                    .isEqualTo("APPROVED");
        }
    }

    // ========================================================================
    // STATUS
    // ========================================================================

    @Nested
    @DisplayName("Get Status")
    class GetStatus {

        @Test
        void getStatusReturnsCorrectResponse() {

            SchemeApplication application =
                    application("UNDER_REVIEW");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            mockHistory();

            VerificationStatusResponse response =
                    service.getStatus(APP_ID);

            assertThat(
                    response.getApplicationId()
            ).isEqualTo(
                    APP_ID
            );

            assertThat(
                    response.getBeneficiaryId()
            ).isEqualTo(
                    BENEFICIARY_ID
            );

            assertThat(
                    response.getApplicationStatus()
            ).isEqualTo(
                    "UNDER_REVIEW"
            );
        }
    }

    // ========================================================================
    // CRITERIA
    // ========================================================================

    @Nested
    @DisplayName("Verification Criteria")
    class VerificationCriteria {

        @Test
        void getFieldCriteriaReturnsCriteria() {

            SchemeApplication application =
                    application("UNDER_REVIEW");

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            List<VerificationCriterion> criteria =
                    List.of(
                            criterion(
                                    application,
                                    VerificationStage.FIELD,
                                    1,
                                    VerificationCriterionStatus.PENDING
                            ),
                            criterion(
                                    application,
                                    VerificationStage.FIELD,
                                    2,
                                    VerificationCriterionStatus.PENDING
                            )
                    );

            given(
                    verificationCriterionRepository
                            .findBySchemeApplicationIdAndStageOrderByIdAsc(
                                    APP_ID,
                                    VerificationStage.FIELD
                            )
            ).willReturn(criteria);

            List<VerificationCriterionResponse> response =
                    service.getCriteria(
                            APP_ID,
                            VerificationStage.FIELD
                    );

            assertThat(response).hasSize(2);

            assertThat(
                    response.get(0).getStage()
            ).isEqualTo(
                    VerificationStage.FIELD
            );
        }
    }

    // ========================================================================
    // UPDATE CRITERION – FAILED → REJECTED
    // ========================================================================

    @Nested
    @DisplayName("Update Criterion")
    class UpdateCriterion {

        /**
         * Regression test for the FAILED → REJECTED bug.
         *
         * Root cause: VerificationRecord.actionTaken was mapped with
         * {@code length = 10}, but the enum value {@code CRITERION_VERIFIED}
         * is 18 characters.  MySQL strict mode raised a data-too-long error,
         * rolling back the transaction and leaving the application UNDER_REVIEW.
         *
         * Fix: column length raised to 30 in VerificationRecord.
         */
        @Test
        @DisplayName("FAILED criterion sets application status to REJECTED and records rejectedAt")
        void failedCriterionRejectsApplication() {

            SchemeApplication application = application("UNDER_REVIEW");

            VerificationCriterion crit = criterion(
                    application,
                    VerificationStage.FIELD,
                    1,
                    VerificationCriterionStatus.PENDING
            );

            given(applicationRepository.findById(APP_ID))
                    .willReturn(Optional.of(application));

            given(verificationCriterionRepository.findById(1L))
                    .willReturn(Optional.of(crit));

            mockOfficer("field.officer", OfficerRole.FIELD_OFFICER);
            mockSave();

            VerificationCriterionUpdateRequest req =
                    new VerificationCriterionUpdateRequest();
            req.setPerformedBy("field.officer");
            req.setStatus(VerificationCriterionStatus.FAILED);
            req.setRemarks("Document is forged.");

            service.updateCriterion(APP_ID, 1L, req);

            assertThat(application.getApplicationStatus())
                    .isEqualTo("REJECTED");

            assertThat(application.getRejectedAt())
                    .isNotNull()
                    .isBeforeOrEqualTo(LocalDateTime.now());

            verify(applicationRepository).save(application);
        }

        @Test
        @DisplayName("FAILED criterion without remarks throws")
        void failedCriterionWithoutRemarksThrows() {

            SchemeApplication application = application("UNDER_REVIEW");

            VerificationCriterion crit = criterion(
                    application,
                    VerificationStage.FIELD,
                    2,
                    VerificationCriterionStatus.PENDING
            );

            given(applicationRepository.findById(APP_ID))
                    .willReturn(Optional.of(application));

            given(verificationCriterionRepository.findById(2L))
                    .willReturn(Optional.of(crit));

            mockOfficer("field.officer", OfficerRole.FIELD_OFFICER);

            VerificationCriterionUpdateRequest req =
                    new VerificationCriterionUpdateRequest();
            req.setPerformedBy("field.officer");
            req.setStatus(VerificationCriterionStatus.FAILED);
            req.setRemarks(null);

            assertThatThrownBy(() -> service.updateCriterion(APP_ID, 2L, req))
                    .isInstanceOf(InvalidVerificationTransitionException.class)
                    .hasMessageContaining("Remarks are required");

            assertThat(application.getApplicationStatus())
                    .isEqualTo("UNDER_REVIEW");
        }
    }
}