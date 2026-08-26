package com.dsgp.beneficiary.repository;

import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.RegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Beneficiary} entities.
 *
 * <p>Provides CRUD operations inherited from {@link JpaRepository} plus
 * custom finder methods required by the beneficiary service layer.
 */
@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    /**
     * Finds a beneficiary by their unique 12-digit Aadhaar number.
     *
     * @param aadhaarNumber the Aadhaar number to search for
     * @return an {@link Optional} containing the beneficiary if found
     */
    Optional<Beneficiary> findByAadhaarNumber(String aadhaarNumber);

    /**
     * Finds a beneficiary by their registered mobile number.
     *
     * @param mobileNumber the 10-digit mobile number
     * @return an {@link Optional} containing the beneficiary if found
     */
    Optional<Beneficiary> findByMobileNumber(String mobileNumber);

    /**
     * Returns a paginated list of beneficiaries belonging to the given district.
     * Used by District Officers and the regional analytics module.
     *
     * @param district the district name (case-sensitive)
     * @param pageable pagination and sorting parameters
     * @return a page of beneficiaries in the specified district
     */
    Page<Beneficiary> findByDistrict(String district, Pageable pageable);

    /**
     * Returns a paginated list of beneficiaries filtered by registration status.
     *
     * @param status   the registration status to filter by
     * @param pageable pagination and sorting parameters
     * @return a page of beneficiaries with the given status
     */
    Page<Beneficiary> findByRegistrationStatus(RegistrationStatus status, Pageable pageable);

    /**
     * Checks whether a beneficiary with the given Aadhaar number already exists.
     * Used to enforce uniqueness before saving a new registration.
     *
     * @param aadhaarNumber the Aadhaar number to check
     * @return {@code true} if a beneficiary with this Aadhaar exists
     */
    boolean existsByAadhaarNumber(String aadhaarNumber);

    /**
     * Checks whether a beneficiary with the given mobile number already exists.
     *
     * @param mobileNumber the mobile number to check
     * @return {@code true} if a beneficiary with this mobile number exists
     */
    boolean existsByMobileNumber(String mobileNumber);
}
