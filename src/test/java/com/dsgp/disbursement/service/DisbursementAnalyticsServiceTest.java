package com.dsgp.disbursement.service;

import com.dsgp.application.entity.SchemeApplication;
import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.disbursement.dto.DisbursementAnalyticsResponse;
import com.dsgp.disbursement.entity.ComplianceStatus;
import com.dsgp.disbursement.entity.DisbursementPlan;
import com.dsgp.disbursement.entity.DisbursementStage;
import com.dsgp.disbursement.entity.DisbursementStageStatus;
import com.dsgp.disbursement.entity.DisbursementStatus;
import com.dsgp.disbursement.entity.DisbursementType;
import com.dsgp.disbursement.repository.DisbursementPlanRepository;
import com.dsgp.disbursement.repository.DisbursementStageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("DisbursementAnalyticsService")
class DisbursementAnalyticsServiceTest {

    @Mock
    private DisbursementPlanRepository disbursementPlanRepository;

    @Mock
    private DisbursementStageRepository disbursementStageRepository;

    @InjectMocks
    private DisbursementAnalyticsService disbursementAnalyticsService;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private Beneficiary beneficiary(String state) {
        Beneficiary b = new Beneficiary();
        b.setId(1);
        b.setFullName("Ravi Kumar");
        b.setState(state);
        return b;
    }

    private Scheme scheme(String schemeName) {
        Scheme s = new Scheme();
        s.setId(1L);
        s.setSchemeName(schemeName);
        return s;
    }

    private SchemeApplication application(String schemeName, String state, BigDecimal sanctioned) {
        Beneficiary b = beneficiary(state);
        Scheme sc = scheme(schemeName);

        SchemeApplication app = SchemeApplication.builder()
                .beneficiary(b)
                .scheme(sc)
                .applicationStatus("APPROVED")
                .sanctionedAmount(sanctioned)
                .build();
        app.setId(10L);
        return app;
    }

    private DisbursementPlan plan(SchemeApplication app, BigDecimal released, BigDecimal remaining) {
        DisbursementPlan p = DisbursementPlan.builder()
                .application(app)
                .totalAmount(app.getSanctionedAmount())
                .releasedAmount(released)
                .remainingAmount(remaining)
                .disbursementType(DisbursementType.STAGED)
                .status(released.compareTo(app.getSanctionedAmount()) == 0
                        ? DisbursementStatus.FULLY_RELEASED
                        : released.compareTo(BigDecimal.ZERO) == 0
                            ? DisbursementStatus.PENDING
                            : DisbursementStatus.PARTIALLY_RELEASED)
                .build();
        p.setId(1L);
        return p;
    }

    private DisbursementStage stage(DisbursementPlan plan, BigDecimal amount, DisbursementStageStatus status) {
        DisbursementStage s = DisbursementStage.builder()
                .disbursementPlan(plan)
                .stageNumber(1)
                .amount(amount)
                .milestone("Test Milestone")
                .dueDate(LocalDate.now().plusDays(30))
                .status(status)
                .complianceStatus(ComplianceStatus.PENDING)
                .build();
        s.setId(1L);
        return s;
    }

    // ── Empty data ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("with no disbursement plans")
    class NoPlanData {

        @Test
        @DisplayName("returns zero for all totals")
        void noPlans_allTotalsAreZero() {
            given(disbursementPlanRepository.findAll()).willReturn(List.of());
            given(disbursementStageRepository.findAll()).willReturn(List.of());

            DisbursementAnalyticsResponse result = disbursementAnalyticsService.getAnalytics();

            assertThat(result.getTotalSanctioned()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getTotalReleased()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getTotalRemaining()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getTotalPlanned()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("returns empty scheme and state maps")
        void noPlans_emptyBreakdownMaps() {
            given(disbursementPlanRepository.findAll()).willReturn(List.of());
            given(disbursementStageRepository.findAll()).willReturn(List.of());

            DisbursementAnalyticsResponse result = disbursementAnalyticsService.getAnalytics();

            assertThat(result.getReleasedByScheme()).isEmpty();
            assertThat(result.getReleasedByState()).isEmpty();
        }
    }

    // ── Single plan ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("with one disbursement plan")
    class SinglePlan {

        @Test
        @DisplayName("totalSanctioned equals the plan's totalAmount")
        void singlePlan_totalSanctionedCorrect() {
            SchemeApplication app = application("PM-KISAN", "Maharashtra", new BigDecimal("6000.00"));
            DisbursementPlan p = plan(app, BigDecimal.ZERO, new BigDecimal("6000.00"));
            given(disbursementPlanRepository.findAll()).willReturn(List.of(p));
            given(disbursementStageRepository.findAll()).willReturn(List.of());

            DisbursementAnalyticsResponse result = disbursementAnalyticsService.getAnalytics();

            assertThat(result.getTotalSanctioned()).isEqualByComparingTo("6000.00");
        }

        @Test
        @DisplayName("totalReleased equals the plan's releasedAmount")
        void singlePartiallyReleasedPlan_totalReleasedCorrect() {
            SchemeApplication app = application("PM-KISAN", "Maharashtra", new BigDecimal("6000.00"));
            DisbursementPlan p = plan(app, new BigDecimal("2000.00"), new BigDecimal("4000.00"));
            given(disbursementPlanRepository.findAll()).willReturn(List.of(p));
            given(disbursementStageRepository.findAll()).willReturn(List.of());

            DisbursementAnalyticsResponse result = disbursementAnalyticsService.getAnalytics();

            assertThat(result.getTotalReleased()).isEqualByComparingTo("2000.00");
        }

        @Test
        @DisplayName("totalRemaining equals the plan's remainingAmount")
        void singlePartiallyReleasedPlan_totalRemainingCorrect() {
            SchemeApplication app = application("PM-KISAN", "Maharashtra", new BigDecimal("6000.00"));
            DisbursementPlan p = plan(app, new BigDecimal("2000.00"), new BigDecimal("4000.00"));
            given(disbursementPlanRepository.findAll()).willReturn(List.of(p));
            given(disbursementStageRepository.findAll()).willReturn(List.of());

            DisbursementAnalyticsResponse result = disbursementAnalyticsService.getAnalytics();

            assertThat(result.getTotalRemaining()).isEqualByComparingTo("4000.00");
        }

        @Test
        @DisplayName("totalPlanned equals the sum of stage amounts")
        void singlePlanWithStages_totalPlannedFromStages() {
            SchemeApplication app = application("PM-KISAN", "Maharashtra", new BigDecimal("6000.00"));
            DisbursementPlan p = plan(app, BigDecimal.ZERO, new BigDecimal("6000.00"));
            DisbursementStage s1 = stage(p, new BigDecimal("2000.00"), DisbursementStageStatus.PENDING);
            DisbursementStage s2 = stage(p, new BigDecimal("2000.00"), DisbursementStageStatus.PENDING);

            given(disbursementPlanRepository.findAll()).willReturn(List.of(p));
            given(disbursementStageRepository.findAll()).willReturn(List.of(s1, s2));

            DisbursementAnalyticsResponse result = disbursementAnalyticsService.getAnalytics();

            assertThat(result.getTotalPlanned()).isEqualByComparingTo("4000.00");
        }
    }

    // ── Scheme-wise breakdown ─────────────────────────────────────────────────

    @Nested
    @DisplayName("scheme-wise analytics")
    class SchemeWise {

        @Test
        @DisplayName("groups released amounts by scheme name")
        void multiplePlansWithSameScheme_aggregatesReleasedByScheme() {
            SchemeApplication app1 = application("PM-KISAN", "Maharashtra", new BigDecimal("6000.00"));
            SchemeApplication app2 = application("PM-KISAN", "Gujarat", new BigDecimal("6000.00"));
            DisbursementPlan p1 = plan(app1, new BigDecimal("2000.00"), new BigDecimal("4000.00"));
            DisbursementPlan p2 = plan(app2, new BigDecimal("2000.00"), new BigDecimal("4000.00"));

            given(disbursementPlanRepository.findAll()).willReturn(List.of(p1, p2));
            given(disbursementStageRepository.findAll()).willReturn(List.of());

            DisbursementAnalyticsResponse result = disbursementAnalyticsService.getAnalytics();

            assertThat(result.getReleasedByScheme()).containsKey("PM-KISAN");
            assertThat(result.getReleasedByScheme().get("PM-KISAN")).isEqualByComparingTo("4000.00");
        }

        @Test
        @DisplayName("separates different schemes into different map entries")
        void twoSchemes_separateMapEntries() {
            SchemeApplication app1 = application("PM-KISAN", "Maharashtra", new BigDecimal("6000.00"));
            SchemeApplication app2 = application("NSP", "Maharashtra", new BigDecimal("50000.00"));
            DisbursementPlan p1 = plan(app1, new BigDecimal("6000.00"), BigDecimal.ZERO);
            DisbursementPlan p2 = plan(app2, new BigDecimal("50000.00"), BigDecimal.ZERO);

            given(disbursementPlanRepository.findAll()).willReturn(List.of(p1, p2));
            given(disbursementStageRepository.findAll()).willReturn(List.of());

            DisbursementAnalyticsResponse result = disbursementAnalyticsService.getAnalytics();

            assertThat(result.getReleasedByScheme()).containsKey("PM-KISAN");
            assertThat(result.getReleasedByScheme()).containsKey("NSP");
            assertThat(result.getReleasedByScheme().get("PM-KISAN")).isEqualByComparingTo("6000.00");
            assertThat(result.getReleasedByScheme().get("NSP")).isEqualByComparingTo("50000.00");
        }

        @Test
        @DisplayName("does not include unreleased plans in scheme breakdown")
        void unreleasedPlan_notInSchemeBreakdown() {
            SchemeApplication app = application("PM-KISAN", "Maharashtra", new BigDecimal("6000.00"));
            DisbursementPlan p = plan(app, BigDecimal.ZERO, new BigDecimal("6000.00"));

            given(disbursementPlanRepository.findAll()).willReturn(List.of(p));
            given(disbursementStageRepository.findAll()).willReturn(List.of());

            DisbursementAnalyticsResponse result = disbursementAnalyticsService.getAnalytics();

            assertThat(result.getReleasedByScheme()).doesNotContainKey("PM-KISAN");
        }
    }

    // ── State-wise breakdown ──────────────────────────────────────────────────

    @Nested
    @DisplayName("state-wise analytics")
    class StateWise {

        @Test
        @DisplayName("groups released amounts by beneficiary state")
        void multiplePlansInSameState_aggregatesReleasedByState() {
            SchemeApplication app1 = application("PM-KISAN", "Maharashtra", new BigDecimal("6000.00"));
            SchemeApplication app2 = application("NSP", "Maharashtra", new BigDecimal("4000.00"));
            DisbursementPlan p1 = plan(app1, new BigDecimal("6000.00"), BigDecimal.ZERO);
            DisbursementPlan p2 = plan(app2, new BigDecimal("4000.00"), BigDecimal.ZERO);

            given(disbursementPlanRepository.findAll()).willReturn(List.of(p1, p2));
            given(disbursementStageRepository.findAll()).willReturn(List.of());

            DisbursementAnalyticsResponse result = disbursementAnalyticsService.getAnalytics();

            assertThat(result.getReleasedByState()).containsKey("Maharashtra");
            assertThat(result.getReleasedByState().get("Maharashtra")).isEqualByComparingTo("10000.00");
        }

        @Test
        @DisplayName("separates different states into different map entries")
        void twoStates_separateMapEntries() {
            SchemeApplication app1 = application("PM-KISAN", "Maharashtra", new BigDecimal("6000.00"));
            SchemeApplication app2 = application("PM-KISAN", "Gujarat", new BigDecimal("6000.00"));
            DisbursementPlan p1 = plan(app1, new BigDecimal("6000.00"), BigDecimal.ZERO);
            DisbursementPlan p2 = plan(app2, new BigDecimal("6000.00"), BigDecimal.ZERO);

            given(disbursementPlanRepository.findAll()).willReturn(List.of(p1, p2));
            given(disbursementStageRepository.findAll()).willReturn(List.of());

            DisbursementAnalyticsResponse result = disbursementAnalyticsService.getAnalytics();

            assertThat(result.getReleasedByState()).containsKey("Maharashtra");
            assertThat(result.getReleasedByState()).containsKey("Gujarat");
            assertThat(result.getReleasedByState().get("Maharashtra")).isEqualByComparingTo("6000.00");
            assertThat(result.getReleasedByState().get("Gujarat")).isEqualByComparingTo("6000.00");
        }

        @Test
        @DisplayName("does not include unreleased plans in state breakdown")
        void unreleasedPlan_notInStateBreakdown() {
            SchemeApplication app = application("PM-KISAN", "Maharashtra", new BigDecimal("6000.00"));
            DisbursementPlan p = plan(app, BigDecimal.ZERO, new BigDecimal("6000.00"));

            given(disbursementPlanRepository.findAll()).willReturn(List.of(p));
            given(disbursementStageRepository.findAll()).willReturn(List.of());

            DisbursementAnalyticsResponse result = disbursementAnalyticsService.getAnalytics();

            assertThat(result.getReleasedByState()).doesNotContainKey("Maharashtra");
        }
    }

    // ── Multiple plans ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("with multiple disbursement plans")
    class MultiplePlans {

        @Test
        @DisplayName("sums totalSanctioned across all plans")
        void threePlans_totalSanctionedIsSumOfAll() {
            SchemeApplication app1 = application("PM-KISAN", "Maharashtra", new BigDecimal("6000.00"));
            SchemeApplication app2 = application("NSP", "Gujarat", new BigDecimal("50000.00"));
            SchemeApplication app3 = application("PMEGP", "Rajasthan", new BigDecimal("500000.00"));
            DisbursementPlan p1 = plan(app1, BigDecimal.ZERO, new BigDecimal("6000.00"));
            DisbursementPlan p2 = plan(app2, BigDecimal.ZERO, new BigDecimal("50000.00"));
            DisbursementPlan p3 = plan(app3, BigDecimal.ZERO, new BigDecimal("500000.00"));

            given(disbursementPlanRepository.findAll()).willReturn(List.of(p1, p2, p3));
            given(disbursementStageRepository.findAll()).willReturn(List.of());

            DisbursementAnalyticsResponse result = disbursementAnalyticsService.getAnalytics();

            assertThat(result.getTotalSanctioned()).isEqualByComparingTo("556000.00");
        }

        @Test
        @DisplayName("sums totalReleased across all plans")
        void threePlansWithPartialReleases_totalReleasedIsCorrect() {
            SchemeApplication app1 = application("PM-KISAN", "Maharashtra", new BigDecimal("6000.00"));
            SchemeApplication app2 = application("NSP", "Gujarat", new BigDecimal("50000.00"));
            DisbursementPlan p1 = plan(app1, new BigDecimal("2000.00"), new BigDecimal("4000.00"));
            DisbursementPlan p2 = plan(app2, new BigDecimal("10000.00"), new BigDecimal("40000.00"));

            given(disbursementPlanRepository.findAll()).willReturn(List.of(p1, p2));
            given(disbursementStageRepository.findAll()).willReturn(List.of());

            DisbursementAnalyticsResponse result = disbursementAnalyticsService.getAnalytics();

            assertThat(result.getTotalReleased()).isEqualByComparingTo("12000.00");
        }
    }
}
