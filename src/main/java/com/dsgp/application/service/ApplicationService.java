package com.dsgp.application.service;

import com.dsgp.application.dto.ApplicationRequest;
import com.dsgp.application.dto.ApplicationResponse;

import java.util.List;

/**
 * Service interface for scheme application submission and retrieval.
 *
 * <p>Phase: Milestone 2 — Application Submission.
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
     */
    ApplicationResponse submitApplication(ApplicationRequest request);

    /**
     * Returns all applications submitted by a specific beneficiary.
     * Used for the "My Applications" dashboard section.
     *
     * @param beneficiaryId the beneficiary's primary key
     * @return list of all applications for this beneficiary
     */
    List<ApplicationResponse> getApplicationsByBeneficiary(Integer beneficiaryId);

    /**
     * Returns all applications with a given workflow status.
     * Used for officer dashboards (queue by status).
     *
     * @param status e.g. "PENDING", "UNDER_REVIEW", "CORRECTION_REQUIRED"
     * @return list of matching applications
     */
    List<ApplicationResponse> getApplicationsByStatus(String status);

    /**
     * Returns all applications (for administrator overview).
     */
    List<ApplicationResponse> getAllApplications();
}

