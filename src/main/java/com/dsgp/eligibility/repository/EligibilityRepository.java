package com.dsgp.eligibility.repository;

import com.dsgp.eligibility.entity.Eligibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EligibilityRepository extends JpaRepository<Eligibility, Long> {

    Optional<Eligibility> findByBeneficiaryIdAndSchemeId(
            Integer beneficiaryId,
            Long schemeId
    );
}