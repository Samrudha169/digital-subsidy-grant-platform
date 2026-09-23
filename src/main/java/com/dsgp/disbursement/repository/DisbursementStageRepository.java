package com.dsgp.disbursement.repository;

import com.dsgp.disbursement.entity.DisbursementStage;
import org.springframework.data.jpa.repository.JpaRepository;

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
}