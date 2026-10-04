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
                    "Application cannot be null."
            );
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
         * Automatically create three stages
         * for a STAGED disbursement plan.
         *
         * Stage 1 = 40%
         * Stage 2 = 35%
         * Stage 3 = 25%
         *
         * The final stage receives the remaining amount
         * after rounding so that all three stages always
         * add up exactly to the sanctioned amount.
         */
        if (type == DisbursementType.STAGED) {

            BigDecimal stageOneAmount =
                    sanctionedAmount
                            .multiply(new BigDecimal("0.40"))
                            .setScale(2, RoundingMode.HALF_UP);

            BigDecimal stageTwoAmount =
                    sanctionedAmount
                            .multiply(new BigDecimal("0.35"))
                            .setScale(2, RoundingMode.HALF_UP);

            BigDecimal stageThreeAmount =
                    sanctionedAmount
                            .subtract(stageOneAmount)
                            .subtract(stageTwoAmount)
                            .setScale(2, RoundingMode.HALF_UP);

            LocalDate today = LocalDate.now();

            /*
             * Stage 1 - 40%
             */
            disbursementStageService.createStage(
                    savedPlan.getId(),
                    1,
                    stageOneAmount,
                    "Initial verification and setup",
                    today.plusDays(30)
            );

            /*
             * Stage 2 - 35%
             */
            disbursementStageService.createStage(
                    savedPlan.getId(),
                    2,
                    stageTwoAmount,
                    "Purchase of approved equipment",
                    today.plusDays(60)
            );

            /*
             * Stage 3 - 25%
             */
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