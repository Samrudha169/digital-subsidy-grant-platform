package com.dsgp.beneficiary.service;

import com.dsgp.beneficiary.dto.BeneficiaryRegistrationRequest;
import com.dsgp.beneficiary.dto.BeneficiaryResponse;
import com.dsgp.beneficiary.dto.BeneficiaryUpdateRequest;
import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.Category;
import com.dsgp.beneficiary.entity.Gender;
import com.dsgp.beneficiary.entity.RegistrationStatus;
import com.dsgp.beneficiary.exception.BeneficiaryNotFoundException;
import com.dsgp.beneficiary.exception.DuplicateAadhaarException;
import com.dsgp.beneficiary.exception.DuplicateMobileException;
import com.dsgp.beneficiary.repository.BeneficiaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link BeneficiaryServiceImpl}.
 *
 * <p>Uses Mockito to isolate the service from the database.
 * Covers Milestone 1 (CRUD behaviour, duplicate checks, not-found)
 * and Milestone 2 (extended eligibility field persistence, patch update).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BeneficiaryServiceImpl")
class BeneficiaryServiceImplTest {

    @Mock
    private BeneficiaryRepository repository;

    @InjectMocks
    private BeneficiaryServiceImpl service;

    // ── Shared test data ─────────────────────────────────────────────────────

    private BeneficiaryRegistrationRequest legacyRequest;
    private BeneficiaryRegistrationRequest extendedRequest;
    private Beneficiary savedBeneficiary;

    @BeforeEach
    void setUp() {
        // Minimal 7-field legacy request (Milestone 1 format)
        legacyRequest = new BeneficiaryRegistrationRequest();
        legacyRequest.setFullName("Ravi Kumar");
        legacyRequest.setGovId("AABCD1234E");
        legacyRequest.setContact("9876543210");
        legacyRequest.setEmail("ravi@example.com");
        legacyRequest.setAge(38);
        legacyRequest.setAddress("Village Uruli Kanchan, Pune");
        legacyRequest.setSchemeName("PM-KISAN");

        // Extended request with eligibility fields (Milestone 2 format)
        extendedRequest = new BeneficiaryRegistrationRequest();
        extendedRequest.setFullName("Sita Devi");
        extendedRequest.setGovId("BBACD9876F");
        extendedRequest.setContact("8765432109");
        extendedRequest.setEmail("sita@example.com");
        extendedRequest.setAge(30);
        extendedRequest.setAddress("Village Wai, Satara");
        extendedRequest.setSchemeName("NSP");
        extendedRequest.setAadhaarNumber("123456789012");
        extendedRequest.setMobileNumber("7654321098");
        extendedRequest.setDateOfBirth(LocalDate.of(1994, 6, 15));
        extendedRequest.setGender(Gender.FEMALE);
        extendedRequest.setVillage("Wai");
        extendedRequest.setTaluka("Wai");
        extendedRequest.setDistrict("Satara");
        extendedRequest.setState("Maharashtra");
        extendedRequest.setPinCode("412803");
        extendedRequest.setAnnualIncome(new BigDecimal("85000.00"));
        extendedRequest.setLandHolding(new BigDecimal("1.5"));
        extendedRequest.setCategory(Category.OBC);

        // A saved entity returned by the mock repository
        savedBeneficiary = Beneficiary.builder()
                .id(1)
                .fullName("Ravi Kumar")
                .govId("AABCD1234E")
                .contact("9876543210")
                .email("ravi@example.com")
                .age(38)
                .address("Village Uruli Kanchan, Pune")
                .schemeName("PM-KISAN")
                .registrationStatus(RegistrationStatus.PENDING)
                .identityVerified(false)
                .build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // registerBeneficiary
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("registerBeneficiary()")
    class RegisterBeneficiary {

        @Test
        @DisplayName("persists legacy 7-field request successfully")
        void register_legacyRequest_success() {
            given(repository.existsByGovId("AABCD1234E")).willReturn(false);
            given(repository.existsByContact("9876543210")).willReturn(false);
            given(repository.save(any(Beneficiary.class))).willReturn(savedBeneficiary);

            BeneficiaryResponse response = service.registerBeneficiary(legacyRequest);

            assertThat(response.getId()).isEqualTo(1);
            assertThat(response.getFullName()).isEqualTo("Ravi Kumar");
            assertThat(response.getGovId()).isEqualTo("AABCD1234E");
            then(repository).should().save(any(Beneficiary.class));
        }

        @Test
        @DisplayName("persists extended eligibility fields when provided")
        void register_extendedRequest_persistsAllFields() {
            given(repository.existsByGovId("BBACD9876F")).willReturn(false);
            given(repository.existsByContact("8765432109")).willReturn(false);
            given(repository.existsByAadhaarNumber("123456789012")).willReturn(false);
            given(repository.existsByMobileNumber("7654321098")).willReturn(false);

            Beneficiary extendedSaved = Beneficiary.builder()
                    .id(2)
                    .fullName("Sita Devi")
                    .govId("BBACD9876F")
                    .contact("8765432109")
                    .email("sita@example.com")
                    .age(30)
                    .address("Village Wai, Satara")
                    .schemeName("NSP")
                    .aadhaarNumber("123456789012")
                    .mobileNumber("7654321098")
                    .dateOfBirth(LocalDate.of(1994, 6, 15))
                    .gender(Gender.FEMALE)
                    .village("Wai")
                    .taluka("Wai")
                    .district("Satara")
                    .state("Maharashtra")
                    .pinCode("412803")
                    .annualIncome(new BigDecimal("85000.00"))
                    .landHolding(new BigDecimal("1.5"))
                    .category(Category.OBC)
                    .registrationStatus(RegistrationStatus.PENDING)
                    .identityVerified(false)
                    .build();

            given(repository.save(any(Beneficiary.class))).willReturn(extendedSaved);

            BeneficiaryResponse response = service.registerBeneficiary(extendedRequest);

            assertThat(response.getAadhaarNumber()).isEqualTo("123456789012");
            assertThat(response.getMobileNumber()).isEqualTo("7654321098");
            assertThat(response.getGender()).isEqualTo(Gender.FEMALE);
            assertThat(response.getDistrict()).isEqualTo("Satara");
            assertThat(response.getAnnualIncome()).isEqualByComparingTo("85000.00");
            assertThat(response.getLandHolding()).isEqualByComparingTo("1.5");
            assertThat(response.getCategory()).isEqualTo(Category.OBC);
            assertThat(response.getRegistrationStatus()).isEqualTo(RegistrationStatus.PENDING);
            assertThat(response.isIdentityVerified()).isFalse();
        }

        @Test
        @DisplayName("throws DuplicateAadhaarException when govId already exists")
        void register_duplicateGovId_throws() {
            given(repository.existsByGovId("AABCD1234E")).willReturn(true);

            assertThatThrownBy(() -> service.registerBeneficiary(legacyRequest))
                    .isInstanceOf(DuplicateAadhaarException.class);

            then(repository).should(never()).save(any());
        }

        @Test
        @DisplayName("throws DuplicateMobileException when contact already exists")
        void register_duplicateContact_throws() {
            given(repository.existsByGovId("AABCD1234E")).willReturn(false);
            given(repository.existsByContact("9876543210")).willReturn(true);

            assertThatThrownBy(() -> service.registerBeneficiary(legacyRequest))
                    .isInstanceOf(DuplicateMobileException.class);

            then(repository).should(never()).save(any());
        }

        @Test
        @DisplayName("throws DuplicateAadhaarException when aadhaarNumber already exists")
        void register_duplicateAadhaar_throws() {
            given(repository.existsByGovId("BBACD9876F")).willReturn(false);
            given(repository.existsByContact("8765432109")).willReturn(false);
            given(repository.existsByAadhaarNumber("123456789012")).willReturn(true);

            assertThatThrownBy(() -> service.registerBeneficiary(extendedRequest))
                    .isInstanceOf(DuplicateAadhaarException.class);

            then(repository).should(never()).save(any());
        }

        @Test
        @DisplayName("throws DuplicateMobileException when mobileNumber already exists")
        void register_duplicateMobile_throws() {
            given(repository.existsByGovId("BBACD9876F")).willReturn(false);
            given(repository.existsByContact("8765432109")).willReturn(false);
            given(repository.existsByAadhaarNumber("123456789012")).willReturn(false);
            given(repository.existsByMobileNumber("7654321098")).willReturn(true);

            assertThatThrownBy(() -> service.registerBeneficiary(extendedRequest))
                    .isInstanceOf(DuplicateMobileException.class);

            then(repository).should(never()).save(any());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // getBeneficiaryById
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getBeneficiaryById()")
    class GetById {

        @Test
        @DisplayName("returns response when beneficiary exists")
        void getById_found_returnsResponse() {
            given(repository.findById(1)).willReturn(Optional.of(savedBeneficiary));

            BeneficiaryResponse response = service.getBeneficiaryById(1);

            assertThat(response.getId()).isEqualTo(1);
            assertThat(response.getFullName()).isEqualTo("Ravi Kumar");
        }

        @Test
        @DisplayName("throws BeneficiaryNotFoundException when id not found")
        void getById_notFound_throws() {
            given(repository.findById(99)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getBeneficiaryById(99))
                    .isInstanceOf(BeneficiaryNotFoundException.class);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // getBeneficiaryByGovId
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getBeneficiaryByGovId()")
    class GetByGovId {

        @Test
        @DisplayName("returns response when govId matches")
        void getByGovId_found_returnsResponse() {
            given(repository.findByGovId("AABCD1234E"))
                    .willReturn(Optional.of(savedBeneficiary));

            BeneficiaryResponse response = service.getBeneficiaryByGovId("AABCD1234E");

            assertThat(response.getGovId()).isEqualTo("AABCD1234E");
        }

        @Test
        @DisplayName("throws BeneficiaryNotFoundException when govId not found")
        void getByGovId_notFound_throws() {
            given(repository.findByGovId("UNKNOWN")).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getBeneficiaryByGovId("UNKNOWN"))
                    .isInstanceOf(BeneficiaryNotFoundException.class);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // getAllBeneficiaries
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAllBeneficiaries()")
    class GetAll {

        @Test
        @DisplayName("returns mapped list of all beneficiaries")
        void getAll_returnsList() {
            given(repository.findAll()).willReturn(List.of(savedBeneficiary));

            List<BeneficiaryResponse> responses = service.getAllBeneficiaries();

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getFullName()).isEqualTo("Ravi Kumar");
        }

        @Test
        @DisplayName("returns empty list when no beneficiaries exist")
        void getAll_empty_returnsEmptyList() {
            given(repository.findAll()).willReturn(List.of());

            assertThat(service.getAllBeneficiaries()).isEmpty();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // updateBeneficiary — legacy path (BeneficiaryRegistrationRequest)
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateBeneficiary(id, BeneficiaryRegistrationRequest)")
    class UpdateLegacy {

        @Test
        @DisplayName("updates legacy fields successfully")
        void update_legacy_success() {
            // govId and contact are unchanged in legacyRequest vs savedBeneficiary,
            // so the duplicate-check guards are skipped — no existsBy stubs needed.
            given(repository.findById(1)).willReturn(Optional.of(savedBeneficiary));
            given(repository.save(any(Beneficiary.class))).willReturn(savedBeneficiary);

            BeneficiaryResponse response = service.updateBeneficiary(1, legacyRequest);

            assertThat(response.getFullName()).isEqualTo("Ravi Kumar");
        }

        @Test
        @DisplayName("throws BeneficiaryNotFoundException when id not found")
        void update_notFound_throws() {
            given(repository.findById(99)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateBeneficiary(99, legacyRequest))
                    .isInstanceOf(BeneficiaryNotFoundException.class);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // updateBeneficiary — Milestone 2 patch path (BeneficiaryUpdateRequest)
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateBeneficiary(id, BeneficiaryUpdateRequest)")
    class UpdatePatch {

        @Test
        @DisplayName("applies only non-null fields in patch update")
        void update_patch_appliesOnlyNonNull() {
            BeneficiaryUpdateRequest patchReq = new BeneficiaryUpdateRequest();
            patchReq.setAnnualIncome(new BigDecimal("120000.00"));
            patchReq.setCategory(Category.SC);

            Beneficiary saved = Beneficiary.builder()
                    .id(1)
                    .fullName("Ravi Kumar")
                    .govId("AABCD1234E")
                    .contact("9876543210")
                    .email("ravi@example.com")
                    .age(38)
                    .address("Village Uruli Kanchan, Pune")
                    .schemeName("PM-KISAN")
                    .annualIncome(new BigDecimal("120000.00"))
                    .category(Category.SC)
                    .registrationStatus(RegistrationStatus.PENDING)
                    .identityVerified(false)
                    .build();

            given(repository.findById(1)).willReturn(Optional.of(savedBeneficiary));
            given(repository.save(any(Beneficiary.class))).willReturn(saved);

            BeneficiaryResponse response = service.updateBeneficiary(1, patchReq);

            assertThat(response.getAnnualIncome()).isEqualByComparingTo("120000.00");
            assertThat(response.getCategory()).isEqualTo(Category.SC);
            // Original fields should still be present
            assertThat(response.getFullName()).isEqualTo("Ravi Kumar");
        }

        @Test
        @DisplayName("throws DuplicateMobileException when new mobile already taken")
        void update_patch_duplicateMobile_throws() {
            BeneficiaryUpdateRequest patchReq = new BeneficiaryUpdateRequest();
            patchReq.setMobileNumber("9999999999");

            savedBeneficiary.setMobileNumber("8888888888"); // different from request
            given(repository.findById(1)).willReturn(Optional.of(savedBeneficiary));
            given(repository.existsByMobileNumber("9999999999")).willReturn(true);

            assertThatThrownBy(() -> service.updateBeneficiary(1, patchReq))
                    .isInstanceOf(DuplicateMobileException.class);
        }

        @Test
        @DisplayName("throws BeneficiaryNotFoundException when id not found")
        void update_patch_notFound_throws() {
            given(repository.findById(99)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateBeneficiary(99, new BeneficiaryUpdateRequest()))
                    .isInstanceOf(BeneficiaryNotFoundException.class);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // deleteBeneficiary
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteBeneficiary()")
    class Delete {

        @Test
        @DisplayName("deletes beneficiary when it exists")
        void delete_existing_success() {
            given(repository.findById(1)).willReturn(Optional.of(savedBeneficiary));

            service.deleteBeneficiary(1);

            then(repository).should().delete(savedBeneficiary);
        }

        @Test
        @DisplayName("throws BeneficiaryNotFoundException when id not found")
        void delete_notFound_throws() {
            given(repository.findById(99)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteBeneficiary(99))
                    .isInstanceOf(BeneficiaryNotFoundException.class);
        }
    }
}
