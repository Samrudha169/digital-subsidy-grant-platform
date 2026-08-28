package com.dsgp.beneficiary.service;

import com.dsgp.beneficiary.dto.BeneficiaryRegistrationRequest;
import com.dsgp.beneficiary.dto.BeneficiaryResponse;

import java.util.List;

public interface BeneficiaryService {

    BeneficiaryResponse registerBeneficiary(
            BeneficiaryRegistrationRequest request);

    BeneficiaryResponse getBeneficiaryById(Integer id);

    BeneficiaryResponse getBeneficiaryByGovId(String govId);

    List<BeneficiaryResponse> getAllBeneficiaries();

    BeneficiaryResponse updateBeneficiary(
            Integer id,
            BeneficiaryRegistrationRequest request);

    void deleteBeneficiary(Integer id);
}