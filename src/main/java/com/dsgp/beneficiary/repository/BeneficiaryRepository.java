package com.dsgp.beneficiary.repository;

import com.dsgp.beneficiary.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Beneficiary} entities.
 *
 * <p>Provides query methods for the primary business keys used in
 * duplicate-detection and lookup operations.
 *
 * <p>Milestone 1 methods (findByGovId, existsByGovId, existsByContact)
 * are preserved unchanged.
 *
 * <p>Milestone 2 additions: existsByAadhaarNumber, existsByMobileNumber,
 * findByAadhaarNumber — support the extended identity model.
 */
@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Integer> {

    // ── Milestone 1 queries (preserved) ─────────────────────────────────────

    Optional<Beneficiary> findByGovId(String govId);

    Optional<Beneficiary> findByContact(String contact);

    boolean existsByGovId(String govId);

    boolean existsByContact(String contact);

    // ── Milestone 2 queries ──────────────────────────────────────────────────

    /**
     * Checks whether a beneficiary with the given 12-digit Aadhaar number
     * already exists. Used for duplicate detection during registration.
     */
    boolean existsByAadhaarNumber(String aadhaarNumber);

    /**
     * Checks whether a beneficiary with the given mobile number already exists.
     * Used for duplicate detection during registration and update.
     */
    boolean existsByMobileNumber(String mobileNumber);

    /**
     * Looks up a beneficiary by their canonical 12-digit Aadhaar number.
     */
    Optional<Beneficiary> findByAadhaarNumber(String aadhaarNumber);
}