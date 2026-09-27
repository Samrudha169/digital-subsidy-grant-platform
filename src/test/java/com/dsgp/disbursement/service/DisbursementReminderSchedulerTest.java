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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link DisbursementReminderScheduler}.
 *
 * <p>Covers both:
 * <ul>
 *   <li>{@code sendApproachingDueDateReminders} — reminder logging</li>
 *   <li>{@code flagOverdueStagesAsNonCompliant} — automatic NON_COMPLIANT flagging</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DisbursementReminderScheduler")
class DisbursementReminderSchedulerTest {

    @Mock
    private DisbursementStageRepository disbursementStageRepository;

    @Mock
    private DisbursementStageService disbursementStageService;

    @InjectMocks
    private DisbursementReminderScheduler disbursementReminderScheduler;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private DisbursementStage stageWithDueDateAndCompliance(
            Long id,
            LocalDate dueDate,
            DisbursementStageStatus stageStatus,
            ComplianceStatus complianceStatus) {

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
                .status(stageStatus)
                .complianceStatus(complianceStatus)
                .build();
        stage.setId(id);
        return stage;
    }

    /** Shorthand: PENDING stage with PENDING compliance, given due date. */
    private DisbursementStage pendingStageWithDueDate(LocalDate dueDate) {
        return stageWithDueDateAndCompliance(
                1L, dueDate, DisbursementStageStatus.PENDING, ComplianceStatus.PENDING);
    }

    // =========================================================================
    // sendApproachingDueDateReminders()
    // =========================================================================

    @Nested
    @DisplayName("sendApproachingDueDateReminders()")
    class SendApproachingDueDateReminders {

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
                verify(disbursementStageRepository).findByStatusNotAndDueDateBetween(
                        any(), fromCaptor.capture(), any(LocalDate.class));

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

                assertThatCode(() -> disbursementReminderScheduler.sendApproachingDueDateReminders())
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

                assertThatCode(() -> disbursementReminderScheduler.sendApproachingDueDateReminders())
                        .doesNotThrowAnyException();
            }
        }

        @Nested
        @DisplayName("when no approaching due-date stages exist")
        class NoApproachingStages {

            @Test
            @DisplayName("does not throw when repository returns empty list")
            void emptyResult_doesNotThrow() {
                given(disbursementStageRepository.findByStatusNotAndDueDateBetween(
                        any(), any(LocalDate.class), any(LocalDate.class)))
                        .willReturn(List.of());

                assertThatCode(() -> disbursementReminderScheduler.sendApproachingDueDateReminders())
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

        @Nested
        @DisplayName("stages outside the reminder window")
        class StagesOutsideWindow {

            @Test
            @DisplayName("repository is called with the exact 3-day window bounds")
            void stageOutsideWindow_repositoryCalledWithCorrectBounds() {
                given(disbursementStageRepository.findByStatusNotAndDueDateBetween(
                        eq(DisbursementStageStatus.RELEASED),
                        eq(LocalDate.now()),
                        eq(LocalDate.now().plusDays(3))))
                        .willReturn(List.of());

                disbursementReminderScheduler.sendApproachingDueDateReminders();

                verify(disbursementStageRepository).findByStatusNotAndDueDateBetween(
                        DisbursementStageStatus.RELEASED,
                        LocalDate.now(),
                        LocalDate.now().plusDays(3));
            }
        }
    }

    // =========================================================================
    // flagOverdueStagesAsNonCompliant()
    // =========================================================================

    @Nested
    @DisplayName("flagOverdueStagesAsNonCompliant()")
    class FlagOverdueStagesAsNonCompliant {

        @Nested
        @DisplayName("when overdue non-released stages exist")
        class OverdueStagesExist {

            @Test
            @DisplayName("queries repository with RELEASED excluded and today as the cutoff")
            void queriesRepositoryWithCorrectArguments() {
                DisbursementStage stage = pendingStageWithDueDate(LocalDate.now().minusDays(1));
                given(disbursementStageRepository.findByStatusNotAndDueDateBefore(
                        eq(DisbursementStageStatus.RELEASED), any(LocalDate.class)))
                        .willReturn(List.of(stage));
                given(disbursementStageService.updateComplianceStatus(any(), any()))
                        .willReturn(stage);

                disbursementReminderScheduler.flagOverdueStagesAsNonCompliant();

                verify(disbursementStageRepository).findByStatusNotAndDueDateBefore(
                        DisbursementStageStatus.RELEASED, LocalDate.now());
            }

            @Test
            @DisplayName("calls updateComplianceStatus(NON_COMPLIANT) for each PENDING-compliance stage")
            void pendingComplianceStage_isMarkedNonCompliant() {
                DisbursementStage stage = pendingStageWithDueDate(LocalDate.now().minusDays(5));
                given(disbursementStageRepository.findByStatusNotAndDueDateBefore(
                        any(), any())).willReturn(List.of(stage));
                given(disbursementStageService.updateComplianceStatus(any(), any()))
                        .willReturn(stage);

                disbursementReminderScheduler.flagOverdueStagesAsNonCompliant();

                verify(disbursementStageService).updateComplianceStatus(
                        stage.getId(), ComplianceStatus.NON_COMPLIANT);
            }

            @Test
            @DisplayName("calls updateComplianceStatus once per qualifying stage")
            void multipleOverdueStages_eachIsFlagged() {
                DisbursementStage s1 = stageWithDueDateAndCompliance(
                        1L, LocalDate.now().minusDays(1),
                        DisbursementStageStatus.PENDING, ComplianceStatus.PENDING);
                DisbursementStage s2 = stageWithDueDateAndCompliance(
                        2L, LocalDate.now().minusDays(2),
                        DisbursementStageStatus.VERIFIED, ComplianceStatus.PENDING);
                DisbursementStage s3 = stageWithDueDateAndCompliance(
                        3L, LocalDate.now().minusDays(3),
                        DisbursementStageStatus.PENDING, ComplianceStatus.COMPLETED);

                given(disbursementStageRepository.findByStatusNotAndDueDateBefore(
                        any(), any())).willReturn(List.of(s1, s2, s3));
                given(disbursementStageService.updateComplianceStatus(any(), any()))
                        .willReturn(s1);

                disbursementReminderScheduler.flagOverdueStagesAsNonCompliant();

                // s1 and s2 are PENDING compliance → should be flagged
                // s3 is COMPLETED compliance → should also be flagged (only NON_COMPLIANT is skipped)
                verify(disbursementStageService, times(3))
                        .updateComplianceStatus(any(), eq(ComplianceStatus.NON_COMPLIANT));
            }

            @Test
            @DisplayName("skips stages already marked NON_COMPLIANT (idempotency)")
            void alreadyNonCompliantStage_isSkipped() {
                DisbursementStage alreadyFlagged = stageWithDueDateAndCompliance(
                        1L, LocalDate.now().minusDays(1),
                        DisbursementStageStatus.PENDING, ComplianceStatus.NON_COMPLIANT);

                given(disbursementStageRepository.findByStatusNotAndDueDateBefore(
                        any(), any())).willReturn(List.of(alreadyFlagged));

                disbursementReminderScheduler.flagOverdueStagesAsNonCompliant();

                verify(disbursementStageService, never())
                        .updateComplianceStatus(any(), any());
            }

            @Test
            @DisplayName("mixed batch: only non-NON_COMPLIANT stages are updated")
            void mixedBatch_onlyPendingAndCompletedAreFlagged() {
                DisbursementStage pending = stageWithDueDateAndCompliance(
                        1L, LocalDate.now().minusDays(1),
                        DisbursementStageStatus.PENDING, ComplianceStatus.PENDING);
                DisbursementStage alreadyFlagged = stageWithDueDateAndCompliance(
                        2L, LocalDate.now().minusDays(2),
                        DisbursementStageStatus.PENDING, ComplianceStatus.NON_COMPLIANT);

                given(disbursementStageRepository.findByStatusNotAndDueDateBefore(
                        any(), any())).willReturn(List.of(pending, alreadyFlagged));
                given(disbursementStageService.updateComplianceStatus(
                        eq(1L), eq(ComplianceStatus.NON_COMPLIANT))).willReturn(pending);

                disbursementReminderScheduler.flagOverdueStagesAsNonCompliant();

                // Only the PENDING-compliance stage should be updated
                verify(disbursementStageService, times(1))
                        .updateComplianceStatus(1L, ComplianceStatus.NON_COMPLIANT);
                verify(disbursementStageService, never())
                        .updateComplianceStatus(eq(2L), any());
            }

            @Test
            @DisplayName("VERIFIED-status overdue stages are also flagged NON_COMPLIANT")
            void verifiedStatusOverdueStage_isFlagged() {
                DisbursementStage verified = stageWithDueDateAndCompliance(
                        5L, LocalDate.now().minusDays(2),
                        DisbursementStageStatus.VERIFIED, ComplianceStatus.PENDING);

                given(disbursementStageRepository.findByStatusNotAndDueDateBefore(
                        any(), any())).willReturn(List.of(verified));
                given(disbursementStageService.updateComplianceStatus(any(), any()))
                        .willReturn(verified);

                disbursementReminderScheduler.flagOverdueStagesAsNonCompliant();

                verify(disbursementStageService).updateComplianceStatus(
                        5L, ComplianceStatus.NON_COMPLIANT);
            }

            @Test
            @DisplayName("does not throw when overdue stages are found and flagged")
            void overdueStagesFound_doesNotThrow() {
                DisbursementStage stage = pendingStageWithDueDate(LocalDate.now().minusDays(1));
                given(disbursementStageRepository.findByStatusNotAndDueDateBefore(
                        any(), any())).willReturn(List.of(stage));
                given(disbursementStageService.updateComplianceStatus(any(), any()))
                        .willReturn(stage);

                assertThatCode(() -> disbursementReminderScheduler.flagOverdueStagesAsNonCompliant())
                        .doesNotThrowAnyException();
            }
        }

        @Nested
        @DisplayName("when no overdue stages exist")
        class NoOverdueStages {

            @Test
            @DisplayName("does not call updateComplianceStatus when there are no overdue stages")
            void noOverdue_neverCallsService() {
                given(disbursementStageRepository.findByStatusNotAndDueDateBefore(
                        any(), any())).willReturn(List.of());

                disbursementReminderScheduler.flagOverdueStagesAsNonCompliant();

                verify(disbursementStageService, never()).updateComplianceStatus(any(), any());
            }

            @Test
            @DisplayName("does not throw when no overdue stages exist")
            void noOverdue_doesNotThrow() {
                given(disbursementStageRepository.findByStatusNotAndDueDateBefore(
                        any(), any())).willReturn(List.of());

                assertThatCode(() -> disbursementReminderScheduler.flagOverdueStagesAsNonCompliant())
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("still queries the repository even with empty results")
            void noOverdue_repositoryStillQueried() {
                given(disbursementStageRepository.findByStatusNotAndDueDateBefore(
                        any(), any())).willReturn(List.of());

                disbursementReminderScheduler.flagOverdueStagesAsNonCompliant();

                verify(disbursementStageRepository).findByStatusNotAndDueDateBefore(
                        DisbursementStageStatus.RELEASED, LocalDate.now());
            }
        }
    }
}
