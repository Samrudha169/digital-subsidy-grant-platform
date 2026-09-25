package com.dsgp.disbursement.repository;

import com.dsgp.disbursement.entity.DisbursementStage;
import com.dsgp.disbursement.entity.DisbursementStageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DisbursementStageRepository
        extends JpaRepository<DisbursementStage, Long> {

    List<DisbursementStage> findByDisbursementPlanIdOrderByStageNumberAsc(
            Long disbursementPlanId
    );

    Optional<DisbursementStage> findByDisbursementPlanIdAndStageNumber(
            Long disbursementPlanId,
            Integer stageNumber
    );

    /*
     * Used by the reminder scheduler: find non-RELEASED stages
     * whose due date falls within the approaching window.
     */
    List<DisbursementStage> findByStatusNotAndDueDateBetween(
            DisbursementStageStatus excludedStatus,
            LocalDate from,
            LocalDate to
    );
}