package com.dsgp.disbursement.repository;

import com.dsgp.disbursement.entity.DisbursementPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DisbursementPlanRepository
        extends JpaRepository<DisbursementPlan, Long> {

    Optional<DisbursementPlan> findByApplication_Id(Long applicationId);

    boolean existsByApplication_Id(Long applicationId);
}