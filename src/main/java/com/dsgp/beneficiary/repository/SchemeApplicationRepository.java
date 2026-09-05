package com.dsgp.beneficiary.repository;

import com.dsgp.application.entity.SchemeApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link SchemeApplication} entities.
 *
 * <p>Placed in the {@code com.dsgp.beneficiary.repository} package alongside
 * {@link SchemeRepository} and {@link BeneficiaryRepository} — the single
 * persistence layer for the {@code scheme_applications} table.
 */
@Repository
public interface SchemeApplicationRepository extends JpaRepository<SchemeApplication, Long> {

    /**
     * Checks whether a beneficiary has already submitted an application for
     * the given scheme. Used to enforce the
     * {@code uk_beneficiary_scheme} unique constraint at the service layer
     * before attempting the INSERT (providing a cleaner error message than
     * a raw {@code DataIntegrityViolationException}).
     *
     * @param beneficiaryId the beneficiary's primary key
     * @param schemeId      the scheme's primary key
     * @return the existing application, or {@link Optional#empty()} if none
     */
    Optional<SchemeApplication> findByBeneficiaryIdAndSchemeId(
            Integer beneficiaryId, Long schemeId);

    /**
     * Returns all applications submitted by a specific beneficiary.
     * Used for the "My Applications" / TrackApplication view.
     *
     * @param beneficiaryId the beneficiary's primary key
     */
    List<SchemeApplication> findByBeneficiaryId(Integer beneficiaryId);

    /**
     * Returns all applications for a given scheme.
     * Used by officer dashboards and scheme-level reporting.
     *
     * @param schemeId the scheme's primary key
     */
    List<SchemeApplication> findBySchemeId(Long schemeId);

    /**
     * Returns all applications with a particular status.
     * Used for officer dashboards (e.g. list all PENDING applications).
     *
     * @param applicationStatus e.g. {@code "PENDING"}, {@code "APPROVED"}
     */
    List<SchemeApplication> findByApplicationStatus(String applicationStatus);
}
