package com.dsgp.eligibility.service;

import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.Scheme;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EligibilityServiceImpl implements EligibilityService {

    @Override
    public boolean isEligible(
            Beneficiary beneficiary,
            Scheme scheme) {

        // ---------------------------------------------------------
        // 1. AGE ELIGIBILITY
        // ---------------------------------------------------------

        if (beneficiary.getAge() == null) {
            return false;
        }

        if (scheme.getMinAge() != null &&
                beneficiary.getAge() < scheme.getMinAge()) {

            return false;
        }

        if (scheme.getMaxAge() != null &&
                beneficiary.getAge() > scheme.getMaxAge()) {

            return false;
        }


        // ---------------------------------------------------------
        // 2. ANNUAL INCOME ELIGIBILITY
        // ---------------------------------------------------------

        if (scheme.getMaxAnnualIncome() != null) {

            if (beneficiary.getAnnualIncome() == null) {
                return false;
            }

            if (beneficiary.getAnnualIncome()
                    .compareTo(scheme.getMaxAnnualIncome()) > 0) {

                return false;
            }
        }


        // ---------------------------------------------------------
        // 3. LAND HOLDING ELIGIBILITY
        // ---------------------------------------------------------

        if (scheme.getMaxLandHolding() != null) {

            if (beneficiary.getLandHolding() == null) {
                return false;
            }

            if (beneficiary.getLandHolding()
                    .compareTo(scheme.getMaxLandHolding()) > 0) {

                return false;
            }
        }


        // ---------------------------------------------------------
        // 4. CATEGORY ELIGIBILITY
        // ---------------------------------------------------------

        if (scheme.getRequiredCategory() != null &&
                !scheme.getRequiredCategory().isBlank() &&
                !scheme.getRequiredCategory()
                        .equalsIgnoreCase("ALL")) {

            if (beneficiary.getCategory() == null) {
                return false;
            }

            if (!scheme.getRequiredCategory()
                    .equalsIgnoreCase(
                            beneficiary.getCategory().name())) {

                return false;
            }
        }


        // ---------------------------------------------------------
        // 5. STATE ELIGIBILITY
        // ---------------------------------------------------------

        if (scheme.getRequiredState() != null &&
                !scheme.getRequiredState().isBlank() &&
                !scheme.getRequiredState()
                        .equalsIgnoreCase("ALL")) {

            if (beneficiary.getState() == null ||
                    beneficiary.getState().isBlank()) {

                return false;
            }

            if (!scheme.getRequiredState()
                    .equalsIgnoreCase(
                            beneficiary.getState().trim())) {

                return false;
            }
        }


        // ---------------------------------------------------------
        // 6. OCCUPATION ELIGIBILITY
        // ---------------------------------------------------------

        if (scheme.getRequiredOccupation() != null &&
                !scheme.getRequiredOccupation().isBlank() &&
                !scheme.getRequiredOccupation()
                        .equalsIgnoreCase("ALL")) {

            if (beneficiary.getOccupation() == null ||
                    beneficiary.getOccupation().isBlank()) {

                return false;
            }

            if (!scheme.getRequiredOccupation()
                    .equalsIgnoreCase(
                            beneficiary.getOccupation().trim())) {

                return false;
            }
        }


        // ---------------------------------------------------------
        // ALL CONDITIONS PASSED
        // ---------------------------------------------------------

        return true;
    }


    @Override
    public int calculateEligibilityScore(
            Beneficiary beneficiary,
            Scheme scheme) {

        // If not eligible, score is 0.
        if (!isEligible(beneficiary, scheme)) {
            return 0;
        }

        int score = 100;


        // ---------------------------------------------------------
        // INCOME SCORE
        // ---------------------------------------------------------

        if (scheme.getMaxAnnualIncome() != null &&
                beneficiary.getAnnualIncome() != null) {

            BigDecimal income = beneficiary.getAnnualIncome();
            BigDecimal limit = scheme.getMaxAnnualIncome();

            if (income.compareTo(
                    limit.multiply(
                            BigDecimal.valueOf(0.75))) > 0) {

                score -= 20;

            } else if (income.compareTo(
                    limit.multiply(
                            BigDecimal.valueOf(0.50))) > 0) {

                score -= 10;
            }
        }


        // ---------------------------------------------------------
        // LAND HOLDING SCORE
        // ---------------------------------------------------------

        if (scheme.getMaxLandHolding() != null &&
                beneficiary.getLandHolding() != null) {

            BigDecimal land = beneficiary.getLandHolding();
            BigDecimal limit = scheme.getMaxLandHolding();

            if (land.compareTo(
                    limit.multiply(
                            BigDecimal.valueOf(0.75))) > 0) {

                score -= 20;

            } else if (land.compareTo(
                    limit.multiply(
                            BigDecimal.valueOf(0.50))) > 0) {

                score -= 10;
            }
        }


        return Math.max(0, Math.min(100, score));
    }


    @Override
    public String determineRouting(
            int eligibilityScore,
            Scheme scheme) {

        BigDecimal grantAmount = scheme.getGrantAmount();

        /*
         * HIGH eligibility
         * Score >= 80
         */
        if (eligibilityScore >= 80) {

            if (grantAmount != null &&
                    grantAmount.compareTo(
                            BigDecimal.valueOf(10000)) >= 0) {

                return "FINAL_AUTHORITY";
            }

            return "DISTRICT_OFFICER";
        }


        /*
         * MEDIUM eligibility
         * Score 60–79
         */
        if (eligibilityScore >= 60) {
            return "VERIFYING_OFFICER";
        }


        /*
         * LOW eligibility
         * Score below 60
         */
        return "FIELD_OFFICER";
    }
}