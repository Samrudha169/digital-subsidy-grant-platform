package com.dsgp.application.service;

import com.dsgp.application.dto.ApplicationRequest;
import com.dsgp.application.dto.ApplicationResponse;

/**
 * Service interface for scheme application submission.
 *
 * <p>Phase: Milestone 2 — Application Submission.
 *
 * <p>The single operation in this phase is {@link #submitApplication(ApplicationRequest)}.
 * Application management (list, status update) and verification routing are
 * implemented in subsequent phases.
 */
public interface ApplicationService {

    /**
     * Submits a new scheme application for a beneficiary.
     *
     * <p>Business rules enforced:
     * <ol>
     *   <li>Beneficiary with {@code beneficiaryId} must exist.</li>
     *   <li>Scheme with {@code schemeId} must exist.</li>
     *   <li>An eligibility result for {@code (beneficiaryId, schemeId)} must
     *       already exist — i.e. {@code POST /eligibility/check} must have been
     *       called first.</li>
     *   <li>The eligibility status must be {@code ELIGIBLE}
     *       (score ≥ 60). INELIGIBLE results block submission.</li>
     *   <li>A duplicate application for the same
     *       {@code (beneficiaryId, schemeId)} pair is rejected.</li>
     *   <li>The created application is persisted with status {@code PENDING}
     *       and {@code applicationDate} set to the current timestamp.</li>
     * </ol>
     *
     * @param request contains {@code beneficiaryId} and {@code schemeId}
     * @return the persisted application details
     * @throws com.dsgp.beneficiary.exception.BeneficiaryNotFoundException
     *         if the beneficiary does not exist
     * @throws com.dsgp.scheme.exception.SchemeNotFoundException
     *         if the scheme does not exist
     * @throws com.dsgp.application.exception.ApplicationException
     *         if eligibility not yet checked, beneficiary is ineligible,
     *         or a duplicate application exists
     */
    ApplicationResponse submitApplication(ApplicationRequest request);
}
