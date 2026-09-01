package com.dsgp.application.service;

import com.dsgp.application.dto.VerificationRequest;
import com.dsgp.application.entity.SchemeApplication;
import com.dsgp.application.repository.SchemeApplicationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SchemeApplicationServiceImpl implements SchemeApplicationService {

    private final SchemeApplicationRepository repository;

    public SchemeApplicationServiceImpl(SchemeApplicationRepository repository) {
        this.repository = repository;
    }

    @Override
    public SchemeApplication submitApplication(SchemeApplication application) {

        application.setStatus("SUBMITTED");
        application.setSubmittedAt(LocalDateTime.now());

        // Start verification workflow at Level 1
        application.setVerificationLevel(1);

        return repository.save(application);
    }

    @Override
    public SchemeApplication getApplicationById(Long applicationId) {

        return repository.findById(applicationId)
                .orElseThrow(() ->
                        new RuntimeException("Application not found"));
    }

    @Override
    public SchemeApplication verifyApplication(
            Long applicationId,
            VerificationRequest request) {

        SchemeApplication application = repository.findById(applicationId)
                .orElseThrow(() ->
                        new RuntimeException("Application not found"));

        // If verifier rejects the application
        if (!request.isApproved()) {

            application.setStatus("REJECTED");
            application.setVerifiedBy(request.getVerifiedBy());
            application.setVerificationRemarks(request.getRemarks());
            application.setVerifiedAt(LocalDateTime.now());

            return repository.save(application);
        }

        // Level 1 → Level 2
        if (application.getVerificationLevel() == 1) {

            application.setVerificationLevel(2);
            application.setStatus("UNDER_VERIFICATION");
        }

        // Level 2 → Level 3
        else if (application.getVerificationLevel() == 2) {

            application.setVerificationLevel(3);
            application.setStatus("UNDER_FINAL_VERIFICATION");
        }

        // Level 3 → Approved
        else if (application.getVerificationLevel() == 3) {

            application.setStatus("APPROVED");
        }

        application.setVerifiedBy(request.getVerifiedBy());
        application.setVerificationRemarks(request.getRemarks());
        application.setVerifiedAt(LocalDateTime.now());

        return repository.save(application);
    }
}