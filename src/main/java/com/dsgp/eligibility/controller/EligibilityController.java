package com.dsgp.eligibility.controller;

import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.Category;
import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.beneficiary.repository.SchemeRepository;
import com.dsgp.eligibility.service.EligibilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/eligibility")
@CrossOrigin(origins = "http://localhost:5173")
public class EligibilityController {

    private final EligibilityService eligibilityService;
    private final SchemeRepository schemeRepository;

    public EligibilityController(
            EligibilityService eligibilityService,
            SchemeRepository schemeRepository) {
        this.eligibilityService = eligibilityService;
        this.schemeRepository = schemeRepository;
    }

    @GetMapping("/test")
    public String test() {
        return "Eligibility Controller is working";
    }

    @PostMapping("/check")
    public ResponseEntity<List<EligibilityResponse>> checkEligibility(
            @RequestBody EligibilityRequest request) {

        List<Scheme> schemes = schemeRepository.findAll();
        List<EligibilityResponse> eligibleSchemes = new ArrayList<>();

        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setAge(request.getAge());
        beneficiary.setAnnualIncome(request.getAnnualIncome());
        beneficiary.setState(request.getState());
        beneficiary.setOccupation(request.getOccupation());

        // Default to 0.0 if user did not provide land holding
        beneficiary.setLandHolding(
                request.getLandHolding() != null ? request.getLandHolding() : BigDecimal.ZERO
        );

        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            try {
                beneficiary.setCategory(
                        Category.valueOf(request.getCategory().toUpperCase())
                );
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid category mapping
            }
        }

        for (Scheme scheme : schemes) {
            if (Boolean.FALSE.equals(scheme.getActive())) {
                continue;
            }

            if (eligibilityService.isEligible(beneficiary, scheme)) {
                int score = eligibilityService.calculateEligibilityScore(beneficiary, scheme);
                eligibleSchemes.add(
                        new EligibilityResponse(
                                scheme.getId(),
                                scheme.getSchemeName(),
                                scheme.getDescription(),
                                score
                        )
                );
            }
        }

        return ResponseEntity.ok(eligibleSchemes);
    }

    public static class EligibilityRequest {
        private String state;
        private Integer age;
        private String occupation;
        private String category;
        private BigDecimal annualIncome;
        private BigDecimal landHolding;

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }

        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }

        public String getOccupation() { return occupation; }
        public void setOccupation(String occupation) { this.occupation = occupation; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public BigDecimal getAnnualIncome() { return annualIncome; }
        public void setAnnualIncome(BigDecimal annualIncome) { this.annualIncome = annualIncome; }

        public BigDecimal getLandHolding() { return landHolding; }
        public void setLandHolding(BigDecimal landHolding) { this.landHolding = landHolding; }
    }

    public static class EligibilityResponse {
        private Long schemeId;
        private String schemeName;
        private String description;
        private int eligibilityScore;

        public EligibilityResponse(
                Long schemeId,
                String schemeName,
                String description,
                int eligibilityScore) {
            this.schemeId = schemeId;
            this.schemeName = schemeName;
            this.description = description;
            this.eligibilityScore = eligibilityScore;
        }

        public Long getSchemeId() { return schemeId; }
        public String getSchemeName() { return schemeName; }
        public String getDescription() { return description; }
        public int getEligibilityScore() { return eligibilityScore; }
    }
}