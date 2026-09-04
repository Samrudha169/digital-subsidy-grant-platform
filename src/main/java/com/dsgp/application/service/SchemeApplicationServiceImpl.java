package com.dsgp.application.service;

import com.dsgp.application.dto.VerificationRequest;
import com.dsgp.application.entity.SchemeApplication;
import com.dsgp.application.repository.SchemeApplicationRepository;
import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.beneficiary.repository.BeneficiaryRepository;
import com.dsgp.beneficiary.repository.SchemeRepository;
import com.dsgp.eligibility.service.EligibilityService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SchemeApplicationServiceImpl implements SchemeApplicationService {

    private final SchemeApplicationRepository repository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final SchemeRepository schemeRepository;
    private final EligibilityService eligibilityService;

    public SchemeApplicationServiceImpl(
            SchemeApplicationRepository repository,
            BeneficiaryRepository beneficiaryRepository,
            SchemeRepository schemeRepository,
            EligibilityService eligibilityService) {

        this.repository = repository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.schemeRepository = schemeRepository;
        this.eligibilityService = eligibilityService;
    }

    @Override
    public SchemeApplication submitApplication(SchemeApplication application) {

        // Find beneficiary
        Beneficiary beneficiary = beneficiaryRepository
                .findById(application.getBeneficiaryId())
                .orElseThrow(() ->
                        new RuntimeException("Beneficiary not found"));

        // Find selected scheme
        Scheme scheme = schemeRepository
                .findById(application.getSchemeId())
                .orElseThrow(() ->
                        new RuntimeException("Scheme not found"));

        // Check eligibility
        boolean eligible = eligibilityService
                .isEligible(beneficiary, scheme);

        if (!eligible) {
            application.setStatus("REJECTED");
            application.setVerificationRemarks(
                    "Beneficiary is not eligible for the selected scheme"
            );
            application.setSubmittedAt(LocalDateTime.now());

            return repository.save(application);
        }

        // Calculate eligibility score
        int eligibilityScore = eligibilityService
                .calculateEligibilityScore(beneficiary, scheme);

        // Determine routing
        String routing = eligibilityService
                .determineRouting(eligibilityScore, scheme);

        application.setStatus("SUBMITTED");
        application.setSubmittedAt(LocalDateTime.now());

        // Set verification level based on routing
        if ("FINAL_AUTHORITY".equals(routing)) {

            application.setVerificationLevel(3);

        } else if ("DISTRICT_OFFICER".equals(routing)
                || "VERIFYING_OFFICER".equals(routing)) {

            application.setVerificationLevel(2);

        } else {

            application.setVerificationLevel(1);
        }

        // Store routing information
        application.setRoutedTo(routing);

        // Store eligibility score
        application.setEligibilityScore(eligibilityScore);

        application.setVerifiedBy(routing);

        application.setVerificationRemarks(
                "Eligibility score: " + eligibilityScore +
                        ". Routed to: " + routing
        );

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

        // If verifier does not approve
        if (!request.isApproved()) {

            application.setStatus("RE_VERIFICATION_REQUIRED");

            application.setVerifiedBy(
                    request.getVerifiedBy()
            );

            application.setVerificationRemarks(
                    request.getRemarks()
            );

            application.setVerifiedAt(
                    LocalDateTime.now()
            );

            return repository.save(application);
        }

        // Level 1 → Level 2
        if (application.getVerificationLevel() == 1) {

            application.setVerificationLevel(2);

            application.setStatus(
                    "UNDER_VERIFICATION"
            );
        }

        // Level 2 → Level 3
        else if (application.getVerificationLevel() == 2) {

            application.setVerificationLevel(3);

            application.setStatus(
                    "UNDER_FINAL_VERIFICATION"
            );
        }

        // Level 3 → Approved
        else if (application.getVerificationLevel() == 3) {

            application.setStatus("APPROVED");
        }

        application.setVerifiedBy(
                request.getVerifiedBy()
        );

        application.setVerificationRemarks(
                request.getRemarks()
        );

        application.setVerifiedAt(
                LocalDateTime.now()
        );

        return repository.save(application);
    }

    @Override
    public SchemeApplication requestReVerification(
            Long applicationId,
            String remarks) {

        SchemeApplication application = repository.findById(applicationId)
                .orElseThrow(() ->
                        new RuntimeException("Application not found"));

        // Put the application back into re-verification
        application.setStatus("RE_VERIFICATION_REQUIRED");

        application.setVerificationRemarks(remarks);

        application.setVerifiedAt(LocalDateTime.now());

        return repository.save(application);
    }
}