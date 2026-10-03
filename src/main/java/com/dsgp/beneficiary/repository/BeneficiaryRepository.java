package com.dsgp.beneficiary.repository;

import com.dsgp.beneficiary.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Integer> {

    // Milestone 1
    Optional<Beneficiary> findByGovId(String govId);

    Optional<Beneficiary> findByContact(String contact);

    boolean existsByGovId(String govId);

    boolean existsByContact(String contact);

    // Milestone 2
    boolean existsByAadhaarNumber(String aadhaarNumber);

    boolean existsByMobileNumber(String mobileNumber);

    Optional<Beneficiary> findByAadhaarNumber(String aadhaarNumber);

    // Authentication — exact match (throws NonUniqueResultException if duplicates exist)
    Optional<Beneficiary> findByEmail(String email);

    // Email uniqueness guard — added for OTP / registration duplicate-email check
    boolean existsByEmail(String email);

    /**
     * Returns the first beneficiary row matching the given email address.
     * Used wherever the caller must not crash if legacy duplicate rows exist.
     * Prefer this over {@link #findByEmail} in login / OTP lookup paths.
     */
    Optional<Beneficiary> findFirstByEmail(String email);
}