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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

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

    /**
     * Number of days a beneficiary must wait before reapplying to the same
     * scheme after a REJECTED application.
     * Configured via {@code dsgp.application.rejection-cooling-period-days}.
     * Set to 0 to disable the cooling period entirely.
     *
     * <p><strong>This is a project-level business rule, NOT an official
     * government policy.</strong>
     */
    @Value("${dsgp.application.rejection-cooling-period-days:30}")
    private int rejectionCoolingPeriodDays;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy");

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

        // ── Rule 5: No duplicate active application; cooling period for REJECTED ─
        Optional<SchemeApplication> existingOpt =
                applicationRepository.findByBeneficiaryIdAndSchemeId(beneficiaryId, schemeId);

        if (existingOpt.isPresent()) {
            SchemeApplication existing = existingOpt.get();

            if (!"REJECTED".equals(existing.getApplicationStatus())) {
                // Active (non-rejected) application already exists — hard block.
                log.warn("Duplicate application attempt: beneficiaryId={}, schemeId={}, status={}",
                        beneficiaryId, schemeId, existing.getApplicationStatus());
                throw new ApplicationException(
                        "An application for scheme '" + scheme.getSchemeName()
                        + "' already exists for this beneficiary.");
            }

            // Previous application was REJECTED — apply cooling period.
            if (rejectionCoolingPeriodDays > 0 && existing.getApplicationDate() != null) {
                LocalDateTime rejectedAt   = existing.getApplicationDate();
                LocalDateTime reapplyAfter = rejectedAt.plusDays(rejectionCoolingPeriodDays);

                if (LocalDateTime.now().isBefore(reapplyAfter)) {
                    String reapplyDateStr = reapplyAfter.toLocalDate().format(DATE_FMT);
                    log.warn(
                            "Cooling period active: beneficiaryId={}, schemeId={}, reapplyAfter={}",
                            beneficiaryId, schemeId, reapplyDateStr);
                    throw new ApplicationException(
                            "Your previous application for scheme '" + scheme.getSchemeName()
                            + "' was rejected. You may reapply after " + reapplyDateStr + ".");
                }

                // Cooling period has expired — allow the re-application.
                log.info(
                        "Cooling period expired for beneficiaryId={}, schemeId={}: allowing reapplication.",
                        beneficiaryId, schemeId);
            }
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
                .sanctionedAmount(app.getSanctionedAmount())
                .build();
    }


    // ── List operations ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByBeneficiary(Integer beneficiaryId) {
        return applicationRepository.findByBeneficiaryId(beneficiaryId)
                .stream()
                .map(app -> {
                    int score = eligibilityResultRepository
                            .findByBeneficiaryIdAndSchemeId(app.getBeneficiary().getId(), app.getScheme().getId())
                            .map(EligibilityResult::getTotalScore)
                            .orElse(0);
                    return mapToResponse(app, score);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByStatus(String status) {
        return applicationRepository.findByApplicationStatus(status)
                .stream()
                .map(app -> {
                    int score = eligibilityResultRepository
                            .findByBeneficiaryIdAndSchemeId(app.getBeneficiary().getId(), app.getScheme().getId())
                            .map(EligibilityResult::getTotalScore)
                            .orElse(0);
                    return mapToResponse(app, score);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getAllApplications() {
        return applicationRepository.findAll()
                .stream()
                .map(app -> {
                    int score = eligibilityResultRepository
                            .findByBeneficiaryIdAndSchemeId(app.getBeneficiary().getId(), app.getScheme().getId())
                            .map(EligibilityResult::getTotalScore)
                            .orElse(0);
                    return mapToResponse(app, score);
                })
                .toList();
    }
}
