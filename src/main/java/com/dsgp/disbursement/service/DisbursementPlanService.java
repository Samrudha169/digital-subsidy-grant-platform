package com.dsgp.disbursement.service;

import com.dsgp.application.entity.SchemeApplication;
import com.dsgp.disbursement.entity.DisbursementPlan;
import com.dsgp.disbursement.entity.DisbursementType;
import com.dsgp.disbursement.repository.DisbursementPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
                application.getSanctionedAmount();

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
         * Automatically create all stages
         * for a STAGED disbursement plan.
         */
        if (type == DisbursementType.STAGED) {

            // Stage 1 - 40%
            disbursementStageService.createStage(
                    savedPlan.getId(),
                    1,
                    sanctionedAmount.multiply(
                            new BigDecimal("0.40")
                    ),
                    "Initial verification and setup",
                    LocalDate.now().plusDays(30)
            );

            // Stage 2 - 35%
            disbursementStageService.createStage(
                    savedPlan.getId(),
                    2,
                    sanctionedAmount.multiply(
                            new BigDecimal("0.35")
                    ),
                    "Purchase of approved equipment",
                    LocalDate.now().plusDays(60)
            );

            // Stage 3 - 25%
            disbursementStageService.createStage(
                    savedPlan.getId(),
                    3,
                    sanctionedAmount.multiply(
                            new BigDecimal("0.25")
                    ),
                    "Final completion and verification of previous fund utilization",
                    LocalDate.now().plusDays(90)
            );
        }

        return savedPlan;
    }
}