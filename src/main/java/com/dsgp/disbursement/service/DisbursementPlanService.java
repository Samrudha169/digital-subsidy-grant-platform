package com.dsgp.disbursement.service;

import com.dsgp.application.entity.SchemeApplication;
import com.dsgp.disbursement.entity.DisbursementPlan;
import com.dsgp.disbursement.entity.DisbursementType;
import com.dsgp.disbursement.repository.DisbursementPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DisbursementPlanService {

    private final DisbursementPlanRepository disbursementPlanRepository;
    private final DisbursementStageService disbursementStageService;

    @Transactional
    public DisbursementPlan createPlan(
            SchemeApplication application,
            DisbursementType type) {

        if (application == null) {
            throw new IllegalArgumentException(
                    "Application cannot be null.");
        }

        if (!"APPROVED".equals(application.getApplicationStatus())) {
            throw new IllegalStateException(
                    "Disbursement plan can only be created for an approved application."
            );
        }

        if (application.getSanctionedAmount() == null ||
                application.getSanctionedAmount()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalStateException(
                    "Sanctioned amount must be greater than zero."
            );
        }

        if (disbursementPlanRepository
                .existsByApplication_Id(application.getId())) {

            throw new IllegalStateException(
                    "A disbursement plan already exists for this application."
            );
        }

        BigDecimal sanctionedAmount =
                application.getSanctionedAmount()
                        .setScale(2, RoundingMode.HALF_UP);

        DisbursementPlan plan = DisbursementPlan.builder()
                .application(application)
                .totalAmount(sanctionedAmount)
                .releasedAmount(BigDecimal.ZERO)
                .remainingAmount(sanctionedAmount)
                .disbursementType(type)
                .build();

        /*
         * First save the disbursement plan.
         */
        DisbursementPlan savedPlan =
                disbursementPlanRepository.save(plan);

        /*
         * Automatically create three equal stages
         * for a STAGED disbursement plan.
         */
        if (type == DisbursementType.STAGED) {

            /*
             * Divide the sanctioned amount equally into
             * three stages.
             *
             * The first two stages are rounded to 2 decimal
             * places. Stage 3 receives the exact remaining
             * amount so that the total always equals the
             * sanctioned amount.
             */
            BigDecimal stageAmount =
                    sanctionedAmount
                            .divide(
                                    new BigDecimal("3"),
                                    2,
                                    RoundingMode.HALF_UP
                            );

            BigDecimal stageOneAmount = stageAmount;

            BigDecimal stageTwoAmount = stageAmount;

            BigDecimal stageThreeAmount =
                    sanctionedAmount
                            .subtract(stageOneAmount)
                            .subtract(stageTwoAmount);

            LocalDate today = LocalDate.now();

            // Stage 1 - Equal third
            disbursementStageService.createStage(
                    savedPlan.getId(),
                    1,
                    stageOneAmount,
                    "Initial verification and setup",
                    today.plusDays(30)
            );

            // Stage 2 - Equal third
            disbursementStageService.createStage(
                    savedPlan.getId(),
                    2,
                    stageTwoAmount,
                    "Purchase of approved equipment",
                    today.plusDays(60)
            );

            // Stage 3 - Remaining amount after rounding
            disbursementStageService.createStage(
                    savedPlan.getId(),
                    3,
                    stageThreeAmount,
                    "Final completion and verification of previous fund utilization",
                    today.plusDays(90)
            );
        }

        return savedPlan;
    }
}