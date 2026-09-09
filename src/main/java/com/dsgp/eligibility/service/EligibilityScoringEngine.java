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

import java.math.BigDecimal;
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

        Map<String, CriterionResult> criteria =
                new LinkedHashMap<>();

        int totalScore;
        boolean mandatoryConditionsPassed;

        // ====================================================================
        // PM-KISAN
        // ====================================================================

        if (isScheme(schemeName, PM_KISAN)) {

            CriterionResult age =
                    evaluatePmKisanAge(beneficiary);

            CriterionResult income =
                    evaluatePmKisanIncome(beneficiary);

            CriterionResult land =
                    evaluatePmKisanLand(beneficiary);

            CriterionResult occupation =
                    evaluatePmKisanOccupation(beneficiary);

            CriterionResult category =
                    evaluateCategory(
                            beneficiary,
                            scheme,
                            5
                    );

            CriterionResult identity =
                    evaluateIdentity(
                            beneficiary,
                            10
                    );

            criteria.put("ageCheck", age);
            criteria.put("incomeCheck", income);
            criteria.put("landCheck", land);
            criteria.put("occupationCheck", occupation);
            criteria.put("categoryCheck", category);
            criteria.put("identityCheck", identity);

            totalScore =
                    age.getPoints()
                            + income.getPoints()
                            + land.getPoints()
                            + occupation.getPoints()
                            + category.getPoints()
                            + identity.getPoints();

            mandatoryConditionsPassed =
                    age.isPassed()
                            && income.isPassed()
                            && land.isPassed()
                            && occupation.isPassed();
        }

        // ====================================================================
        // NSP
        // ====================================================================

        else if (isScheme(schemeName, NSP)) {

            CriterionResult age =
                    evaluateNspAge(beneficiary);

            CriterionResult income =
                    evaluateNspIncome(beneficiary);

            CriterionResult occupation =
                    evaluateNspOccupation(beneficiary);

            CriterionResult category =
                    evaluateCategory(
                            beneficiary,
                            scheme,
                            10
                    );

            CriterionResult identity =
                    evaluateIdentity(
                            beneficiary,
                            10
                    );

            criteria.put("ageCheck", age);
            criteria.put("incomeCheck", income);
            criteria.put("occupationCheck", occupation);
            criteria.put("categoryCheck", category);
            criteria.put("identityCheck", identity);

            totalScore =
                    age.getPoints()
                            + income.getPoints()
                            + occupation.getPoints()
                            + category.getPoints()
                            + identity.getPoints();

            mandatoryConditionsPassed =
                    age.isPassed()
                            && income.isPassed()
                            && occupation.isPassed();
        }

        // ====================================================================
        // PMEGP
        // ====================================================================

        else if (isScheme(schemeName, PMEGP)) {

            CriterionResult age =
                    evaluatePmegpAge(beneficiary);

            CriterionResult income =
                    evaluatePmegpIncome(beneficiary);

            CriterionResult occupation =
                    evaluatePmegpOccupation(beneficiary);

            CriterionResult category =
                    evaluateCategory(
                            beneficiary,
                            scheme,
                            20
                    );

            CriterionResult identity =
                    evaluateIdentity(
                            beneficiary,
                            10
                    );

            criteria.put("ageCheck", age);
            criteria.put("incomeCheck", income);
            criteria.put("occupationCheck", occupation);
            criteria.put("categoryCheck", category);
            criteria.put("identityCheck", identity);

            totalScore =
                    age.getPoints()
                            + income.getPoints()
                            + occupation.getPoints()
                            + category.getPoints()
                            + identity.getPoints();

            mandatoryConditionsPassed =
                    age.isPassed()
                            && income.isPassed();
        }

        else {
            throw new EligibilityCheckException(
                    "Unsupported scheme for eligibility scoring: "
                            + schemeName);
        }

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
    // PM-KISAN
    // ========================================================================

    private CriterionResult evaluatePmKisanAge(
            Beneficiary b) {

        Integer age = b.getAge();

        if (age == null) {
            return failed("Beneficiary age not recorded");
        }

        if (age < 18 || age > 70) {
            return failed(
                    "Age " + age
                            + " is outside PM-KISAN required range 18-70"
            );
        }

        if (age <= 30) {
            return passed(
                    15,
                    "Age " + age + " → 15/15 points"
            );
        }

        if (age <= 45) {
            return passed(
                    12,
                    "Age " + age + " → 12/15 points"
            );
        }

        if (age <= 60) {
            return passed(
                    9,
                    "Age " + age + " → 9/15 points"
            );
        }

        return passed(
                6,
                "Age " + age + " → 6/15 points"
        );
    }

    private CriterionResult evaluatePmKisanIncome(
            Beneficiary b) {

        BigDecimal income = b.getAnnualIncome();

        if (income == null) {
            return failed(
                    "Beneficiary annual income not recorded"
            );
        }

        if (income.compareTo(
                new BigDecimal("300000")) > 0) {

            return failed(
                    "Income ₹" + income
                            + " exceeds PM-KISAN maximum ₹300000"
            );
        }

        if (income.compareTo(
                new BigDecimal("100000")) <= 0) {

            return passed(
                    25,
                    "Income ₹" + income
                            + " → 25/25 points"
            );
        }

        if (income.compareTo(
                new BigDecimal("150000")) <= 0) {

            return passed(
                    20,
                    "Income ₹" + income
                            + " → 20/25 points"
            );
        }

        if (income.compareTo(
                new BigDecimal("200000")) <= 0) {

            return passed(
                    15,
                    "Income ₹" + income
                            + " → 15/25 points"
            );
        }

        if (income.compareTo(
                new BigDecimal("250000")) <= 0) {

            return passed(
                    10,
                    "Income ₹" + income
                            + " → 10/25 points"
            );
        }

        return passed(
                5,
                "Income ₹" + income
                        + " → 5/25 points"
        );
    }

    private CriterionResult evaluatePmKisanLand(
            Beneficiary b) {

        BigDecimal land = b.getLandHolding();

        if (land == null) {
            return failed(
                    "Beneficiary land holding not recorded"
            );
        }

        if (land.compareTo(
                new BigDecimal("5")) > 0) {

            return failed(
                    "Land holding " + land
                            + " acres exceeds PM-KISAN maximum 5 acres"
            );
        }

        if (land.compareTo(
                new BigDecimal("1")) <= 0) {

            return passed(
                    25,
                    "Land holding " + land
                            + " acres → 25/25 points"
            );
        }

        if (land.compareTo(
                new BigDecimal("2")) <= 0) {

            return passed(
                    20,
                    "Land holding " + land
                            + " acres → 20/25 points"
            );
        }

        if (land.compareTo(
                new BigDecimal("3")) <= 0) {

            return passed(
                    15,
                    "Land holding " + land
                            + " acres → 15/25 points"
            );
        }

        if (land.compareTo(
                new BigDecimal("4")) <= 0) {

            return passed(
                    10,
                    "Land holding " + land
                            + " acres → 10/25 points"
            );
        }

        return passed(
                5,
                "Land holding " + land
                        + " acres → 5/25 points"
        );
    }

    private CriterionResult evaluatePmKisanOccupation(
            Beneficiary b) {

        String occupation =
                normalise(b.getOccupation());

        if (occupation.isEmpty()) {
            return failed(
                    "Beneficiary occupation not recorded"
            );
        }

        if (occupation.equals("farmer")
                || occupation.equals("agriculture")
                || occupation.equals("agriculturist")) {

            return passed(
                    20,
                    "Occupation Farmer → 20/20 points"
            );
        }

        return failed(
                "Occupation " + b.getOccupation()
                        + " does not satisfy PM-KISAN Farmer requirement"
        );
    }

    // ========================================================================
    // NSP
    // ========================================================================

    private CriterionResult evaluateNspAge(
            Beneficiary b) {

        Integer age = b.getAge();

        if (age == null) {
            return failed("Beneficiary age not recorded");
        }

        if (age < 15 || age > 30) {
            return failed(
                    "Age " + age
                            + " is outside NSP required range 15-30"
            );
        }

        if (age <= 18) {
            return passed(
                    20,
                    "Age " + age + " → 20/20 points"
            );
        }

        if (age <= 22) {
            return passed(
                    17,
                    "Age " + age + " → 17/20 points"
            );
        }

        if (age <= 26) {
            return passed(
                    14,
                    "Age " + age + " → 14/20 points"
            );
        }

        return passed(
                10,
                "Age " + age + " → 10/20 points"
        );
    }

    private CriterionResult evaluateNspIncome(
            Beneficiary b) {

        BigDecimal income =
                b.getAnnualIncome();

        if (income == null) {
            return failed(
                    "Beneficiary annual income not recorded"
            );
        }

        if (income.compareTo(
                new BigDecimal("250000")) > 0) {

            return failed(
                    "Income ₹" + income
                            + " exceeds NSP maximum ₹250000"
            );
        }

        if (income.compareTo(
                new BigDecimal("100000")) <= 0) {

            return passed(
                    30,
                    "Income ₹" + income
                            + " → 30/30 points"
            );
        }

        if (income.compareTo(
                new BigDecimal("150000")) <= 0) {

            return passed(
                    25,
                    "Income ₹" + income
                            + " → 25/30 points"
            );
        }

        if (income.compareTo(
                new BigDecimal("200000")) <= 0) {

            return passed(
                    20,
                    "Income ₹" + income
                            + " → 20/30 points"
            );
        }

        return passed(
                10,
                "Income ₹" + income
                        + " → 10/30 points"
        );
    }

    private CriterionResult evaluateNspOccupation(
            Beneficiary b) {

        String occupation =
                normalise(b.getOccupation());

        if (occupation.isEmpty()) {
            return failed(
                    "Beneficiary occupation not recorded"
            );
        }

        if (occupation.equals("student")
                || occupation.equals("studying")
                || occupation.equals("student/learner")) {

            return passed(
                    30,
                    "Occupation Student → 30/30 points"
            );
        }

        return failed(
                "Occupation " + b.getOccupation()
                        + " does not satisfy NSP Student requirement"
        );
    }

    // ========================================================================
    // PMEGP
    // ========================================================================

    private CriterionResult evaluatePmegpAge(
            Beneficiary b) {

        Integer age = b.getAge();

        if (age == null) {
            return failed("Beneficiary age not recorded");
        }

        if (age < 18 || age > 55) {
            return failed(
                    "Age " + age
                            + " is outside PMEGP required range 18-55"
            );
        }

        if (age <= 25) {
            return passed(
                    20,
                    "Age " + age + " → 20/20 points"
            );
        }

        if (age <= 35) {
            return passed(
                    17,
                    "Age " + age + " → 17/20 points"
            );
        }

        if (age <= 45) {
            return passed(
                    14,
                    "Age " + age + " → 14/20 points"
            );
        }

        return passed(
                10,
                "Age " + age + " → 10/20 points"
        );
    }

    private CriterionResult evaluatePmegpIncome(
            Beneficiary b) {

        BigDecimal income =
                b.getAnnualIncome();

        if (income == null) {
            return failed(
                    "Beneficiary annual income not recorded"
            );
        }

        if (income.compareTo(
                new BigDecimal("800000")) > 0) {

            return failed(
                    "Income ₹" + income
                            + " exceeds PMEGP maximum ₹800000"
            );
        }

        if (income.compareTo(
                new BigDecimal("200000")) <= 0) {

            return passed(
                    30,
                    "Income ₹" + income
                            + " → 30/30 points"
            );
        }

        if (income.compareTo(
                new BigDecimal("400000")) <= 0) {

            return passed(
                    25,
                    "Income ₹" + income
                            + " → 25/30 points"
            );
        }

        if (income.compareTo(
                new BigDecimal("600000")) <= 0) {

            return passed(
                    20,
                    "Income ₹" + income
                            + " → 20/30 points"
            );
        }

        return passed(
                10,
                "Income ₹" + income
                        + " → 10/30 points"
        );
    }

    private CriterionResult evaluatePmegpOccupation(
            Beneficiary b) {

        String occupation =
                normalise(b.getOccupation());

        if (occupation.isEmpty()) {
            return failed(
                    "Beneficiary occupation not recorded"
            );
        }

        return passed(
                20,
                "Occupation " + b.getOccupation()
                        + " → 20/20 points"
        );
    }

    // ========================================================================
    // CATEGORY
    // ========================================================================

    private CriterionResult evaluateCategory(
            Beneficiary b,
            Scheme s,
            int maxPoints) {

        String requiredCategory =
                normalise(s.getRequiredCategory());

        if (requiredCategory.isEmpty()
                || requiredCategory.equals("all")) {

            return passed(
                    maxPoints,
                    "No category restriction → "
                            + maxPoints + "/" + maxPoints + " points"
            );
        }

        if (b.getCategory() == null) {
            return failed(
                    "Beneficiary category not recorded; required: "
                            + s.getRequiredCategory()
            );
        }

        boolean matches =
                b.getCategory()
                        .name()
                        .equalsIgnoreCase(
                                s.getRequiredCategory().trim()
                        );

        if (matches) {
            return passed(
                    maxPoints,
                    "Category "
                            + b.getCategory().name()
                            + " matches required "
                            + s.getRequiredCategory()
            );
        }

        return failed(
                "Category "
                        + b.getCategory().name()
                        + "; required "
                        + s.getRequiredCategory()
        );
    }

    // ========================================================================
    // IDENTITY
    // ========================================================================

    private CriterionResult evaluateIdentity(
            Beneficiary b,
            int maxPoints) {

        if (b.isIdentityVerified()) {

            return passed(
                    maxPoints,
                    "Identity verified → "
                            + maxPoints
                            + "/"
                            + maxPoints
                            + " points"
            );
        }

        return failed(
                "Identity not yet verified by a Field Officer"
        );
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private CriterionResult passed(
            int points,
            String detail) {

        return CriterionResult.builder()
                .points(points)
                .passed(true)
                .detail(detail)
                .build();
    }

    private CriterionResult failed(
            String detail) {

        return CriterionResult.builder()
                .points(0)
                .passed(false)
                .detail(detail)
                .build();
    }

    private String normalise(String value) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase()
                .replaceAll("\\s+", " ");
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