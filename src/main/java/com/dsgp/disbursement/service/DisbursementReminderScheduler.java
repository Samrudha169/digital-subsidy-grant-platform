package com.dsgp.disbursement.service;

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
 * Runs once daily and logs a compliance reminder for every
 * disbursement stage whose due date falls within the next 3 days
 * and has not yet been released.
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

    /**
     * Fires every day at 08:00 server time.
     * Cron expression: second minute hour day month weekday
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendApproachingDueDateReminders() {

        LocalDate today = LocalDate.now();
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
}
