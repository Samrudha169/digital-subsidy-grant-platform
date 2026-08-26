package com.dsgp.beneficiary.service;

import com.dsgp.beneficiary.dto.*;
import com.dsgp.beneficiary.entity.*;
import com.dsgp.beneficiary.exception.*;
import com.dsgp.beneficiary.repository.BeneficiaryDocumentRepository;
import com.dsgp.beneficiary.repository.BeneficiaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BeneficiaryServiceImpl}.
 *
 * <p>Uses Mockito for all external dependencies. No Spring context is loaded;
 * no database is required.
 */
@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceImplTest {

    @Mock
    private BeneficiaryRepository beneficiaryRepository;

    @Mock
    private BeneficiaryDocumentRepository documentRepository;

    @InjectMocks
    private BeneficiaryServiceImpl service;

    // ── Test Fixtures ──────────────────────────────────────────────────────────

    private BeneficiaryRegistrationRequest validRequest;
    private Beneficiary savedBeneficiary;

    @BeforeEach
    void setUp() {
        // Point upload dir to a temp location that exists
        ReflectionTestUtils.setField(service, "uploadDir", System.getProperty("java.io.tmpdir"));

        validRequest = new BeneficiaryRegistrationRequest();
        validRequest.setFirstName("Ramesh");
        validRequest.setLastName("Kumar");
        validRequest.setDateOfBirth(LocalDate.of(1990, 5, 15));
        validRequest.setGender(Gender.MALE);
        validRequest.setAadhaarNumber("123456789012");
        validRequest.setMobileNumber("9876543210");
        validRequest.setEmail("ramesh@example.com");
        validRequest.setDistrict("Pune");
        validRequest.setState("Maharashtra");
        validRequest.setCategory(Category.OBC);
        validRequest.setCreatedBy("officer1");

        savedBeneficiary = Beneficiary.builder()
                .id(1L)
                .firstName("Ramesh")
                .lastName("Kumar")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .gender(Gender.MALE)
                .aadhaarNumber("123456789012")
                .mobileNumber("9876543210")
                .email("ramesh@example.com")
                .district("Pune")
                .state("Maharashtra")
                .category(Category.OBC)
                .registrationStatus(RegistrationStatus.PENDING)
                .registrationDate(LocalDateTime.now())
                .createdBy("officer1")
                .identityVerified(false)
                .build();
    }

    // ── registerBeneficiary ────────────────────────────────────────────────────

    @Test
    @DisplayName("registerBeneficiary: valid request → saves and returns response")
    void registerBeneficiary_success() {
        when(beneficiaryRepository.existsByAadhaarNumber("123456789012")).thenReturn(false);
        when(beneficiaryRepository.existsByMobileNumber("9876543210")).thenReturn(false);
        when(beneficiaryRepository.save(any(Beneficiary.class))).thenReturn(savedBeneficiary);

        BeneficiaryResponse response = service.registerBeneficiary(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFirstName()).isEqualTo("Ramesh");
        assertThat(response.getAadhaarNumber()).isEqualTo("123456789012");
        assertThat(response.getRegistrationStatus()).isEqualTo(RegistrationStatus.PENDING);
        verify(beneficiaryRepository).save(any(Beneficiary.class));
    }

    @Test
    @DisplayName("registerBeneficiary: duplicate Aadhaar → throws DuplicateAadhaarException")
    void registerBeneficiary_duplicateAadhaar_throwsException() {
        when(beneficiaryRepository.existsByAadhaarNumber("123456789012")).thenReturn(true);

        assertThatThrownBy(() -> service.registerBeneficiary(validRequest))
                .isInstanceOf(DuplicateAadhaarException.class)
                .hasMessageContaining("already registered");

        verify(beneficiaryRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerBeneficiary: duplicate mobile → throws DuplicateMobileException")
    void registerBeneficiary_duplicateMobile_throwsException() {
        when(beneficiaryRepository.existsByAadhaarNumber("123456789012")).thenReturn(false);
        when(beneficiaryRepository.existsByMobileNumber("9876543210")).thenReturn(true);

        assertThatThrownBy(() -> service.registerBeneficiary(validRequest))
                .isInstanceOf(DuplicateMobileException.class)
                .hasMessageContaining("9876543210");

        verify(beneficiaryRepository, never()).save(any());
    }

    // ── getBeneficiaryById ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getBeneficiaryById: existing ID → returns correct response")
    void getBeneficiaryById_found() {
        when(beneficiaryRepository.findById(1L)).thenReturn(Optional.of(savedBeneficiary));

        BeneficiaryResponse response = service.getBeneficiaryById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getLastName()).isEqualTo("Kumar");
    }

    @Test
    @DisplayName("getBeneficiaryById: non-existent ID → throws BeneficiaryNotFoundException")
    void getBeneficiaryById_notFound_throwsException() {
        when(beneficiaryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBeneficiaryById(99L))
                .isInstanceOf(BeneficiaryNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── getBeneficiaryByAadhaar ────────────────────────────────────────────────

    @Test
    @DisplayName("getBeneficiaryByAadhaar: matching Aadhaar → returns correct response")
    void getBeneficiaryByAadhaar_found() {
        when(beneficiaryRepository.findByAadhaarNumber("123456789012"))
                .thenReturn(Optional.of(savedBeneficiary));

        BeneficiaryResponse response = service.getBeneficiaryByAadhaar("123456789012");

        assertThat(response.getAadhaarNumber()).isEqualTo("123456789012");
    }

    @Test
    @DisplayName("getBeneficiaryByAadhaar: unknown Aadhaar → throws BeneficiaryNotFoundException")
    void getBeneficiaryByAadhaar_notFound_throwsException() {
        when(beneficiaryRepository.findByAadhaarNumber("000000000000"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBeneficiaryByAadhaar("000000000000"))
                .isInstanceOf(BeneficiaryNotFoundException.class);
    }

    // ── updateBeneficiary ──────────────────────────────────────────────────────

    @Test
    @DisplayName("updateBeneficiary: new mobile already in use → throws DuplicateMobileException")
    void updateBeneficiary_mobileAlreadyInUse_throwsException() {
        when(beneficiaryRepository.findById(1L)).thenReturn(Optional.of(savedBeneficiary));
        when(beneficiaryRepository.existsByMobileNumber("8888888888")).thenReturn(true);

        BeneficiaryUpdateRequest updateRequest = new BeneficiaryUpdateRequest();
        updateRequest.setMobileNumber("8888888888");

        assertThatThrownBy(() -> service.updateBeneficiary(1L, updateRequest))
                .isInstanceOf(DuplicateMobileException.class);
    }

    @Test
    @DisplayName("updateBeneficiary: partial fields update → only provided fields change")
    void updateBeneficiary_partialUpdate_success() {
        when(beneficiaryRepository.findById(1L)).thenReturn(Optional.of(savedBeneficiary));
        when(beneficiaryRepository.save(any(Beneficiary.class))).thenReturn(savedBeneficiary);

        BeneficiaryUpdateRequest updateRequest = new BeneficiaryUpdateRequest();
        updateRequest.setDistrict("Nashik");

        service.updateBeneficiary(1L, updateRequest);

        verify(beneficiaryRepository).save(argThat(b -> "Nashik".equals(b.getDistrict())));
    }

    // ── updateRegistrationStatus ───────────────────────────────────────────────

    @Test
    @DisplayName("updateRegistrationStatus: PENDING → ACTIVE → status changes correctly")
    void updateRegistrationStatus_pendingToActive() {
        when(beneficiaryRepository.findById(1L)).thenReturn(Optional.of(savedBeneficiary));
        Beneficiary activeBeneficiary = Beneficiary.builder()
                .id(1L).firstName("Ramesh").lastName("Kumar")
                .aadhaarNumber("123456789012").mobileNumber("9876543210")
                .district("Pune").state("Maharashtra").category(Category.OBC)
                .gender(Gender.MALE).dateOfBirth(LocalDate.of(1990, 5, 15))
                .registrationStatus(RegistrationStatus.ACTIVE)
                .registrationDate(LocalDateTime.now()).identityVerified(false).build();
        when(beneficiaryRepository.save(any(Beneficiary.class))).thenReturn(activeBeneficiary);

        BeneficiaryResponse response = service.updateRegistrationStatus(1L, RegistrationStatus.ACTIVE);

        assertThat(response.getRegistrationStatus()).isEqualTo(RegistrationStatus.ACTIVE);
    }

    // ── verifyIdentity ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("verifyIdentity: existing beneficiary → identityVerified becomes true")
    void verifyIdentity_setsIdentityVerifiedTrue() {
        when(beneficiaryRepository.findById(1L)).thenReturn(Optional.of(savedBeneficiary));
        Beneficiary verifiedBeneficiary = Beneficiary.builder()
                .id(1L).firstName("Ramesh").lastName("Kumar")
                .aadhaarNumber("123456789012").mobileNumber("9876543210")
                .district("Pune").state("Maharashtra").category(Category.OBC)
                .gender(Gender.MALE).dateOfBirth(LocalDate.of(1990, 5, 15))
                .registrationStatus(RegistrationStatus.PENDING)
                .registrationDate(LocalDateTime.now()).identityVerified(true).build();
        when(beneficiaryRepository.save(any(Beneficiary.class))).thenReturn(verifiedBeneficiary);

        BeneficiaryResponse response = service.verifyIdentity(1L);

        assertThat(response.isIdentityVerified()).isTrue();
        verify(beneficiaryRepository).save(argThat(Beneficiary::isIdentityVerified));
    }

    @Test
    @DisplayName("verifyIdentity: non-existent ID → throws BeneficiaryNotFoundException")
    void verifyIdentity_notFound_throwsException() {
        when(beneficiaryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyIdentity(99L))
                .isInstanceOf(BeneficiaryNotFoundException.class);
    }

    // ── deleteBeneficiary (soft delete) ───────────────────────────────────────

    @Test
    @DisplayName("deleteBeneficiary: sets status to SUSPENDED (soft delete)")
    void deleteBeneficiary_setsSuspended() {
        when(beneficiaryRepository.findById(1L)).thenReturn(Optional.of(savedBeneficiary));
        when(beneficiaryRepository.save(any(Beneficiary.class))).thenReturn(savedBeneficiary);

        service.deleteBeneficiary(1L);

        verify(beneficiaryRepository).save(argThat(
                b -> b.getRegistrationStatus() == RegistrationStatus.SUSPENDED));
    }

    // ── getBeneficiariesByStatus ───────────────────────────────────────────────

    @Test
    @DisplayName("getBeneficiariesByStatus: returns page of matching beneficiaries")
    void getBeneficiariesByStatus_returnsFilteredPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Beneficiary> page = new PageImpl<>(List.of(savedBeneficiary));
        when(beneficiaryRepository.findByRegistrationStatus(RegistrationStatus.PENDING, pageable))
                .thenReturn(page);

        Page<BeneficiarySummaryResponse> result =
                service.getBeneficiariesByStatus(RegistrationStatus.PENDING, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getRegistrationStatus())
                .isEqualTo(RegistrationStatus.PENDING);
    }

    // ── uploadDocument ────────────────────────────────────────────────────────

    @Test
    @DisplayName("uploadDocument: empty file → throws DocumentUploadException")
    void uploadDocument_emptyFile_throwsException() {
        when(beneficiaryRepository.findById(1L)).thenReturn(Optional.of(savedBeneficiary));
        when(documentRepository.existsByBeneficiaryIdAndDocumentType(anyLong(), any()))
                .thenReturn(false);

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.uploadDocument(1L, "AADHAAR", emptyFile, "officer1"))
                .isInstanceOf(DocumentUploadException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("uploadDocument: unsupported file type → throws DocumentUploadException")
    void uploadDocument_invalidFileType_throwsException() {
        when(beneficiaryRepository.findById(1L)).thenReturn(Optional.of(savedBeneficiary));
        when(documentRepository.existsByBeneficiaryIdAndDocumentType(anyLong(), any()))
                .thenReturn(false);

        MockMultipartFile excelFile = new MockMultipartFile(
                "file", "data.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy".getBytes());

        assertThatThrownBy(() -> service.uploadDocument(1L, "AADHAAR", excelFile, "officer1"))
                .isInstanceOf(DocumentUploadException.class)
                .hasMessageContaining("Unsupported file type");
    }

    @Test
    @DisplayName("uploadDocument: invalid document type string → throws InvalidDocumentTypeException")
    void uploadDocument_invalidDocumentType_throwsException() {
        when(beneficiaryRepository.findById(1L)).thenReturn(Optional.of(savedBeneficiary));

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> service.uploadDocument(1L, "PASSPORT", file, "officer1"))
                .isInstanceOf(InvalidDocumentTypeException.class)
                .hasMessageContaining("PASSPORT");
    }

    @Test
    @DisplayName("uploadDocument: duplicate document type → throws DuplicateDocumentTypeException")
    void uploadDocument_duplicateType_throwsException() {
        when(beneficiaryRepository.findById(1L)).thenReturn(Optional.of(savedBeneficiary));
        when(documentRepository.existsByBeneficiaryIdAndDocumentType(1L, DocumentType.AADHAAR))
                .thenReturn(true);

        MockMultipartFile file = new MockMultipartFile(
                "file", "aadhaar.pdf", "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> service.uploadDocument(1L, "AADHAAR", file, "officer1"))
                .isInstanceOf(DuplicateDocumentTypeException.class)
                .hasMessageContaining("AADHAAR");
    }
}
