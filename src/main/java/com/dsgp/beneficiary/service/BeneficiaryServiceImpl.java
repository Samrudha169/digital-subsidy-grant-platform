package com.dsgp.beneficiary.service;

import com.dsgp.beneficiary.dto.BeneficiaryRegistrationRequest;
import com.dsgp.beneficiary.dto.BeneficiaryResponse;
import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.exception.BeneficiaryNotFoundException;
import com.dsgp.beneficiary.exception.DuplicateAadhaarException;
import com.dsgp.beneficiary.exception.DuplicateMobileException;
import com.dsgp.beneficiary.repository.BeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BeneficiaryServiceImpl implements BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;

    @Override
    public BeneficiaryResponse registerBeneficiary(
            BeneficiaryRegistrationRequest request) {

        if (beneficiaryRepository.existsByGovId(request.getGovId())) {
            throw new DuplicateAadhaarException(request.getGovId());
        }

        if (beneficiaryRepository.existsByContact(request.getContact())) {
            throw new DuplicateMobileException(request.getContact());
        }

        Beneficiary beneficiary = Beneficiary.builder()
                .fullName(request.getFullName())
                .govId(request.getGovId())
                .contact(request.getContact())
                .email(request.getEmail())
                .age(request.getAge())
                .address(request.getAddress())
                .schemeName(request.getSchemeName())
                .build();

        Beneficiary saved = beneficiaryRepository.save(beneficiary);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BeneficiaryResponse getBeneficiaryById(Integer id) {

        Beneficiary beneficiary = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new BeneficiaryNotFoundException(id));

        return mapToResponse(beneficiary);
    }

    @Override
    @Transactional(readOnly = true)
    public BeneficiaryResponse getBeneficiaryByGovId(String govId) {

        Beneficiary beneficiary = beneficiaryRepository.findByGovId(govId)
                .orElseThrow(() ->
                        new BeneficiaryNotFoundException(
                                "Beneficiary not found with Government ID: " + govId
                        ));

        return mapToResponse(beneficiary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getAllBeneficiaries() {

        return beneficiaryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public BeneficiaryResponse updateBeneficiary(
            Integer id,
            BeneficiaryRegistrationRequest request) {

        Beneficiary beneficiary = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new BeneficiaryNotFoundException(id));

        if (!request.getGovId().equals(beneficiary.getGovId())
                && beneficiaryRepository.existsByGovId(request.getGovId())) {
            throw new DuplicateAadhaarException(request.getGovId());
        }

        if (!request.getContact().equals(beneficiary.getContact())
                && beneficiaryRepository.existsByContact(request.getContact())) {
            throw new DuplicateMobileException(request.getContact());
        }

        beneficiary.setFullName(request.getFullName());
        beneficiary.setGovId(request.getGovId());
        beneficiary.setContact(request.getContact());
        beneficiary.setEmail(request.getEmail());
        beneficiary.setAge(request.getAge());
        beneficiary.setAddress(request.getAddress());
        beneficiary.setSchemeName(request.getSchemeName());

        return mapToResponse(beneficiaryRepository.save(beneficiary));
    }

    @Override
    public void deleteBeneficiary(Integer id) {

        Beneficiary beneficiary = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new BeneficiaryNotFoundException(id));

        beneficiaryRepository.delete(beneficiary);
    }

    private BeneficiaryResponse mapToResponse(Beneficiary beneficiary) {

        return BeneficiaryResponse.builder()
                .id(beneficiary.getId())
                .fullName(beneficiary.getFullName())
                .govId(beneficiary.getGovId())
                .contact(beneficiary.getContact())
                .email(beneficiary.getEmail())
                .age(beneficiary.getAge())
                .address(beneficiary.getAddress())
                .schemeName(beneficiary.getSchemeName())
                .build();
    }
}