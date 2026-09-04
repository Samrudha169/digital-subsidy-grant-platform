package com.dsgp.eligibility.service;

import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.Scheme;

public interface EligibilityService {

    boolean isEligible(Beneficiary beneficiary, Scheme scheme);

    int calculateEligibilityScore(Beneficiary beneficiary, Scheme scheme);

    String determineRouting(int eligibilityScore, Scheme scheme);
}