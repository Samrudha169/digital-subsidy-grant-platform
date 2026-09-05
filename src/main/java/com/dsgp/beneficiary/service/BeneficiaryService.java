package com.dsgp.beneficiary.service;

import com.dsgp.beneficiary.dto.BeneficiaryRegistrationRequest;
import com.dsgp.beneficiary.dto.BeneficiaryResponse;
import com.dsgp.beneficiary.dto.BeneficiaryUpdateRequest;

import java.util.List;

/**
 * Service contract for beneficiary management operations.
 *
 * <p>Two update overloads are provided:
 * <ul>
 *   <li>{@link #updateBeneficiary(Integer, BeneficiaryRegistrationRequest)} —
 *       retained for Milestone 1 backward compatibility (all legacy fields).</li>
 *   <li>{@link #updateBeneficiary(Integer, BeneficiaryUpdateRequest)} —
 *       preferred Milestone 2 path (optional patch-style update with extended
 *       eligibility fields).</li>
 * </ul>
 */
public interface BeneficiaryService {

    BeneficiaryResponse registerBeneficiary(
            BeneficiaryRegistrationRequest request);

    BeneficiaryResponse getBeneficiaryById(Integer id);

    BeneficiaryResponse getBeneficiaryByGovId(String govId);

    List<BeneficiaryResponse> getAllBeneficiaries();

    /** Milestone 1 update path — kept for backward compatibility. */
    BeneficiaryResponse updateBeneficiary(
            Integer id,
            BeneficiaryRegistrationRequest request);

    /**
     * Milestone 2 update path — patch-style; only non-null fields are applied.
     * Used by {@code PUT /beneficiaries/{id}} from Milestone 2 onward.
     */
    BeneficiaryResponse updateBeneficiary(
            Integer id,
            BeneficiaryUpdateRequest request);

    void deleteBeneficiary(Integer id);
}