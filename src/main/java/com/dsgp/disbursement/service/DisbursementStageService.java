package com.dsgp.disbursement.service;

import com.dsgp.disbursement.entity.DisbursementPlan;
import com.dsgp.disbursement.entity.DisbursementStage;
import com.dsgp.disbursement.entity.DisbursementStageStatus;
import com.dsgp.disbursement.entity.DisbursementStatus;
import com.dsgp.disbursement.repository.DisbursementPlanRepository;
import com.dsgp.disbursement.repository.DisbursementStageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisbursementStageService {

    private final DisbursementPlanRepository disbursementPlanRepository;
    private final DisbursementStageRepository disbursementStageRepository;

    @Transactional
    public DisbursementStage createStage(
            Long planId,
            Integer stageNumber,
            BigDecimal amount,
            String milestone,
            LocalDate dueDate) {

        DisbursementPlan plan = disbursementPlanRepository.findById(planId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Disbursement plan not found: " + planId));

        if (plan.getDisbursementType().name().equals("SINGLE")) {
            throw new IllegalStateException(
                    "Stages cannot be added to a SINGLE disbursement plan.");
        }

        if (stageNumber == null || stageNumber <= 0) {
            throw new IllegalArgumentException(
                    "Stage number must be greater than zero.");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Stage amount must be greater than zero.");
        }

        if (milestone == null || milestone.isBlank()) {
            throw new IllegalArgumentException(
                    "Milestone cannot be empty.");
        }

        if (disbursementStageRepository
                .findByDisbursementPlanIdAndStageNumber(planId, stageNumber)
                .isPresent()) {

            throw new IllegalStateException(
                    "Stage " + stageNumber +
                            " already exists for this disbursement plan.");
        }

        List<DisbursementStage> existingStages =
                disbursementStageRepository
                        .findByDisbursementPlanIdOrderByStageNumberAsc(planId);

        BigDecimal existingTotal = existingStages.stream()
                .map(DisbursementStage::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal newTotal = existingTotal.add(amount);

        if (newTotal.compareTo(plan.getTotalAmount()) > 0) {
            throw new IllegalArgumentException(
                    "Total stage amount cannot exceed sanctioned amount.");
        }

        return disbursementStageRepository.save(
                DisbursementStage.builder()
                        .disbursementPlan(plan)
                        .stageNumber(stageNumber)
                        .amount(amount)
                        .milestone(milestone)
                        .dueDate(dueDate)
                        .status(DisbursementStageStatus.PENDING)
                        .build()
        );
    }

    @Transactional
    public DisbursementStage verifyStage(Long stageId) {

        DisbursementStage stage =
                disbursementStageRepository.findById(stageId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Disbursement stage not found: " + stageId));

        if (stage.getStatus() == DisbursementStageStatus.RELEASED) {
            throw new IllegalStateException(
                    "Stage has already been released.");
        }

        stage.setStatus(DisbursementStageStatus.VERIFIED);

        return disbursementStageRepository.save(stage);
    }

    @Transactional
    public DisbursementStage releaseStage(Long stageId) {

        DisbursementStage stage =
                disbursementStageRepository.findById(stageId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Disbursement stage not found: " + stageId));

        if (stage.getStatus() != DisbursementStageStatus.VERIFIED) {
            throw new IllegalStateException(
                    "Stage must be VERIFIED before amount can be released.");
        }

        DisbursementPlan plan = stage.getDisbursementPlan();

        BigDecimal newReleased =
                plan.getReleasedAmount().add(stage.getAmount());

        if (newReleased.compareTo(plan.getTotalAmount()) > 0) {
            throw new IllegalStateException(
                    "Released amount cannot exceed sanctioned amount.");
        }

        BigDecimal remaining =
                plan.getTotalAmount().subtract(newReleased);

        plan.setReleasedAmount(newReleased);
        plan.setRemainingAmount(remaining);

        if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            plan.setStatus(DisbursementStatus.FULLY_RELEASED);
        } else {
            plan.setStatus(DisbursementStatus.PARTIALLY_RELEASED);
        }

        stage.setStatus(DisbursementStageStatus.RELEASED);
        stage.setReleasedAt(LocalDateTime.now());

        disbursementPlanRepository.save(plan);

        return disbursementStageRepository.save(stage);
    }

    @Transactional(readOnly = true)
    public List<DisbursementStage> getStages(Long planId) {

        if (!disbursementPlanRepository.existsById(planId)) {
            throw new IllegalArgumentException(
                    "Disbursement plan not found: " + planId);
        }

        return disbursementStageRepository
                .findByDisbursementPlanIdOrderByStageNumberAsc(planId);
    }
}