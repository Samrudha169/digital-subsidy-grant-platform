package com.dsgp.disbursement.service;

import com.dsgp.application.entity.SchemeApplication;
import com.dsgp.disbursement.entity.DisbursementPlan;
import com.dsgp.disbursement.entity.DisbursementType;
import com.dsgp.disbursement.repository.DisbursementPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DisbursementPlanService {

    private final DisbursementPlanRepository disbursementPlanRepository;

    @Transactional
    public DisbursementPlan createPlan(
            SchemeApplication application,
            DisbursementType type) {

        if (application == null) {
            throw new IllegalArgumentException("Application cannot be null.");
        }

        if (!"APPROVED".equals(application.getApplicationStatus())) {
            throw new IllegalStateException(
                    "Disbursement plan can only be created for an approved application."
            );
        }

        if (application.getSanctionedAmount() == null ||
                application.getSanctionedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                    "Sanctioned amount must be greater than zero."
            );
        }

        if (disbursementPlanRepository.existsByApplicationId(application.getId())) {
            throw new IllegalStateException(
                    "A disbursement plan already exists for this application."
            );
        }

        BigDecimal sanctionedAmount = application.getSanctionedAmount();

        DisbursementPlan plan = DisbursementPlan.builder()
                .application(application)
                .totalAmount(sanctionedAmount)
                .releasedAmount(BigDecimal.ZERO)
                .remainingAmount(sanctionedAmount)
                .disbursementType(type)
                .build();

        return disbursementPlanRepository.save(plan);
    }
}