package com.dsgp.disbursement.service;

import com.dsgp.disbursement.entity.ComplianceStatus;
import com.dsgp.disbursement.entity.DisbursementStage;
import com.dsgp.disbursement.entity.DisbursementStageStatus;
import com.dsgp.disbursement.repository.DisbursementStageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Runs daily jobs related to disbursement stage compliance:
 *
 * <ol>
 *   <li>{@link #sendApproachingDueDateReminders()} — logs a reminder for every
 *       non-released stage whose due date falls within the next 3 days.</li>
 *   <li>{@link #flagOverdueStagesAsNonCompliant()} — automatically marks
 *       every non-released stage whose due date is strictly in the past as
 *       {@link ComplianceStatus#NON_COMPLIANT}. Already-flagged stages are
 *       skipped (idempotent).</li>
 * </ol>
 *
 * Email/SMS notifications are NOT sent yet — log output only.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DisbursementReminderScheduler {

    /*
     * How many days ahead to look for approaching due dates.
     */
    private static final int REMINDER_WINDOW_DAYS = 3;

    private final DisbursementStageRepository disbursementStageRepository;
    private final DisbursementStageService    disbursementStageService;

    /**
     * Fires every day at 08:00 server time.
     * Logs a reminder for every non-released stage due within the next 3 days.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendApproachingDueDateReminders() {

        LocalDate today     = LocalDate.now();
        LocalDate windowEnd = today.plusDays(REMINDER_WINDOW_DAYS);

        List<DisbursementStage> approachingStages =
                disbursementStageRepository.findByStatusNotAndDueDateBetween(
                        DisbursementStageStatus.RELEASED,
                        today,
                        windowEnd
                );

        if (approachingStages.isEmpty()) {
            log.info("[DisbursementReminder] No stages approaching due date " +
                     "within the next {} days.", REMINDER_WINDOW_DAYS);
            return;
        }

        log.warn("[DisbursementReminder] {} stage(s) are due within {} days:",
                approachingStages.size(), REMINDER_WINDOW_DAYS);

        for (DisbursementStage stage : approachingStages) {
            log.warn(
                "[DisbursementReminder] COMPLIANCE REMINDER — " +
                "Stage ID: {}, Milestone: '{}', Due Date: {}, Status: {}",
                stage.getId(),
                stage.getMilestone(),
                stage.getDueDate(),
                stage.getStatus()
            );
        }
    }

    /**
     * Fires every day at 08:05 server time (5 minutes after the reminder job).
     *
     * <p>Finds every non-released stage whose due date has already passed
     * and whose compliance status is not already {@code NON_COMPLIANT}, then
     * marks each one {@code NON_COMPLIANT} using the existing
     * {@link DisbursementStageService#updateComplianceStatus} method.
     *
     * <p>The operation is idempotent: re-running it on an already-flagged
     * stage is a no-op because those stages are filtered out by the
     * {@link com.dsgp.disbursement.repository.DisbursementStageRepository#findByStatusNotAndDueDateBefore}
     * query combined with the compliance-status guard below.
     */
    @Scheduled(cron = "0 5 8 * * *")
    public void flagOverdueStagesAsNonCompliant() {

        LocalDate today = LocalDate.now();

        // Fetch all non-released stages whose due date is strictly in the past.
        List<DisbursementStage> overdueStages =
                disbursementStageRepository.findByStatusNotAndDueDateBefore(
                        DisbursementStageStatus.RELEASED,
                        today
                );

        if (overdueStages.isEmpty()) {
            log.info("[DisbursementCompliance] No overdue stages found.");
            return;
        }

        int flagged = 0;

        for (DisbursementStage stage : overdueStages) {

            // Skip stages already marked NON_COMPLIANT (idempotency guard).
            if (stage.getComplianceStatus() == ComplianceStatus.NON_COMPLIANT) {
                continue;
            }

            disbursementStageService.updateComplianceStatus(
                    stage.getId(),
                    ComplianceStatus.NON_COMPLIANT
            );

            log.warn(
                "[DisbursementCompliance] Stage ID: {} marked NON_COMPLIANT. " +
                "Milestone: '{}', Due Date: {}, Stage Status: {}",
                stage.getId(),
                stage.getMilestone(),
                stage.getDueDate(),
                stage.getStatus()
            );

            flagged++;
        }

        log.warn("[DisbursementCompliance] {} overdue stage(s) marked NON_COMPLIANT " +
                 "(out of {} candidate(s) checked).", flagged, overdueStages.size());
    }
}
