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
import com.dsgp.eligibility.rules.EligibilityRule;
import com.dsgp.eligibility.rules.NspEligibilityRule;
import com.dsgp.eligibility.rules.PmKisanEligibilityRule;
import com.dsgp.eligibility.rules.PmegpEligibilityRule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EligibilityScoringEngine implements EligibilityService {

    private static final int ELIGIBILITY_THRESHOLD = 60;

    private static final String PM_KISAN = "PM-KISAN";
    private static final String NSP = "NSP";
    private static final String PMEGP = "PMEGP";

    private final BeneficiaryRepository beneficiaryRepository;
    private final SchemeRepository schemeRepository;
    private final EligibilityResultRepository resultRepository;
    private final ObjectMapper objectMapper;

    // Rule instances are created here directly because the rule classes are
    // stateless; this also preserves full test compatibility with @InjectMocks,
    // which only injects mocked/spied collaborators declared in the test.
    private final EligibilityRule pmKisanRule = new PmKisanEligibilityRule();
    private final EligibilityRule nspRule     = new NspEligibilityRule();
    private final EligibilityRule pmegpRule   = new PmegpEligibilityRule();

    // ========================================================================
    // MAIN ELIGIBILITY CHECK
    // ========================================================================

    @Override
    public EligibilityResultResponse checkEligibility(
            EligibilityCheckRequest request) {

        Beneficiary beneficiary = beneficiaryRepository
                .findById(request.getBeneficiaryId())
                .orElseThrow(() -> new EligibilityCheckException(
                        "Beneficiary not found with ID: "
                                + request.getBeneficiaryId()));

        Scheme scheme = schemeRepository
                .findById(request.getSchemeId())
                .orElseThrow(() -> new EligibilityCheckException(
                        "Scheme not found with ID: "
                                + request.getSchemeId()));

        String schemeName = scheme.getSchemeName();

        log.info(
                "Evaluating eligibility: beneficiary={} scheme={}",
                beneficiary.getId(),
                schemeName
        );

        // ====================================================================
        // SELECT RULE
        // ====================================================================

        EligibilityRule rule = selectRule(schemeName);

        // ====================================================================
        // DELEGATE SCHEME-SPECIFIC EVALUATION
        // ====================================================================

        Map<String, CriterionResult> criteria =
                rule.evaluate(beneficiary, scheme);

        // ====================================================================
        // CALCULATE TOTAL SCORE
        // ====================================================================

        int totalScore = criteria.values()
                .stream()
                .mapToInt(CriterionResult::getPoints)
                .sum();

        boolean mandatoryConditionsPassed =
                rule.mandatoryConditionsPassed(criteria);

        // ====================================================================
        // FINAL DECISION
        // ====================================================================

        boolean eligible =
                mandatoryConditionsPassed
                        && totalScore >= ELIGIBILITY_THRESHOLD;

        EligibilityStatus status =
                eligible
                        ? EligibilityStatus.ELIGIBLE
                        : EligibilityStatus.INELIGIBLE;

        log.info(
                "Eligibility result: beneficiary={} scheme={} score={} mandatory={} status={}",
                beneficiary.getId(),
                schemeName,
                totalScore,
                mandatoryConditionsPassed,
                status
        );

        // ====================================================================
        // SAVE RESULT
        // ====================================================================

        EligibilityResult entity =
                resultRepository
                        .findByBeneficiaryIdAndSchemeId(
                                beneficiary.getId(),
                                scheme.getId()
                        )
                        .orElseGet(EligibilityResult::new);

        entity.setBeneficiaryId(beneficiary.getId());
        entity.setSchemeId(scheme.getId());
        entity.setSchemeName(scheme.getSchemeName());
        entity.setTotalScore(totalScore);
        entity.setEligibilityStatus(status);
        entity.setCriteriaJson(
                serialiseCriteria(criteria)
        );

        EligibilityResult saved =
                resultRepository.save(entity);

        return toResponse(saved, criteria);
    }

    // ========================================================================
    // RULE SELECTION
    // ========================================================================

    /**
     * Selects the {@link EligibilityRule} that corresponds to the given scheme
     * name.  The scheme name is matched case-insensitively and may optionally
     * be followed by a space and additional text (e.g. "PM-KISAN 2024" still
     * maps to {@link PmKisanEligibilityRule}).
     *
     * @param schemeName the name of the scheme as stored in the database
     * @return the matching rule
     * @throws EligibilityCheckException if no rule is registered for the scheme
     */
    private EligibilityRule selectRule(String schemeName) {

        if (isScheme(schemeName, PM_KISAN)) {
            return pmKisanRule;
        }

        if (isScheme(schemeName, NSP)) {
            return nspRule;
        }

        if (isScheme(schemeName, PMEGP)) {
            return pmegpRule;
        }

        throw new EligibilityCheckException(
                "Unsupported scheme for eligibility scoring: "
                        + schemeName);
    }

    private boolean isScheme(
            String actualScheme,
            String expectedScheme) {

        if (actualScheme == null || expectedScheme == null) {
            return false;
        }

        String actual = actualScheme.trim().toLowerCase();
        String expected = expectedScheme.trim().toLowerCase();

        return actual.equals(expected)
                || actual.startsWith(expected + " ");
    }

    // ========================================================================
    // RESPONSE MAPPING
    // ========================================================================

    private EligibilityResultResponse toResponse(
            EligibilityResult saved,
            Map<String, CriterionResult> criteria) {

        return EligibilityResultResponse.builder()
                .resultId(saved.getId())
                .beneficiaryId(saved.getBeneficiaryId())
                .schemeId(saved.getSchemeId())
                .schemeName(saved.getSchemeName())
                .totalScore(saved.getTotalScore())
                .eligibilityStatus(
                        saved.getEligibilityStatus()
                )
                .eligible(
                        saved.getEligibilityStatus()
                                == EligibilityStatus.ELIGIBLE
                )
                .criteria(criteria)
                .evaluatedAt(saved.getEvaluatedAt())
                .build();
    }

    private EligibilityResultResponse toResponseFromEntity(
            EligibilityResult entity) {

        Map<String, CriterionResult> criteria =
                deserialiseCriteria(
                        entity.getCriteriaJson()
                );

        return EligibilityResultResponse.builder()
                .resultId(entity.getId())
                .beneficiaryId(entity.getBeneficiaryId())
                .schemeId(entity.getSchemeId())
                .schemeName(entity.getSchemeName())
                .totalScore(entity.getTotalScore())
                .eligibilityStatus(
                        entity.getEligibilityStatus()
                )
                .eligible(
                        entity.getEligibilityStatus()
                                == EligibilityStatus.ELIGIBLE
                )
                .criteria(criteria)
                .evaluatedAt(entity.getEvaluatedAt())
                .build();
    }

    private String serialiseCriteria(
            Map<String, CriterionResult> criteria) {

        try {
            return objectMapper.writeValueAsString(criteria);

        } catch (JsonProcessingException e) {

            log.error(
                    "Failed to serialise eligibility criteria",
                    e
            );

            return "{}";
        }
    }

    private Map<String, CriterionResult>
    deserialiseCriteria(String json) {

        if (json == null || json.isBlank()) {
            return Map.of();
        }

        try {

            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory()
                            .constructMapType(
                                    LinkedHashMap.class,
                                    String.class,
                                    CriterionResult.class
                            )
            );

        } catch (JsonProcessingException e) {

            log.error(
                    "Failed to deserialise eligibility criteria",
                    e
            );

            return Map.of();
        }
    }

    // ========================================================================
    // READ-ONLY LOOKUPS
    // ========================================================================

    @Override
    @Transactional(readOnly = true)
    public EligibilityResultResponse getResult(
            Integer beneficiaryId,
            Long schemeId) {

        EligibilityResult result =
                resultRepository
                        .findByBeneficiaryIdAndSchemeId(
                                beneficiaryId,
                                schemeId
                        )
                        .orElseThrow(() ->
                                new EligibilityCheckException(
                                        "No eligibility result found for beneficiary "
                                                + beneficiaryId
                                                + " and scheme "
                                                + schemeId
                                ));

        return toResponseFromEntity(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EligibilityResultResponse>
    getResultsForBeneficiary(
            Integer beneficiaryId) {

        return resultRepository
                .findByBeneficiaryId(beneficiaryId)
                .stream()
                .map(this::toResponseFromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EligibilityResultResponse>
    getResultsForScheme(Long schemeId) {

        return resultRepository
                .findBySchemeId(schemeId)
                .stream()
                .map(this::toResponseFromEntity)
                .toList();
    }
}