package com.dsgp.budget.repository;

import com.dsgp.budget.entity.BudgetAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetAllocationRepository extends JpaRepository<BudgetAllocation, Long> {

    List<BudgetAllocation> findByActiveTrue();

    List<BudgetAllocation> findAllByOrderByIdDesc();

    Optional<BudgetAllocation> findBySchemeIdAndRegionIgnoreCase(
            Long schemeId,
            String region
    );

    boolean existsBySchemeIdAndRegionIgnoreCase(
            Long schemeId,
            String region
    );

    boolean existsBySchemeIdAndRegionIgnoreCaseAndIdNot(
            Long schemeId,
            String region,
            Long id
    );
}