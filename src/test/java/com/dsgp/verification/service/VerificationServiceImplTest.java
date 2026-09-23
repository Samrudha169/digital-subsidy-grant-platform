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
import com.dsgp.verification.repository.VerificationCriterionRepository;
import com.dsgp.verification.repository.VerificationRecordRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @BeforeEach
    void injectRoutingThresholds() {
        // @Value fields are not injected by Mockito. Set them explicitly so
        // that the routing logic uses the same defaults as application.properties.
        ReflectionTestUtils.setField(
                service,
                "minimumDirectFinanceScore",
                60
        );
        ReflectionTestUtils.setField(
                service,
                "maximumDirectFinanceGrantAmount",
                10000
        );
    }

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
        void eligiblePendingApplicationMovesToUnderReview() {

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
                            eligibleResult()
                    )
            );

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
        void fieldApprovalWithAllCriteriaMovesToFieldApproved() {

            // score=100, grant=5000 (below 10000 threshold) → FIELD_APPROVED
            SchemeApplication application =
                    application(
                            "UNDER_REVIEW",
                            new java.math.BigDecimal("5000")
                    );

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
                    Optional.of(eligibleResult())
            );

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
                    "FIELD_APPROVED"
            );

            assertThat(
                    response.getApplicationStatus()
            ).isEqualTo(
                    "FIELD_APPROVED"
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
        void completeFieldWithAllCriteriaMovesToFieldApproved() {

            // score=100, grant=5000 (below 10000 threshold) → FIELD_APPROVED
            SchemeApplication application =
                    application(
                            "UNDER_REVIEW",
                            new java.math.BigDecimal("5000")
                    );

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
                    Optional.of(eligibleResult())
            );

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
                    "FIELD_APPROVED"
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
    // FIELD OFFICER — ROUTING
    // ========================================================================

    @Nested
    @DisplayName("Field Officer Routing")
    class FieldOfficerRouting {

        // Default thresholds from application.properties:
        //   minimumDirectFinanceScore         = 60
        //   maximumDirectFinanceGrantAmount   = 10000

        @Test
        @DisplayName("high score + low grant amount routes to FIELD_APPROVED (direct Finance)")
        void highScoreAndLowGrantRoutesToFieldApproved() {

            // score=100 (>=60), grant=6000 (<=10000) → FIELD_APPROVED
            SchemeApplication application =
                    application(
                            "UNDER_REVIEW",
                            new java.math.BigDecimal("6000")
                    );

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
                    "FIELD_APPROVED"
            );
        }

        @Test
        @DisplayName("high score + grant above threshold routes to ESCALATED")
        void highScoreAndHighGrantRoutesToEscalated() {

            // score=100 (>=60), grant=50000 (>10000) → ESCALATED
            SchemeApplication application =
                    application(
                            "UNDER_REVIEW",
                            new java.math.BigDecimal("50000")
                    );

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
        @DisplayName("score below direct-finance threshold routes to ESCALATED regardless of grant")
        void lowScoreRoutesToEscalatedRegardlessOfGrant() {

            // score=40 (<60), grant=500 (<=10000) → ESCALATED (score fails)
            SchemeApplication application =
                    application(
                            "UNDER_REVIEW",
                            new java.math.BigDecimal("500")
                    );

            given(
                    applicationRepository.findById(APP_ID)
            ).willReturn(
                    Optional.of(application)
            );

            EligibilityResult lowScore =
                    EligibilityResult.builder()
                            .beneficiaryId(BENEFICIARY_ID)
                            .schemeId(SCHEME_ID)
                            .schemeName("PM-KISAN")
                            .totalScore(40)
                            .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                            .evaluatedAt(java.time.LocalDateTime.now())
                            .build();

            given(
                    eligibilityResultRepository
                            .findByBeneficiaryIdAndSchemeId(
                                    BENEFICIARY_ID,
                                    SCHEME_ID
                            )
            ).willReturn(
                    Optional.of(lowScore)
            );

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
        @DisplayName("null grant amount routes to ESCALATED")
        void nullGrantAmountRoutesToEscalated() {

            // score=100 (>=60), grant=null → ESCALATED (null treated as above threshold)
            SchemeApplication application =
                    application(
                            "UNDER_REVIEW",
                            null   // null grant amount
                    );

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
         * FIELD_APPROVED is the direct-to-Finance route.
         * getCriteria(DISTRICT) must NOT create District criteria for a
         * FIELD_APPROVED application — it bypassed the District Officer.
         */
        @Test
        @DisplayName("FIELD_APPROVED application does NOT get District criteria created")
        void fieldApprovedApplicationDoesNotGetDistrictCriteriaCreated() {

            SchemeApplication application =
                    application("FIELD_APPROVED");

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
         * After automatic routing sends a direct-Finance application to
         * FIELD_APPROVED, getCriteria(FINANCE) must auto-create Finance
         * criteria (was broken: it was only checking DISTRICT_APPROVED).
         */
        @Test
        @DisplayName("FIELD_APPROVED application gets Finance criteria created on first getCriteria call")
        void fieldApprovedApplicationGetsFinanceCriteriaCreated() {

            SchemeApplication application =
                    application("FIELD_APPROVED");

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
        @DisplayName("FIELD_APPROVED + all Finance criteria VERIFIED -> APPROVED (direct route)")
        void financeApproveFromFieldApprovedMovesToApproved() {

            // Direct Finance route: high score + low grant skipped District.
            SchemeApplication application =
                    application("FIELD_APPROVED", new java.math.BigDecimal("6000"));

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
        @DisplayName("ESCALATED -> Finance approval rejected")
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
        @DisplayName("FIELD_APPROVED without all Finance criteria -> rejected")
        void financeCannotApproveFieldApprovedWithIncompleteCriteria() {

            SchemeApplication application =
                    application("FIELD_APPROVED");

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
                    1
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
                    )
                    .hasMessageContaining(
                            "cannot approve until all verification criteria are VERIFIED"
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
}