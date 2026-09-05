package com.dsgp.application.service;

import com.dsgp.application.dto.VerificationRequest;
import com.dsgp.application.entity.SchemeApplication;
import com.dsgp.beneficiary.repository.SchemeApplicationRepository;
import org.springframework.stereotype.Service;

/**
 * Lightweight read/utility service for SchemeApplication.
 *
 * <p>The authoritative application SUBMISSION workflow is handled by
 * {@link ApplicationServiceImpl} (eligibility gate, duplicate check, etc.).
 * The authoritative VERIFICATION workflow is handled by
 * {@link com.dsgp.verification.service.VerificationServiceImpl}.
 *
 * <p>This class exists to provide {@link #getApplicationById} for
 * use-cases that need a simple lookup without the full submission logic
 * (e.g. Samrudha's admin read endpoints). The {@link #verifyApplication}
 * method is intentionally a no-op stub — all verification must go through
 * the VerificationController / VerificationServiceImpl state machine.
 */
@Service
public class SchemeApplicationServiceImpl implements SchemeApplicationService {

    private final SchemeApplicationRepository repository;

    public SchemeApplicationServiceImpl(SchemeApplicationRepository repository) {
        this.repository = repository;
    }

    /**
     * Not used — application submission is handled exclusively by
     * {@link ApplicationServiceImpl#submitApplication}.
     * Throws {@link UnsupportedOperationException} to prevent misuse.
     */
    @Override
    public SchemeApplication submitApplication(SchemeApplication application) {
        throw new UnsupportedOperationException(
                "Use POST /api/v1/applications via ApplicationServiceImpl. " +
                "SchemeApplicationServiceImpl.submitApplication is not active.");
    }

    @Override
    public SchemeApplication getApplicationById(Long applicationId) {
        return repository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found with ID: " + applicationId));
    }

    /**
     * Not used — verification state machine is handled exclusively by
     * {@link com.dsgp.verification.service.VerificationServiceImpl}.
     * Throws {@link UnsupportedOperationException} to prevent misuse.
     */
    @Override
    public SchemeApplication verifyApplication(
            Long applicationId,
            VerificationRequest request) {
        throw new UnsupportedOperationException(
                "Use POST /api/v1/verification/applications/{id}/... via VerificationServiceImpl. " +
                "SchemeApplicationServiceImpl.verifyApplication is not active.");
    }
}