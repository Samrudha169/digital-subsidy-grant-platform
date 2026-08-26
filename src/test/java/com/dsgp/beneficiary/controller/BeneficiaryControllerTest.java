package com.dsgp.beneficiary.controller;

import com.dsgp.beneficiary.dto.*;
import com.dsgp.beneficiary.entity.*;
import com.dsgp.beneficiary.exception.*;
import com.dsgp.beneficiary.service.BeneficiaryService;
import com.dsgp.security.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web MVC slice test for {@link BeneficiaryController}.
 *
 * <p>Uses {@code @WebMvcTest} to load only the web layer — no Spring Security
 * filter chain is active (security is permit-all for Module 1 anyway).
 * The service layer is mocked with {@code @MockBean}.
 */
@WebMvcTest(BeneficiaryController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@WithMockUser
class BeneficiaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BeneficiaryService beneficiaryService;

    private ObjectMapper objectMapper;

    // ── Test Fixtures ──────────────────────────────────────────────────────────

    private BeneficiaryRegistrationRequest validRegistrationRequest;
    private BeneficiaryResponse sampleResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        validRegistrationRequest = new BeneficiaryRegistrationRequest();
        validRegistrationRequest.setFirstName("Ramesh");
        validRegistrationRequest.setLastName("Kumar");
        validRegistrationRequest.setDateOfBirth(LocalDate.of(1990, 5, 15));
        validRegistrationRequest.setGender(Gender.MALE);
        validRegistrationRequest.setAadhaarNumber("123456789012");
        validRegistrationRequest.setMobileNumber("9876543210");
        validRegistrationRequest.setEmail("ramesh@example.com");
        validRegistrationRequest.setDistrict("Pune");
        validRegistrationRequest.setState("Maharashtra");
        validRegistrationRequest.setCategory(Category.OBC);

        sampleResponse = BeneficiaryResponse.builder()
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
                .identityVerified(false)
                .build();
    }

    // ── POST /beneficiaries ────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /beneficiaries: valid body → 201 Created with response")
    void registerBeneficiary_validRequest_returns201() throws Exception {
        when(beneficiaryService.registerBeneficiary(any(BeneficiaryRegistrationRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/beneficiaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Ramesh"))
                .andExpect(jsonPath("$.aadhaarNumber").value("123456789012"))
                .andExpect(jsonPath("$.registrationStatus").value("PENDING"));
    }

    @Test
    @DisplayName("POST /beneficiaries: missing firstName → 400 with fieldErrors")
    void registerBeneficiary_missingFirstName_returns400() throws Exception {
        validRegistrationRequest.setFirstName(null);  // violates @NotBlank

        mockMvc.perform(post("/beneficiaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @DisplayName("POST /beneficiaries: invalid Aadhaar (11 digits) → 400 with fieldErrors")
    void registerBeneficiary_invalidAadhaar_returns400() throws Exception {
        validRegistrationRequest.setAadhaarNumber("12345678901");  // only 11 digits

        mockMvc.perform(post("/beneficiaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /beneficiaries: duplicate Aadhaar → 409 Conflict")
    void registerBeneficiary_duplicateAadhaar_returns409() throws Exception {
        when(beneficiaryService.registerBeneficiary(any()))
                .thenThrow(new DuplicateAadhaarException("123456789012"));

        mockMvc.perform(post("/beneficiaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST /beneficiaries: duplicate mobile → 409 Conflict")
    void registerBeneficiary_duplicateMobile_returns409() throws Exception {
        when(beneficiaryService.registerBeneficiary(any()))
                .thenThrow(new DuplicateMobileException("9876543210"));

        mockMvc.perform(post("/beneficiaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationRequest)))
                .andExpect(status().isConflict());
    }

    // ── GET /beneficiaries ────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /beneficiaries: returns 200 with paginated content")
    void getAllBeneficiaries_returns200() throws Exception {
        BeneficiarySummaryResponse summary = BeneficiarySummaryResponse.builder()
                .id(1L).firstName("Ramesh").lastName("Kumar")
                .aadhaarNumber("123456789012").mobileNumber("9876543210")
                .district("Pune").registrationStatus(RegistrationStatus.PENDING)
                .identityVerified(false).build();

        Page<BeneficiarySummaryResponse> page = new PageImpl<>(List.of(summary));
        when(beneficiaryService.getAllBeneficiaries(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/beneficiaries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].firstName").value("Ramesh"));
    }

    // ── GET /beneficiaries/{id} ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /beneficiaries/{id}: existing ID → 200 with full response")
    void getBeneficiaryById_found_returns200() throws Exception {
        when(beneficiaryService.getBeneficiaryById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/beneficiaries/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.lastName").value("Kumar"));
    }

    @Test
    @DisplayName("GET /beneficiaries/{id}: non-existent ID → 404 Not Found")
    void getBeneficiaryById_notFound_returns404() throws Exception {
        when(beneficiaryService.getBeneficiaryById(99L))
                .thenThrow(new BeneficiaryNotFoundException(99L));

        mockMvc.perform(get("/beneficiaries/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── GET /beneficiaries/aadhaar/{aadhaar} ──────────────────────────────────

    @Test
    @DisplayName("GET /beneficiaries/aadhaar/{aadhaar}: found → 200")
    void getBeneficiaryByAadhaar_found_returns200() throws Exception {
        when(beneficiaryService.getBeneficiaryByAadhaar("123456789012")).thenReturn(sampleResponse);

        mockMvc.perform(get("/beneficiaries/aadhaar/123456789012"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aadhaarNumber").value("123456789012"));
    }

    // ── PUT /beneficiaries/{id} ────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /beneficiaries/{id}: valid update → 200 OK")
    void updateBeneficiary_valid_returns200() throws Exception {
        BeneficiaryUpdateRequest updateRequest = new BeneficiaryUpdateRequest();
        updateRequest.setDistrict("Nashik");

        when(beneficiaryService.updateBeneficiary(eq(1L), any(BeneficiaryUpdateRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(put("/beneficiaries/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());
    }

    // ── PATCH /beneficiaries/{id}/status ──────────────────────────────────────

    @Test
    @DisplayName("PATCH /beneficiaries/{id}/status: ACTIVE → 200 OK")
    void updateStatus_validStatus_returns200() throws Exception {
        BeneficiaryResponse activeResponse = BeneficiaryResponse.builder()
                .id(1L).firstName("Ramesh").lastName("Kumar")
                .aadhaarNumber("123456789012").mobileNumber("9876543210")
                .district("Pune").state("Maharashtra").category(Category.OBC)
                .gender(Gender.MALE).dateOfBirth(LocalDate.of(1990, 5, 15))
                .registrationStatus(RegistrationStatus.ACTIVE)
                .registrationDate(LocalDateTime.now()).identityVerified(false).build();

        when(beneficiaryService.updateRegistrationStatus(1L, RegistrationStatus.ACTIVE))
                .thenReturn(activeResponse);

        mockMvc.perform(patch("/beneficiaries/1/status")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationStatus").value("ACTIVE"));
    }

    // ── PATCH /beneficiaries/{id}/verify ──────────────────────────────────────

    @Test
    @DisplayName("PATCH /beneficiaries/{id}/verify: existing ID → 200 with identityVerified=true")
    void verifyIdentity_existing_returns200() throws Exception {
        BeneficiaryResponse verifiedResponse = BeneficiaryResponse.builder()
                .id(1L).firstName("Ramesh").lastName("Kumar")
                .aadhaarNumber("123456789012").mobileNumber("9876543210")
                .district("Pune").state("Maharashtra").category(Category.OBC)
                .gender(Gender.MALE).dateOfBirth(LocalDate.of(1990, 5, 15))
                .registrationStatus(RegistrationStatus.PENDING)
                .registrationDate(LocalDateTime.now()).identityVerified(true).build();

        when(beneficiaryService.verifyIdentity(1L)).thenReturn(verifiedResponse);

        mockMvc.perform(patch("/beneficiaries/1/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identityVerified").value(true));
    }

    @Test
    @DisplayName("PATCH /beneficiaries/{id}/verify: non-existent → 404")
    void verifyIdentity_notFound_returns404() throws Exception {
        when(beneficiaryService.verifyIdentity(99L))
                .thenThrow(new BeneficiaryNotFoundException(99L));

        mockMvc.perform(patch("/beneficiaries/99/verify"))
                .andExpect(status().isNotFound());
    }

    // ── GET /beneficiaries/status/{status} ────────────────────────────────────

    @Test
    @DisplayName("GET /beneficiaries/status/PENDING: returns 200 with paginated summaries")
    void getBeneficiariesByStatus_returns200() throws Exception {
        BeneficiarySummaryResponse summary = BeneficiarySummaryResponse.builder()
                .id(1L).firstName("Ramesh").lastName("Kumar")
                .aadhaarNumber("123456789012").mobileNumber("9876543210")
                .district("Pune").registrationStatus(RegistrationStatus.PENDING)
                .identityVerified(false).build();

        Page<BeneficiarySummaryResponse> page = new PageImpl<>(List.of(summary));
        when(beneficiaryService.getBeneficiariesByStatus(
                eq(RegistrationStatus.PENDING), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/beneficiaries/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].registrationStatus").value("PENDING"));
    }

    @Test
    @DisplayName("GET /beneficiaries/status/INVALID: invalid status value → 400")
    void getBeneficiariesByStatus_invalidStatus_returns400() throws Exception {
        mockMvc.perform(get("/beneficiaries/status/UNKNOWN_STATUS"))
                .andExpect(status().isBadRequest());
    }

    // ── DELETE /beneficiaries/{id} ─────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /beneficiaries/{id}: existing ID → 204 No Content (soft delete)")
    void deleteBeneficiary_existing_returns204() throws Exception {
        doNothing().when(beneficiaryService).deleteBeneficiary(1L);

        mockMvc.perform(delete("/beneficiaries/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /beneficiaries/{id}: non-existent → 404")
    void deleteBeneficiary_notFound_returns404() throws Exception {
        doThrow(new BeneficiaryNotFoundException(99L))
                .when(beneficiaryService).deleteBeneficiary(99L);

        mockMvc.perform(delete("/beneficiaries/99"))
                .andExpect(status().isNotFound());
    }

    // ── GET /beneficiaries/{id}/documents ─────────────────────────────────────

    @Test
    @DisplayName("GET /beneficiaries/{id}/documents: returns 200 with document list")
    void getDocuments_returns200() throws Exception {
        DocumentResponse doc = DocumentResponse.builder()
                .id(10L).beneficiaryId(1L)
                .documentType(DocumentType.AADHAAR)
                .originalFileName("aadhaar.pdf")
                .fileName("uuid_aadhaar.pdf")
                .fileSize(50000L)
                .mimeType("application/pdf")
                .uploadedAt(LocalDateTime.now())
                .uploadedBy("officer1")
                .verified(false)
                .build();

        when(beneficiaryService.getDocuments(1L)).thenReturn(List.of(doc));

        mockMvc.perform(get("/beneficiaries/1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentType").value("AADHAAR"))
                .andExpect(jsonPath("$[0].originalFileName").value("aadhaar.pdf"));
    }

    // ── Duplicate Document Type → 409 ─────────────────────────────────────────

    @Test
    @DisplayName("POST /beneficiaries/{id}/documents: duplicate type → 409 Conflict")
    void uploadDocument_duplicateType_returns409() throws Exception {
        when(beneficiaryService.uploadDocument(eq(1L), eq("AADHAAR"), any(), any()))
                .thenThrow(new DuplicateDocumentTypeException(1L, "AADHAAR"));

        mockMvc.perform(multipart("/beneficiaries/1/documents")
                        .file("file", "content".getBytes())
                        .param("documentType", "AADHAAR")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
