package com.dsgp.beneficiary.service;

import com.dsgp.beneficiary.dto.BeneficiaryRegistrationRequest;
import com.dsgp.beneficiary.dto.BeneficiaryResponse;
import com.dsgp.beneficiary.dto.BeneficiaryUpdateRequest;
import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.exception.BeneficiaryNotFoundException;
import com.dsgp.beneficiary.exception.DuplicateAadhaarException;
import com.dsgp.beneficiary.exception.DuplicateMobileException;
import com.dsgp.beneficiary.repository.BeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BeneficiaryServiceImpl implements BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final PasswordEncoder passwordEncoder;

    // ============================================================
    // REGISTER BENEFICIARY
    // ============================================================

    @Override
    public BeneficiaryResponse registerBeneficiary(
            BeneficiaryRegistrationRequest request) {

        // Check legacy Government ID
        if (beneficiaryRepository.existsByGovId(request.getGovId())) {
            throw new DuplicateAadhaarException(request.getGovId());
        }

        // Check legacy contact number
        if (beneficiaryRepository.existsByContact(request.getContact())) {
            throw new DuplicateMobileException(request.getContact());
        }

        // Check Aadhaar if supplied
        if (request.getAadhaarNumber() != null
                && beneficiaryRepository.existsByAadhaarNumber(request.getAadhaarNumber())) {
            throw new DuplicateAadhaarException(request.getAadhaarNumber());
        }

        // Check mobile number if supplied
        if (request.getMobileNumber() != null
                && beneficiaryRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new DuplicateMobileException(request.getMobileNumber());
        }

        Beneficiary beneficiary = Beneficiary.builder()
                // Legacy fields
                .fullName(request.getFullName())
                .govId(request.getGovId())
                .contact(request.getContact())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .age(request.getAge())
                .address(request.getAddress())
                .schemeName(request.getSchemeName())

                // Extended identity fields
                .aadhaarNumber(request.getAadhaarNumber())
                .mobileNumber(request.getMobileNumber())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())

                // Structured address
                .village(request.getVillage())
                .taluka(request.getTaluka())
                .district(request.getDistrict())
                .state(request.getState())
                .pinCode(request.getPinCode())

                // Eligibility fields
                .annualIncome(request.getAnnualIncome())
                .landHolding(request.getLandHolding())
                .category(request.getCategory())

                .build();

        Beneficiary saved = beneficiaryRepository.save(beneficiary);

        return mapToResponse(saved);
    }

    // ============================================================
    // GET BY ID
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public BeneficiaryResponse getBeneficiaryById(Integer id) {

        Beneficiary beneficiary = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new BeneficiaryNotFoundException(id));

        return mapToResponse(beneficiary);
    }

    // ============================================================
    // GET BY GOVERNMENT ID
    // ============================================================

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

    // ============================================================
    // GET ALL
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getAllBeneficiaries() {

        return beneficiaryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ============================================================
    // LEGACY UPDATE
    // ============================================================

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

        /*
         * Password is intentionally not changed here.
         * Password changes should be handled separately.
         */

        beneficiaryRepository.save(beneficiary);

        return mapToResponse(beneficiary);
    }

    // ============================================================
    // MILESTONE 2 PATCH UPDATE
    // ============================================================

    @Override
    public BeneficiaryResponse updateBeneficiary(
            Integer id,
            BeneficiaryUpdateRequest request) {

        Beneficiary beneficiary = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new BeneficiaryNotFoundException(id));

        // --------------------------------------------------------
        // Name
        // --------------------------------------------------------

        if (request.getFirstName() != null) {
            beneficiary.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            beneficiary.setLastName(request.getLastName());
        }

        // --------------------------------------------------------
        // Personal details
        // --------------------------------------------------------

        if (request.getDateOfBirth() != null) {
            beneficiary.setDateOfBirth(request.getDateOfBirth());
        }

        if (request.getGender() != null) {
            beneficiary.setGender(request.getGender());
        }

        // --------------------------------------------------------
        // Mobile
        // --------------------------------------------------------

        if (request.getMobileNumber() != null) {

            if (!request.getMobileNumber().equals(beneficiary.getMobileNumber())
                    && beneficiaryRepository.existsByMobileNumber(
                    request.getMobileNumber())) {

                throw new DuplicateMobileException(
                        request.getMobileNumber());
            }

            beneficiary.setMobileNumber(request.getMobileNumber());

            // Keep legacy contact synchronized
            beneficiary.setContact(request.getMobileNumber());
        }

        // --------------------------------------------------------
        // Email
        // --------------------------------------------------------

        if (request.getEmail() != null) {
            beneficiary.setEmail(request.getEmail());
        }

        // --------------------------------------------------------
        // Address
        // --------------------------------------------------------

        if (request.getAddress() != null) {
            beneficiary.setAddress(request.getAddress());
        }

        if (request.getVillage() != null) {
            beneficiary.setVillage(request.getVillage());
        }

        if (request.getTaluka() != null) {
            beneficiary.setTaluka(request.getTaluka());
        }

        if (request.getDistrict() != null) {
            beneficiary.setDistrict(request.getDistrict());
        }

        if (request.getState() != null) {
            beneficiary.setState(request.getState());
        }

        if (request.getPinCode() != null) {
            beneficiary.setPinCode(request.getPinCode());
        }

        // --------------------------------------------------------
        // Eligibility fields
        // --------------------------------------------------------

        if (request.getAnnualIncome() != null) {
            beneficiary.setAnnualIncome(request.getAnnualIncome());
        }

        if (request.getLandHolding() != null) {
            beneficiary.setLandHolding(request.getLandHolding());
        }

        if (request.getCategory() != null) {
            beneficiary.setCategory(request.getCategory());
        }

        /*
         * Government ID / Aadhaar is intentionally NOT updated.
         */

        beneficiaryRepository.save(beneficiary);

        /*
         * Return the managed entity instead of relying on save()
         * returning a value. This also makes the method robust when
         * mocked in unit tests.
         */
        return mapToResponse(beneficiary);
    }

    // ============================================================
    // DELETE
    // ============================================================

    @Override
    public void deleteBeneficiary(Integer id) {

        Beneficiary beneficiary = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new BeneficiaryNotFoundException(id));

        beneficiaryRepository.delete(beneficiary);
    }

    // ============================================================
    // ENTITY → RESPONSE
    // ============================================================

    private BeneficiaryResponse mapToResponse(Beneficiary beneficiary) {

        return BeneficiaryResponse.builder()
                // Original fields
                .id(beneficiary.getId())
                .fullName(beneficiary.getFullName())
                .govId(beneficiary.getGovId())
                .contact(beneficiary.getContact())
                .email(beneficiary.getEmail())
                .age(beneficiary.getAge())
                .address(beneficiary.getAddress())
                .schemeName(beneficiary.getSchemeName())

                // Extended identity fields
                .aadhaarNumber(beneficiary.getAadhaarNumber())
                .mobileNumber(beneficiary.getMobileNumber())
                .firstName(beneficiary.getFirstName())
                .lastName(beneficiary.getLastName())
                .dateOfBirth(beneficiary.getDateOfBirth())
                .gender(beneficiary.getGender())

                // Structured address
                .village(beneficiary.getVillage())
                .taluka(beneficiary.getTaluka())
                .district(beneficiary.getDistrict())
                .state(beneficiary.getState())
                .pinCode(beneficiary.getPinCode())

                // Eligibility fields
                .annualIncome(beneficiary.getAnnualIncome())
                .landHolding(beneficiary.getLandHolding())
                .category(beneficiary.getCategory())

                // Lifecycle fields
                .registrationStatus(beneficiary.getRegistrationStatus())
                .identityVerified(beneficiary.isIdentityVerified())

                .build();
    }
}