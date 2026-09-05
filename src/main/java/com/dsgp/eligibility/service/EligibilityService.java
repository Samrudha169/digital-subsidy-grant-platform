package com.dsgp.eligibility.service;

import com.dsgp.eligibility.dto.EligibilityCheckRequest;
import com.dsgp.eligibility.dto.EligibilityResultResponse;

import java.util.List;

/**
 * Service contract for eligibility evaluation operations.
 *
 * <p>The primary operation is {@link #checkEligibility}, which evaluates a
 * beneficiary against a scheme's rules using the weighted scoring engine
 * and persists the result. All other methods are read-only lookups.
 */
public interface EligibilityService {

    /**
     * Evaluates a beneficiary's eligibility for a given scheme.
     *
     * <p>If a previous result exists for the same (beneficiary, scheme) pair,
     * it is replaced by the new evaluation. The result is persisted and returned.
     *
     * @param request the check request containing beneficiaryId and schemeId
     * @return the full eligibility result with per-criterion breakdown
     * @throws com.dsgp.eligibility.exception.EligibilityCheckException
     *         if the beneficiary or scheme is not found
     */
    EligibilityResultResponse checkEligibility(EligibilityCheckRequest request);

    /**
     * Returns the most recent eligibility result for a specific
     * (beneficiary, scheme) pair.
     *
     * @throws com.dsgp.eligibility.exception.EligibilityCheckException if not found
     */
    EligibilityResultResponse getResult(Integer beneficiaryId, Long schemeId);

    /**
     * Returns all eligibility results for a given beneficiary across all schemes.
     */
    List<EligibilityResultResponse> getResultsForBeneficiary(Integer beneficiaryId);

    /**
     * Returns all eligibility results for a given scheme.
     */
    List<EligibilityResultResponse> getResultsForScheme(Long schemeId);
}
