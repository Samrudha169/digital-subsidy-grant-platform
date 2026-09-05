package com.dsgp.application.service;

import com.dsgp.application.dto.ApplicationRequest;
import com.dsgp.application.dto.ApplicationResponse;
import com.dsgp.application.exception.ApplicationException;
import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.application.entity.SchemeApplication;
import com.dsgp.beneficiary.exception.BeneficiaryNotFoundException;
import com.dsgp.beneficiary.repository.BeneficiaryRepository;
import com.dsgp.beneficiary.repository.SchemeApplicationRepository;
import com.dsgp.beneficiary.repository.SchemeRepository;
import com.dsgp.eligibility.entity.EligibilityResult;
import com.dsgp.eligibility.entity.EligibilityStatus;
import com.dsgp.eligibility.repository.EligibilityResultRepository;
import com.dsgp.scheme.exception.SchemeNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic implementation for scheme application submission.
 *
 * <p>Enforces all five eligibility-gate and duplicate-prevention rules
 * documented in {@link ApplicationService} before persisting the
 * {@link SchemeApplication} record.
 *
 * <p>Intentionally does NOT call the eligibility scoring engine — it reads
 * the already-persisted {@link EligibilityResult} so the engine is not
 * re-invoked on every submission attempt. The engine was designed as an
 * idempotent "run once and store" operation (see {@code eligibility-scoring.md}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

    private final BeneficiaryRepository        beneficiaryRepository;
    private final SchemeRepository              schemeRepository;
    private final EligibilityResultRepository   eligibilityResultRepository;
    private final SchemeApplicationRepository   applicationRepository;

    // ════════════════════════════════════════════════════════════════════════
    // submitApplication
    // ════════════════════════════════════════════════════════════════════════

    @Override
    public ApplicationResponse submitApplication(ApplicationRequest request) {

        Integer beneficiaryId = request.getBeneficiaryId();
        Long    schemeId      = request.getSchemeId();

        log.debug("Application submission requested: beneficiaryId={}, schemeId={}",
                beneficiaryId, schemeId);

        // ── Rule 1: Beneficiary must exist ───────────────────────────────────
        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() ->
                        new BeneficiaryNotFoundException(
                                "Beneficiary not found with ID: " + beneficiaryId));

        // ── Rule 2: Scheme must exist ─────────────────────────────────────────
        Scheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new SchemeNotFoundException(schemeId));

        // ── Rule 3 & 4: Eligibility must have been checked and must be ELIGIBLE ─
        EligibilityResult eligibilityResult = eligibilityResultRepository
                .findByBeneficiaryIdAndSchemeId(beneficiaryId, schemeId)
                .orElseThrow(() -> new ApplicationException(
                        "Eligibility has not been checked for beneficiary ID " + beneficiaryId
                        + " and scheme '" + scheme.getSchemeName()
                        + "'. Please run POST /api/v1/eligibility/check first."));

        if (eligibilityResult.getEligibilityStatus() == EligibilityStatus.INELIGIBLE) {
            log.warn("Application blocked — beneficiary {} is INELIGIBLE for scheme {} (score={})",
                    beneficiaryId, schemeId, eligibilityResult.getTotalScore());
            throw new ApplicationException(
                    "Beneficiary is not eligible for scheme '" + scheme.getSchemeName()
                    + "'. Eligibility score: " + eligibilityResult.getTotalScore()
                    + "/100 (minimum required: 60).");
        }

        // ── Rule 5: No duplicate application ─────────────────────────────────
        if (applicationRepository.findByBeneficiaryIdAndSchemeId(beneficiaryId, schemeId)
                .isPresent()) {
            log.warn("Duplicate application attempt: beneficiaryId={}, schemeId={}",
                    beneficiaryId, schemeId);
            throw new ApplicationException(
                    "An application for scheme '" + scheme.getSchemeName()
                    + "' already exists for this beneficiary.");
        }

        // ── Rule 6: Persist with PENDING status ───────────────────────────────
        SchemeApplication application = SchemeApplication.builder()
                .beneficiary(beneficiary)
                .scheme(scheme)
                .applicationStatus("PENDING")
                .build();
        // applicationDate is set by @PrePersist on SchemeApplication

        SchemeApplication saved = applicationRepository.save(application);

        log.info("Application submitted: id={}, beneficiaryId={}, schemeId={}, status={}",
                saved.getId(), beneficiaryId, schemeId, saved.getApplicationStatus());

        return mapToResponse(saved, eligibilityResult.getTotalScore());
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private ApplicationResponse mapToResponse(SchemeApplication app, int eligibilityScore) {
        return ApplicationResponse.builder()
                .applicationId(app.getId())
                .beneficiaryId(app.getBeneficiary().getId())
                .beneficiaryName(app.getBeneficiary().getFullName())
                .schemeId(app.getScheme().getId())
                .schemeName(app.getScheme().getSchemeName())
                .applicationStatus(app.getApplicationStatus())
                .eligibilityScore(eligibilityScore)
                .applicationDate(app.getApplicationDate())
                .build();
    }
}
