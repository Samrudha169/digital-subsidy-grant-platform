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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link BeneficiaryService}.
 *
 * <p>Milestone 1 behaviour is preserved exactly:
 * <ul>
 *   <li>Duplicate govId → {@link DuplicateAadhaarException} (409)</li>
 *   <li>Duplicate contact → {@link DuplicateMobileException} (409)</li>
 *   <li>Unknown id → {@link BeneficiaryNotFoundException} (404)</li>
 * </ul>
 *
 * <p>Milestone 2 additions:
 * <ul>
 *   <li>{@link #registerBeneficiary} now also persists the extended eligibility
 *       fields (annualIncome, landHolding, category, gender, dateOfBirth,
 *       address components, aadhaarNumber, mobileNumber) when provided.</li>
 *   <li>A second {@link #updateBeneficiary(Integer, BeneficiaryUpdateRequest)}
 *       overload supports patch-style updates with the extended field set.</li>
 *   <li>{@link #mapToResponse} returns the full extended profile.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BeneficiaryServiceImpl implements BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;

    // ── Registration ─────────────────────────────────────────────────────────

    @Override
    public BeneficiaryResponse registerBeneficiary(
            BeneficiaryRegistrationRequest request) {

        // Duplicate checks (Milestone 1 behaviour — preserved)
        if (beneficiaryRepository.existsByGovId(request.getGovId())) {
            throw new DuplicateAadhaarException(request.getGovId());
        }
        if (beneficiaryRepository.existsByContact(request.getContact())) {
            throw new DuplicateMobileException(request.getContact());
        }

        // Duplicate check for optional Aadhaar number (Milestone 2)
        if (request.getAadhaarNumber() != null
                && beneficiaryRepository.existsByAadhaarNumber(request.getAadhaarNumber())) {
            throw new DuplicateAadhaarException(request.getAadhaarNumber());
        }

        // Duplicate check for optional mobile number (Milestone 2)
        if (request.getMobileNumber() != null
                && beneficiaryRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new DuplicateMobileException(request.getMobileNumber());
        }

        Beneficiary beneficiary = Beneficiary.builder()
                // ── Original fields ──────────────────────────────────────────
                .fullName(request.getFullName())
                .govId(request.getGovId())
                .contact(request.getContact())
                .email(request.getEmail())
                .age(request.getAge())
                .address(request.getAddress())
                .schemeName(request.getSchemeName())
                // ── Extended eligibility fields (may be null) ────────────────
                .aadhaarNumber(request.getAadhaarNumber())
                .mobileNumber(request.getMobileNumber())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .village(request.getVillage())
                .taluka(request.getTaluka())
                .district(request.getDistrict())
                .state(request.getState())
                .pinCode(request.getPinCode())
                .annualIncome(request.getAnnualIncome())
                .landHolding(request.getLandHolding())
                .category(request.getCategory())
                .build();

        return mapToResponse(beneficiaryRepository.save(beneficiary));
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public BeneficiaryResponse getBeneficiaryById(Integer id) {
        return mapToResponse(
                beneficiaryRepository.findById(id)
                        .orElseThrow(() -> new BeneficiaryNotFoundException(id))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public BeneficiaryResponse getBeneficiaryByGovId(String govId) {
        return mapToResponse(
                beneficiaryRepository.findByGovId(govId)
                        .orElseThrow(() -> new BeneficiaryNotFoundException(
                                "Beneficiary not found with Government ID: " + govId))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getAllBeneficiaries() {
        return beneficiaryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ── Update (Milestone 1 path — BeneficiaryRegistrationRequest) ──────────

    /**
     * Milestone 1 update path. Retained for backward compatibility.
     * Applies the seven original fields; extended fields are left unchanged.
     */
    @Override
    public BeneficiaryResponse updateBeneficiary(
            Integer id,
            BeneficiaryRegistrationRequest request) {

        Beneficiary beneficiary = findOrThrow(id);

        // Duplicate govId check (skip if unchanged)
        if (!request.getGovId().equals(beneficiary.getGovId())
                && beneficiaryRepository.existsByGovId(request.getGovId())) {
            throw new DuplicateAadhaarException(request.getGovId());
        }

        // Duplicate contact check (skip if unchanged)
        if (!request.getContact().equals(beneficiary.getContact())
                && beneficiaryRepository.existsByContact(request.getContact())) {
            throw new DuplicateMobileException(request.getContact());
        }

        // Update legacy fields
        beneficiary.setFullName(request.getFullName());
        beneficiary.setGovId(request.getGovId());
        beneficiary.setContact(request.getContact());
        beneficiary.setEmail(request.getEmail());
        beneficiary.setAge(request.getAge());
        beneficiary.setAddress(request.getAddress());
        beneficiary.setSchemeName(request.getSchemeName());

        // Also apply any extended fields provided in this request
        applyExtendedFields(beneficiary, request);

        return mapToResponse(beneficiaryRepository.save(beneficiary));
    }

    // ── Update (Milestone 2 path — BeneficiaryUpdateRequest, patch-style) ───

    /**
     * Milestone 2 update path. Only non-null fields in the request are applied.
     * Aadhaar number cannot be updated after registration.
     */
    @Override
    public BeneficiaryResponse updateBeneficiary(
            Integer id,
            BeneficiaryUpdateRequest request) {

        Beneficiary beneficiary = findOrThrow(id);

        // Duplicate mobile check (skip if unchanged or null)
        if (request.getMobileNumber() != null
                && !request.getMobileNumber().equals(beneficiary.getMobileNumber())
                && beneficiaryRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new DuplicateMobileException(request.getMobileNumber());
        }

        // Apply only non-null fields (patch semantics)
        if (request.getFirstName()    != null) beneficiary.setFirstName(request.getFirstName());
        if (request.getLastName()     != null) beneficiary.setLastName(request.getLastName());
        if (request.getDateOfBirth()  != null) beneficiary.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender()       != null) beneficiary.setGender(request.getGender());
        if (request.getMobileNumber() != null) beneficiary.setMobileNumber(request.getMobileNumber());
        if (request.getEmail()        != null) beneficiary.setEmail(request.getEmail());
        if (request.getAddress()      != null) beneficiary.setAddress(request.getAddress());
        if (request.getVillage()      != null) beneficiary.setVillage(request.getVillage());
        if (request.getTaluka()       != null) beneficiary.setTaluka(request.getTaluka());
        if (request.getDistrict()     != null) beneficiary.setDistrict(request.getDistrict());
        if (request.getState()        != null) beneficiary.setState(request.getState());
        if (request.getPinCode()      != null) beneficiary.setPinCode(request.getPinCode());
        if (request.getAnnualIncome() != null) beneficiary.setAnnualIncome(request.getAnnualIncome());
        if (request.getLandHolding()  != null) beneficiary.setLandHolding(request.getLandHolding());
        if (request.getCategory()     != null) beneficiary.setCategory(request.getCategory());

        return mapToResponse(beneficiaryRepository.save(beneficiary));
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Override
    public void deleteBeneficiary(Integer id) {
        beneficiaryRepository.delete(findOrThrow(id));
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Beneficiary findOrThrow(Integer id) {
        return beneficiaryRepository.findById(id)
                .orElseThrow(() -> new BeneficiaryNotFoundException(id));
    }

    /**
     * Applies extended eligibility fields from a registration request onto an
     * existing beneficiary entity. Only non-null values are written so that
     * existing extended data is not overwritten with null if a legacy-format
     * request is sent.
     */
    private void applyExtendedFields(Beneficiary beneficiary,
                                     BeneficiaryRegistrationRequest request) {
        if (request.getAadhaarNumber() != null) beneficiary.setAadhaarNumber(request.getAadhaarNumber());
        if (request.getMobileNumber()  != null) beneficiary.setMobileNumber(request.getMobileNumber());
        if (request.getDateOfBirth()   != null) beneficiary.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender()        != null) beneficiary.setGender(request.getGender());
        if (request.getVillage()       != null) beneficiary.setVillage(request.getVillage());
        if (request.getTaluka()        != null) beneficiary.setTaluka(request.getTaluka());
        if (request.getDistrict()      != null) beneficiary.setDistrict(request.getDistrict());
        if (request.getState()         != null) beneficiary.setState(request.getState());
        if (request.getPinCode()       != null) beneficiary.setPinCode(request.getPinCode());
        if (request.getAnnualIncome()  != null) beneficiary.setAnnualIncome(request.getAnnualIncome());
        if (request.getLandHolding()   != null) beneficiary.setLandHolding(request.getLandHolding());
        if (request.getCategory()      != null) beneficiary.setCategory(request.getCategory());
    }

    /**
     * Maps a {@link Beneficiary} entity to a full {@link BeneficiaryResponse}.
     * Returns all original and extended fields; new fields may be null for
     * records registered before Milestone 2.
     */
    private BeneficiaryResponse mapToResponse(Beneficiary b) {
        return BeneficiaryResponse.builder()
                // ── Original fields ──────────────────────────────────────────
                .id(b.getId())
                .fullName(b.getFullName())
                .govId(b.getGovId())
                .contact(b.getContact())
                .email(b.getEmail())
                .age(b.getAge())
                .address(b.getAddress())
                .schemeName(b.getSchemeName())
                // ── Extended eligibility fields ──────────────────────────────
                .aadhaarNumber(b.getAadhaarNumber())
                .mobileNumber(b.getMobileNumber())
                .firstName(b.getFirstName())
                .lastName(b.getLastName())
                .dateOfBirth(b.getDateOfBirth())
                .gender(b.getGender())
                .village(b.getVillage())
                .taluka(b.getTaluka())
                .district(b.getDistrict())
                .state(b.getState())
                .pinCode(b.getPinCode())
                .annualIncome(b.getAnnualIncome())
                .landHolding(b.getLandHolding())
                .category(b.getCategory())
                .registrationStatus(b.getRegistrationStatus())
                .identityVerified(b.isIdentityVerified())
                .build();
    }
}