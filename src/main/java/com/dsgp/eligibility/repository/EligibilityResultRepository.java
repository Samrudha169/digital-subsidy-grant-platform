package com.dsgp.eligibility.repository;

import com.dsgp.eligibility.entity.EligibilityResult;
import com.dsgp.eligibility.entity.EligibilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link EligibilityResult} entities.
 *
 * <p>Provides lookup methods used by the scoring engine and eligibility
 * query endpoints.
 */
@Repository
public interface EligibilityResultRepository extends JpaRepository<EligibilityResult, Long> {

    /**
     * Finds the most recent evaluation for a specific (beneficiary, scheme) pair.
     * Used by the scoring engine to detect re-evaluations.
     */
    Optional<EligibilityResult> findByBeneficiaryIdAndSchemeId(Integer beneficiaryId, Long schemeId);

    /**
     * Returns all evaluation results for a given beneficiary across all schemes.
     * Used by beneficiary-facing "my eligibility" dashboard.
     */
    List<EligibilityResult> findByBeneficiaryId(Integer beneficiaryId);

    /**
     * Returns all results for a given scheme.
     * Used by officers for bulk eligibility analysis.
     */
    List<EligibilityResult> findBySchemeId(Long schemeId);

    /**
     * Returns all ELIGIBLE or INELIGIBLE results for a scheme — used for
     * filtering on the officer dashboard.
     */
    List<EligibilityResult> findBySchemeIdAndEligibilityStatus(
            Long schemeId, EligibilityStatus status);
}
