package com.dsgp.eligibility.service;

import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.beneficiary.repository.BeneficiaryRepository;
import com.dsgp.beneficiary.repository.SchemeRepository;
import com.dsgp.eligibility.dto.CriterionResult;
import com.dsgp.eligibility.dto.EligibilityCheckRequest;
import com.dsgp.eligibility.dto.EligibilityResultResponse;
import com.dsgp.eligibility.entity.EligibilityResult;
import com.dsgp.eligibility.entity.EligibilityStatus;
import com.dsgp.eligibility.exception.EligibilityCheckException;
import com.dsgp.eligibility.repository.EligibilityResultRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Core eligibility scoring engine.
 *
 * <p>Implements the weighted 100-point scoring system defined in
 * {@code docs/architecture/eligibility-scoring.md}.
 *
 * <h2>Scoring weights</h2>
 * <table>
 *   <tr><th>Criterion</th>      <th>Max Points</th></tr>
 *   <tr><td>Age Range</td>      <td>20</td></tr>
 *   <tr><td>Annual Income</td>  <td>30</td></tr>
 *   <tr><td>Land Holding</td>   <td>20</td></tr>
 *   <tr><td>Category Match</td> <td>20</td></tr>
 *   <tr><td>Identity Verified</td><td>10</td></tr>
 *   <tr><th>Total</th>          <th>100</th></tr>
 * </table>
 *
 * <h2>Pass threshold</h2>
 * <p>Score ≥ 60 → ELIGIBLE; score < 60 → INELIGIBLE.
 *
 * <h2>Null-threshold rule</h2>
 * <p>If a scheme does not define a threshold for a criterion (the field is
 * {@code null}), that criterion is automatically satisfied and full points
 * are awarded.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EligibilityScoringEngine implements EligibilityService {

    // ── Scoring weights (per spec) ───────────────────────────────────────────
    static final int POINTS_AGE      = 20;
    static final int POINTS_INCOME   = 30;
    static final int POINTS_LAND     = 20;
    static final int POINTS_CATEGORY = 20;
    static final int POINTS_IDENTITY = 10;

    /** Score threshold: ≥ this value → ELIGIBLE. */
    static final int ELIGIBILITY_THRESHOLD = 60;

    // ── Dependencies ─────────────────────────────────────────────────────────
    private final BeneficiaryRepository beneficiaryRepository;
    private final SchemeRepository      schemeRepository;
    private final EligibilityResultRepository resultRepository;
    private final ObjectMapper objectMapper;

    // ════════════════════════════════════════════════════════════════════════
    // checkEligibility — primary operation
    // ════════════════════════════════════════════════════════════════════════

    @Override
    public EligibilityResultResponse checkEligibility(EligibilityCheckRequest request) {
        Beneficiary beneficiary = beneficiaryRepository.findById(request.getBeneficiaryId())
                .orElseThrow(() -> new EligibilityCheckException(
                        "Beneficiary not found with ID: " + request.getBeneficiaryId()));

        Scheme scheme = schemeRepository.findById(request.getSchemeId())
                .orElseThrow(() -> new EligibilityCheckException(
                        "Scheme not found with ID: " + request.getSchemeId()));

        log.info("Evaluating eligibility: beneficiary={} scheme={}",
                beneficiary.getId(), scheme.getId());

        // ── Run scoring engine ───────────────────────────────────────────────
        Map<String, CriterionResult> criteria = new LinkedHashMap<>();
        int totalScore = 0;

        CriterionResult ageResult      = evaluateAge(beneficiary, scheme);
        CriterionResult incomeResult   = evaluateIncome(beneficiary, scheme);
        CriterionResult landResult     = evaluateLand(beneficiary, scheme);
        CriterionResult categoryResult = evaluateCategory(beneficiary, scheme);
        CriterionResult identityResult = evaluateIdentity(beneficiary);

        criteria.put("ageCheck",      ageResult);
        criteria.put("incomeCheck",   incomeResult);
        criteria.put("landCheck",     landResult);
        criteria.put("categoryCheck", categoryResult);
        criteria.put("identityCheck", identityResult);

        totalScore += ageResult.getPoints()
                   + incomeResult.getPoints()
                   + landResult.getPoints()
                   + categoryResult.getPoints()
                   + identityResult.getPoints();

        EligibilityStatus status = totalScore >= ELIGIBILITY_THRESHOLD
                ? EligibilityStatus.ELIGIBLE
                : EligibilityStatus.INELIGIBLE;

        log.info("Eligibility result: beneficiary={} scheme={} score={} status={}",
                beneficiary.getId(), scheme.getId(), totalScore, status);

        // ── Persist result (replace previous if exists) ──────────────────────
        EligibilityResult entity = resultRepository
                .findByBeneficiaryIdAndSchemeId(beneficiary.getId(), scheme.getId())
                .orElseGet(EligibilityResult::new);

        entity.setBeneficiaryId(beneficiary.getId());
        entity.setSchemeId(scheme.getId());
        entity.setSchemeName(scheme.getSchemeName());
        entity.setTotalScore(totalScore);
        entity.setEligibilityStatus(status);
        entity.setCriteriaJson(serialiseCriteria(criteria));

        EligibilityResult saved = resultRepository.save(entity);

        // ── Build response ───────────────────────────────────────────────────
        return toResponse(saved, criteria);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Read-only lookups
    // ════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public EligibilityResultResponse getResult(Integer beneficiaryId, Long schemeId) {
        EligibilityResult result = resultRepository
                .findByBeneficiaryIdAndSchemeId(beneficiaryId, schemeId)
                .orElseThrow(() -> new EligibilityCheckException(
                        "No eligibility result found for beneficiary " + beneficiaryId
                        + " and scheme " + schemeId));
        return toResponseFromEntity(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EligibilityResultResponse> getResultsForBeneficiary(Integer beneficiaryId) {
        return resultRepository.findByBeneficiaryId(beneficiaryId)
                .stream()
                .map(this::toResponseFromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EligibilityResultResponse> getResultsForScheme(Long schemeId) {
        return resultRepository.findBySchemeId(schemeId)
                .stream()
                .map(this::toResponseFromEntity)
                .toList();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Per-criterion evaluators — package-private for unit testing
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Age Range criterion (20 points).
     * Null minAge/maxAge → criterion auto-satisfied.
     */
    CriterionResult evaluateAge(Beneficiary b, Scheme s) {
        if (s.getMinAge() == null && s.getMaxAge() == null) {
            return CriterionResult.builder()
                    .points(POINTS_AGE)
                    .passed(true)
                    .detail("No age restriction defined for this scheme")
                    .build();
        }

        Integer age = b.getAge();
        if (age == null) {
            return CriterionResult.builder()
                    .points(0)
                    .passed(false)
                    .detail("Beneficiary age not recorded")
                    .build();
        }

        boolean minOk = (s.getMinAge() == null) || (age >= s.getMinAge());
        boolean maxOk = (s.getMaxAge() == null) || (age <= s.getMaxAge());

        if (minOk && maxOk) {
            return CriterionResult.builder()
                    .points(POINTS_AGE)
                    .passed(true)
                    .detail(String.format("Age %d within range [%s, %s]",
                            age,
                            s.getMinAge() != null ? s.getMinAge() : "no min",
                            s.getMaxAge() != null ? s.getMaxAge() : "no max"))
                    .build();
        }

        return CriterionResult.builder()
                .points(0)
                .passed(false)
                .detail(String.format("Age %d outside required range [%s, %s]",
                        age,
                        s.getMinAge() != null ? s.getMinAge() : "no min",
                        s.getMaxAge() != null ? s.getMaxAge() : "no max"))
                .build();
    }

    /**
     * Annual Income criterion (30 points).
     * Null maxAnnualIncome → criterion auto-satisfied.
     */
    CriterionResult evaluateIncome(Beneficiary b, Scheme s) {
        if (s.getMaxAnnualIncome() == null) {
            return CriterionResult.builder()
                    .points(POINTS_INCOME)
                    .passed(true)
                    .detail("No income restriction defined for this scheme")
                    .build();
        }

        if (b.getAnnualIncome() == null) {
            return CriterionResult.builder()
                    .points(0)
                    .passed(false)
                    .detail("Beneficiary annual income not recorded")
                    .build();
        }

        boolean passes = b.getAnnualIncome().compareTo(s.getMaxAnnualIncome()) <= 0;
        if (passes) {
            return CriterionResult.builder()
                    .points(POINTS_INCOME)
                    .passed(true)
                    .detail(String.format("Income \u20b9%.0f \u2264 threshold \u20b9%.0f",
                            b.getAnnualIncome(), s.getMaxAnnualIncome()))
                    .build();
        }

        return CriterionResult.builder()
                .points(0)
                .passed(false)
                .detail(String.format("Income \u20b9%.0f exceeds threshold \u20b9%.0f",
                        b.getAnnualIncome(), s.getMaxAnnualIncome()))
                .build();
    }

    /**
     * Land Holding criterion (20 points).
     * Null maxLandHolding → criterion auto-satisfied.
     */
    CriterionResult evaluateLand(Beneficiary b, Scheme s) {
        if (s.getMaxLandHolding() == null) {
            return CriterionResult.builder()
                    .points(POINTS_LAND)
                    .passed(true)
                    .detail("No land holding restriction defined for this scheme")
                    .build();
        }

        if (b.getLandHolding() == null) {
            return CriterionResult.builder()
                    .points(0)
                    .passed(false)
                    .detail("Beneficiary land holding not recorded")
                    .build();
        }

        boolean passes = b.getLandHolding().compareTo(s.getMaxLandHolding()) <= 0;
        if (passes) {
            return CriterionResult.builder()
                    .points(POINTS_LAND)
                    .passed(true)
                    .detail(String.format("Land %.2f ac \u2264 threshold %.2f ac",
                            b.getLandHolding(), s.getMaxLandHolding()))
                    .build();
        }

        return CriterionResult.builder()
                .points(0)
                .passed(false)
                .detail(String.format("Land %.2f ac exceeds threshold %.2f ac",
                        b.getLandHolding(), s.getMaxLandHolding()))
                .build();
    }

    /**
     * Category Match criterion (20 points).
     * Null requiredCategory → criterion auto-satisfied (open to all categories).
     * Scheme stores category as a String; compared case-insensitively to
     * beneficiary's {@code Category} enum name.
     */
    CriterionResult evaluateCategory(Beneficiary b, Scheme s) {
        if (s.getRequiredCategory() == null || s.getRequiredCategory().isBlank()) {
            return CriterionResult.builder()
                    .points(POINTS_CATEGORY)
                    .passed(true)
                    .detail("No category restriction — open to all beneficiaries")
                    .build();
        }

        if (b.getCategory() == null) {
            return CriterionResult.builder()
                    .points(0)
                    .passed(false)
                    .detail("Beneficiary category not recorded; required: " + s.getRequiredCategory())
                    .build();
        }

        boolean matches = b.getCategory().name()
                .equalsIgnoreCase(s.getRequiredCategory().trim());

        if (matches) {
            return CriterionResult.builder()
                    .points(POINTS_CATEGORY)
                    .passed(true)
                    .detail("Category " + b.getCategory().name()
                            + " matches required " + s.getRequiredCategory())
                    .build();
        }

        return CriterionResult.builder()
                .points(0)
                .passed(false)
                .detail("Category " + b.getCategory().name()
                        + "; required " + s.getRequiredCategory())
                .build();
    }

    /**
     * Identity Verified criterion (10 points).
     * No scheme-level threshold — this is always a system-level check.
     */
    CriterionResult evaluateIdentity(Beneficiary b) {
        if (b.isIdentityVerified()) {
            return CriterionResult.builder()
                    .points(POINTS_IDENTITY)
                    .passed(true)
                    .detail("Identity verified")
                    .build();
        }

        return CriterionResult.builder()
                .points(0)
                .passed(false)
                .detail("Identity not yet verified by a Field Officer")
                .build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Private helpers
    // ════════════════════════════════════════════════════════════════════════

    /** Builds a response from a freshly-evaluated result + in-memory criteria map. */
    private EligibilityResultResponse toResponse(EligibilityResult saved,
                                                 Map<String, CriterionResult> criteria) {
        return EligibilityResultResponse.builder()
                .resultId(saved.getId())
                .beneficiaryId(saved.getBeneficiaryId())
                .schemeId(saved.getSchemeId())
                .schemeName(saved.getSchemeName())
                .totalScore(saved.getTotalScore())
                .eligibilityStatus(saved.getEligibilityStatus())
                .eligible(saved.getEligibilityStatus() == EligibilityStatus.ELIGIBLE)
                .criteria(criteria)
                .evaluatedAt(saved.getEvaluatedAt())
                .build();
    }

    /** Builds a response from a persisted entity (deserialises the criteria JSON). */
    private EligibilityResultResponse toResponseFromEntity(EligibilityResult entity) {
        Map<String, CriterionResult> criteria = deserialiseCriteria(entity.getCriteriaJson());
        return EligibilityResultResponse.builder()
                .resultId(entity.getId())
                .beneficiaryId(entity.getBeneficiaryId())
                .schemeId(entity.getSchemeId())
                .schemeName(entity.getSchemeName())
                .totalScore(entity.getTotalScore())
                .eligibilityStatus(entity.getEligibilityStatus())
                .eligible(entity.getEligibilityStatus() == EligibilityStatus.ELIGIBLE)
                .criteria(criteria)
                .evaluatedAt(entity.getEvaluatedAt())
                .build();
    }

    private String serialiseCriteria(Map<String, CriterionResult> criteria) {
        try {
            return objectMapper.writeValueAsString(criteria);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialise criteria map", e);
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, CriterionResult> deserialiseCriteria(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructMapType(
                            LinkedHashMap.class, String.class, CriterionResult.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialise criteria JSON", e);
            return Map.of();
        }
    }
}
