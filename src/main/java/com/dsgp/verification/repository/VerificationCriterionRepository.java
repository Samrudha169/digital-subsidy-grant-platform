package com.dsgp.verification.repository;

import com.dsgp.verification.entity.VerificationCriterion;
import com.dsgp.verification.entity.VerificationCriterionStatus;
import com.dsgp.verification.entity.VerificationStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VerificationCriterionRepository
        extends JpaRepository<VerificationCriterion, Long> {

    List<VerificationCriterion> findBySchemeApplicationIdOrderByIdAsc(
            Long applicationId);

    List<VerificationCriterion> findBySchemeApplicationIdAndStageOrderByIdAsc(
            Long applicationId,
            VerificationStage stage);

    boolean existsBySchemeApplicationIdAndCriterionCode(
            Long applicationId,
            String criterionCode);

    long countBySchemeApplicationIdAndStageAndStatus(
            Long applicationId,
            VerificationStage stage,
            VerificationCriterionStatus status);

    long countBySchemeApplicationIdAndStage(
            Long applicationId,
            VerificationStage stage);
}