package com.dsgp.disbursement.service;

import com.dsgp.application.entity.SchemeApplication;
import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.disbursement.entity.ComplianceStatus;
import com.dsgp.disbursement.entity.DisbursementPlan;
import com.dsgp.disbursement.entity.DisbursementStage;
import com.dsgp.disbursement.entity.DisbursementStageStatus;
import com.dsgp.disbursement.entity.DisbursementStatus;
import com.dsgp.disbursement.entity.DisbursementType;
import com.dsgp.disbursement.repository.DisbursementStageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link DisbursementReminderScheduler}.
 *
 * <p>The scheduler logs-only (no email/SMS). Tests verify the correct
 * repository query parameters and that it handles empty result sets
 * without errors.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DisbursementReminderScheduler")
class DisbursementReminderSchedulerTest {

    @Mock
    private DisbursementStageRepository disbursementStageRepository;

    @InjectMocks
    private DisbursementReminderScheduler disbursementReminderScheduler;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private DisbursementStage pendingStageWithDueDate(LocalDate dueDate) {
        Beneficiary b = new Beneficiary();
        b.setId(1);
        b.setFullName("Ravi Kumar");

        Scheme s = new Scheme();
        s.setId(1L);
        s.setSchemeName("PM-KISAN");

        SchemeApplication app = SchemeApplication.builder()
                .beneficiary(b)
                .scheme(s)
                .applicationStatus("APPROVED")
                .sanctionedAmount(new BigDecimal("6000.00"))
                .build();
        app.setId(10L);

        DisbursementPlan plan = DisbursementPlan.builder()
                .application(app)
                .totalAmount(new BigDecimal("6000.00"))
                .releasedAmount(BigDecimal.ZERO)
                .remainingAmount(new BigDecimal("6000.00"))
                .disbursementType(DisbursementType.STAGED)
                .status(DisbursementStatus.PENDING)
                .build();
        plan.setId(1L);

        DisbursementStage stage = DisbursementStage.builder()
                .disbursementPlan(plan)
                .stageNumber(1)
                .amount(new BigDecimal("2000.00"))
                .milestone("Equipment purchase")
                .dueDate(dueDate)
                .status(DisbursementStageStatus.PENDING)
                .complianceStatus(ComplianceStatus.PENDING)
                .build();
        stage.setId(1L);
        return stage;
    }

    // ── Scheduler: approaching stages found ───────────────────────────────────

    @Nested
    @DisplayName("when approaching due-date stages exist")
    class ApproachingStagesExist {

        @Test
        @DisplayName("queries the repository using RELEASED as the excluded status")
        void queriesRepositoryWithReleasedExcluded() {
            DisbursementStage stage = pendingStageWithDueDate(LocalDate.now().plusDays(2));
            given(disbursementStageRepository.findByStatusNotAndDueDateBetween(
                    eq(DisbursementStageStatus.RELEASED), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(List.of(stage));

            disbursementReminderScheduler.sendApproachingDueDateReminders();

            ArgumentCaptor<DisbursementStageStatus> statusCaptor =
                    ArgumentCaptor.forClass(DisbursementStageStatus.class);
            verify(disbursementStageRepository).findByStatusNotAndDueDateBetween(
                    statusCaptor.capture(), any(LocalDate.class), any(LocalDate.class));

            assertThat(statusCaptor.getValue()).isEqualTo(DisbursementStageStatus.RELEASED);
        }

        @Test
        @DisplayName("queries starting from today's date")
        void queriesFromToday() {
            DisbursementStage stage = pendingStageWithDueDate(LocalDate.now().plusDays(1));
            given(disbursementStageRepository.findByStatusNotAndDueDateBetween(
                    any(), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(List.of(stage));

            disbursementReminderScheduler.sendApproachingDueDateReminders();

            ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
            ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
            verify(disbursementStageRepository).findByStatusNotAndDueDateBetween(
                    any(), fromCaptor.capture(), toCaptor.capture());

            assertThat(fromCaptor.getValue()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("queries with a 3-day window end date")
        void queriesWithThreeDayWindow() {
            DisbursementStage stage = pendingStageWithDueDate(LocalDate.now().plusDays(2));
            given(disbursementStageRepository.findByStatusNotAndDueDateBetween(
                    any(), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(List.of(stage));

            disbursementReminderScheduler.sendApproachingDueDateReminders();

            ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
            verify(disbursementStageRepository).findByStatusNotAndDueDateBetween(
                    any(), any(LocalDate.class), toCaptor.capture());

            assertThat(toCaptor.getValue()).isEqualTo(LocalDate.now().plusDays(3));
        }

        @Test
        @DisplayName("does not throw when matching stages are found")
        void matchingStagesFound_doesNotThrow() {
            DisbursementStage stage = pendingStageWithDueDate(LocalDate.now().plusDays(1));
            given(disbursementStageRepository.findByStatusNotAndDueDateBetween(
                    any(), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(List.of(stage));

            org.assertj.core.api.Assertions.assertThatCode(
                    () -> disbursementReminderScheduler.sendApproachingDueDateReminders())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("handles multiple stages within the window without throwing")
        void multipleMatchingStages_noException() {
            DisbursementStage s1 = pendingStageWithDueDate(LocalDate.now().plusDays(1));
            DisbursementStage s2 = pendingStageWithDueDate(LocalDate.now().plusDays(2));
            DisbursementStage s3 = pendingStageWithDueDate(LocalDate.now().plusDays(3));
            given(disbursementStageRepository.findByStatusNotAndDueDateBetween(
                    any(), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(List.of(s1, s2, s3));

            org.assertj.core.api.Assertions.assertThatCode(
                    () -> disbursementReminderScheduler.sendApproachingDueDateReminders())
                    .doesNotThrowAnyException();
        }
    }

    // ── Scheduler: no stages found ────────────────────────────────────────────

    @Nested
    @DisplayName("when no approaching due-date stages exist")
    class NoApproachingStages {

        @Test
        @DisplayName("does not throw when repository returns empty list")
        void emptyResult_doesNotThrow() {
            given(disbursementStageRepository.findByStatusNotAndDueDateBetween(
                    any(), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(List.of());

            org.assertj.core.api.Assertions.assertThatCode(
                    () -> disbursementReminderScheduler.sendApproachingDueDateReminders())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("still invokes repository query even with empty results")
        void emptyResult_stillCallsRepository() {
            given(disbursementStageRepository.findByStatusNotAndDueDateBetween(
                    any(), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(List.of());

            disbursementReminderScheduler.sendApproachingDueDateReminders();

            verify(disbursementStageRepository).findByStatusNotAndDueDateBetween(
                    any(), any(LocalDate.class), any(LocalDate.class));
        }
    }

    // ── Scheduler: stages outside the window ──────────────────────────────────

    @Nested
    @DisplayName("stages outside the reminder window")
    class StagesOutsideWindow {

        @Test
        @DisplayName("repository exclusion means stages due in 7+ days are not returned")
        void stageOutsideWindow_notReturnedByRepositoryQuery() {
            // Stage due in 10 days — outside the 3-day window
            given(disbursementStageRepository.findByStatusNotAndDueDateBetween(
                    eq(DisbursementStageStatus.RELEASED),
                    eq(LocalDate.now()),
                    eq(LocalDate.now().plusDays(3))))
                    .willReturn(List.of()); // repository correctly filters

            disbursementReminderScheduler.sendApproachingDueDateReminders();

            // Confirm repository was called with the exact 3-day window
            verify(disbursementStageRepository).findByStatusNotAndDueDateBetween(
                    DisbursementStageStatus.RELEASED,
                    LocalDate.now(),
                    LocalDate.now().plusDays(3));
        }
    }
}
