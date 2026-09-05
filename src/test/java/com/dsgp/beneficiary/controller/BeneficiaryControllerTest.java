package com.dsgp.beneficiary.controller;

import com.dsgp.beneficiary.dto.BeneficiaryRegistrationRequest;
import com.dsgp.beneficiary.dto.BeneficiaryResponse;
import com.dsgp.beneficiary.dto.BeneficiaryUpdateRequest;
import com.dsgp.beneficiary.entity.Category;
import com.dsgp.beneficiary.entity.Gender;
import com.dsgp.beneficiary.entity.RegistrationStatus;
import com.dsgp.beneficiary.exception.BeneficiaryNotFoundException;
import com.dsgp.beneficiary.exception.DuplicateAadhaarException;
import com.dsgp.beneficiary.exception.DuplicateMobileException;
import com.dsgp.beneficiary.security.SecurityConfig;
import com.dsgp.beneficiary.service.BeneficiaryService;
import com.dsgp.config.ApiErrorResponse;
import com.dsgp.config.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc slice tests for {@link BeneficiaryController}.
 *
 * <p>Uses {@code @WebMvcTest} to load only the web layer (controller +
 * filters). The service layer is fully mocked via {@code @MockBean}.
 *
 * <p>Covers:
 * <ul>
 *   <li>Milestone 1 — POST/GET/DELETE happy paths and error cases.</li>
 *   <li>Milestone 2 — Extended field round-trip (POST with eligibility fields,
 *       PATCH-style PUT with {@link BeneficiaryUpdateRequest}).</li>
 *   <li>Validation — 400 responses for missing/invalid mandatory fields.</li>
 *   <li>Conflict — 409 responses for duplicate govId / contact / aadhaar / mobile.</li>
 * </ul>
 */
@WebMvcTest(BeneficiaryController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ApiErrorResponse.class})
@ActiveProfiles("test")
@WithMockUser
@DisplayName("BeneficiaryController")
class BeneficiaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BeneficiaryService beneficiaryService;

    private ObjectMapper objectMapper;

    // ── Shared fixtures ──────────────────────────────────────────────────────

    /** Minimal legacy 7-field registration request (Milestone 1 format). */
    private BeneficiaryRegistrationRequest legacyRequest;

    /** Extended registration request with all eligibility fields. */
    private BeneficiaryRegistrationRequest extendedRequest;

    /** Response containing only original fields (simulates M1 legacy record). */
    private BeneficiaryResponse legacyResponse;

    /** Response with all extended eligibility fields populated. */
    private BeneficiaryResponse extendedResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        legacyRequest = new BeneficiaryRegistrationRequest();
        legacyRequest.setFullName("Ravi Kumar");
        legacyRequest.setGovId("AABCD1234E");
        legacyRequest.setContact("9876543210");
        legacyRequest.setEmail("ravi@example.com");
        legacyRequest.setAge(38);
        legacyRequest.setAddress("Village Uruli Kanchan, Pune");
        legacyRequest.setSchemeName("PM-KISAN");

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

        legacyResponse = BeneficiaryResponse.builder()
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

        extendedResponse = BeneficiaryResponse.builder()
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
    }

    // ════════════════════════════════════════════════════════════════════════
    // POST /beneficiaries
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /beneficiaries")
    class PostBeneficiary {

        @Test
        @DisplayName("201 Created — legacy 7-field request succeeds")
        void post_legacyRequest_returns201() throws Exception {
            given(beneficiaryService.registerBeneficiary(any())).willReturn(legacyResponse);

            mockMvc.perform(post("/beneficiaries")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(legacyRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.fullName").value("Ravi Kumar"))
                    .andExpect(jsonPath("$.govId").value("AABCD1234E"))
                    .andExpect(jsonPath("$.registrationStatus").value("PENDING"));
        }

        @Test
        @DisplayName("201 Created — extended request persists eligibility fields")
        void post_extendedRequest_returns201WithEligibilityFields() throws Exception {
            given(beneficiaryService.registerBeneficiary(any())).willReturn(extendedResponse);

            mockMvc.perform(post("/beneficiaries")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(extendedRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.aadhaarNumber").value("123456789012"))
                    .andExpect(jsonPath("$.mobileNumber").value("7654321098"))
                    .andExpect(jsonPath("$.gender").value("FEMALE"))
                    .andExpect(jsonPath("$.district").value("Satara"))
                    .andExpect(jsonPath("$.annualIncome").value(85000.00))
                    .andExpect(jsonPath("$.landHolding").value(1.5))
                    .andExpect(jsonPath("$.category").value("OBC"));
        }

        @Test
        @DisplayName("400 Bad Request — fullName blank")
        void post_blankFullName_returns400() throws Exception {
            legacyRequest.setFullName("");

            mockMvc.perform(post("/beneficiaries")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(legacyRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("400 Bad Request — contact has wrong format (not 10 digits)")
        void post_invalidContact_returns400() throws Exception {
            legacyRequest.setContact("123");

            mockMvc.perform(post("/beneficiaries")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(legacyRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 Bad Request — invalid email format")
        void post_invalidEmail_returns400() throws Exception {
            legacyRequest.setEmail("not-an-email");

            mockMvc.perform(post("/beneficiaries")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(legacyRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 Bad Request — aadhaar number not 12 digits")
        void post_invalidAadhaar_returns400() throws Exception {
            extendedRequest.setAadhaarNumber("1234"); // too short

            mockMvc.perform(post("/beneficiaries")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(extendedRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 Bad Request — mobile number doesn't start with 6-9")
        void post_invalidMobileNumber_returns400() throws Exception {
            extendedRequest.setMobileNumber("5876543210"); // starts with 5

            mockMvc.perform(post("/beneficiaries")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(extendedRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 Bad Request — negative annual income")
        void post_negativeIncome_returns400() throws Exception {
            extendedRequest.setAnnualIncome(new BigDecimal("-1.00"));

            mockMvc.perform(post("/beneficiaries")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(extendedRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("409 Conflict — duplicate govId throws DuplicateAadhaarException")
        void post_duplicateGovId_returns409() throws Exception {
            given(beneficiaryService.registerBeneficiary(any()))
                    .willThrow(new DuplicateAadhaarException("AABCD1234E"));

            mockMvc.perform(post("/beneficiaries")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(legacyRequest)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("409 Conflict — duplicate contact throws DuplicateMobileException")
        void post_duplicateContact_returns409() throws Exception {
            given(beneficiaryService.registerBeneficiary(any()))
                    .willThrow(new DuplicateMobileException("9876543210"));

            mockMvc.perform(post("/beneficiaries")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(legacyRequest)))
                    .andExpect(status().isConflict());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /beneficiaries/{id}
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /beneficiaries/{id}")
    class GetById {

        @Test
        @DisplayName("200 OK — returns beneficiary with all fields")
        void getById_found_returns200() throws Exception {
            given(beneficiaryService.getBeneficiaryById(1)).willReturn(legacyResponse);

            mockMvc.perform(get("/beneficiaries/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.fullName").value("Ravi Kumar"))
                    .andExpect(jsonPath("$.registrationStatus").value("PENDING"))
                    .andExpect(jsonPath("$.identityVerified").value(false));
        }

        @Test
        @DisplayName("404 Not Found — unknown id")
        void getById_notFound_returns404() throws Exception {
            given(beneficiaryService.getBeneficiaryById(99))
                    .willThrow(new BeneficiaryNotFoundException(99));

            mockMvc.perform(get("/beneficiaries/99"))
                    .andExpect(status().isNotFound());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /beneficiaries/gov-id/{govId}
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /beneficiaries/gov-id/{govId}")
    class GetByGovId {

        @Test
        @DisplayName("200 OK — returns beneficiary matching govId")
        void getByGovId_found_returns200() throws Exception {
            given(beneficiaryService.getBeneficiaryByGovId("AABCD1234E"))
                    .willReturn(legacyResponse);

            mockMvc.perform(get("/beneficiaries/gov-id/AABCD1234E"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.govId").value("AABCD1234E"));
        }

        @Test
        @DisplayName("404 Not Found — unknown govId")
        void getByGovId_notFound_returns404() throws Exception {
            given(beneficiaryService.getBeneficiaryByGovId("UNKNOWN"))
                    .willThrow(new BeneficiaryNotFoundException("Beneficiary not found with Government ID: UNKNOWN"));

            mockMvc.perform(get("/beneficiaries/gov-id/UNKNOWN"))
                    .andExpect(status().isNotFound());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /beneficiaries
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /beneficiaries")
    class GetAll {

        @Test
        @DisplayName("200 OK — returns list of all beneficiaries")
        void getAll_returns200WithList() throws Exception {
            given(beneficiaryService.getAllBeneficiaries())
                    .willReturn(List.of(legacyResponse, extendedResponse));

            mockMvc.perform(get("/beneficiaries"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].fullName").value("Ravi Kumar"))
                    .andExpect(jsonPath("$[1].fullName").value("Sita Devi"));
        }

        @Test
        @DisplayName("200 OK — returns empty array when no beneficiaries")
        void getAll_empty_returns200WithEmptyList() throws Exception {
            given(beneficiaryService.getAllBeneficiaries()).willReturn(List.of());

            mockMvc.perform(get("/beneficiaries"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // PUT /beneficiaries/{id}
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /beneficiaries/{id}")
    class PutBeneficiary {

        @Test
        @DisplayName("200 OK — patch update with eligibility fields succeeds")
        void put_patchWithEligibilityFields_returns200() throws Exception {
            BeneficiaryUpdateRequest patchReq = new BeneficiaryUpdateRequest();
            patchReq.setAnnualIncome(new BigDecimal("120000.00"));
            patchReq.setCategory(Category.SC);
            patchReq.setDistrict("Pune");

            BeneficiaryResponse updated = BeneficiaryResponse.builder()
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
                    .district("Pune")
                    .registrationStatus(RegistrationStatus.PENDING)
                    .build();

            given(beneficiaryService.updateBeneficiary(eq(1), any(BeneficiaryUpdateRequest.class)))
                    .willReturn(updated);

            mockMvc.perform(put("/beneficiaries/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(patchReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.annualIncome").value(120000.00))
                    .andExpect(jsonPath("$.category").value("SC"))
                    .andExpect(jsonPath("$.district").value("Pune"));
        }

        @Test
        @DisplayName("200 OK — empty patch body (no fields) is accepted")
        void put_emptyPatch_returns200() throws Exception {
            BeneficiaryUpdateRequest emptyReq = new BeneficiaryUpdateRequest();
            given(beneficiaryService.updateBeneficiary(eq(1), any(BeneficiaryUpdateRequest.class)))
                    .willReturn(legacyResponse);

            mockMvc.perform(put("/beneficiaries/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emptyReq)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("400 Bad Request — invalid PIN code format")
        void put_invalidPinCode_returns400() throws Exception {
            BeneficiaryUpdateRequest badReq = new BeneficiaryUpdateRequest();
            badReq.setPinCode("12"); // not 6 digits

            mockMvc.perform(put("/beneficiaries/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badReq)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 Bad Request — negative land holding")
        void put_negativeLandHolding_returns400() throws Exception {
            BeneficiaryUpdateRequest badReq = new BeneficiaryUpdateRequest();
            badReq.setLandHolding(new BigDecimal("-0.5"));

            mockMvc.perform(put("/beneficiaries/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badReq)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("404 Not Found — beneficiary does not exist")
        void put_notFound_returns404() throws Exception {
            given(beneficiaryService.updateBeneficiary(eq(99), any(BeneficiaryUpdateRequest.class)))
                    .willThrow(new BeneficiaryNotFoundException(99));

            mockMvc.perform(put("/beneficiaries/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new BeneficiaryUpdateRequest())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("409 Conflict — mobile number already taken")
        void put_duplicateMobile_returns409() throws Exception {
            BeneficiaryUpdateRequest req = new BeneficiaryUpdateRequest();
            req.setMobileNumber("9999999999");

            given(beneficiaryService.updateBeneficiary(eq(1), any(BeneficiaryUpdateRequest.class)))
                    .willThrow(new DuplicateMobileException("9999999999"));

            mockMvc.perform(put("/beneficiaries/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // DELETE /beneficiaries/{id}
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /beneficiaries/{id}")
    class DeleteBeneficiary {

        @Test
        @DisplayName("204 No Content — successful deletion")
        void delete_existing_returns204() throws Exception {
            willDoNothing().given(beneficiaryService).deleteBeneficiary(1);

            mockMvc.perform(delete("/beneficiaries/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("404 Not Found — beneficiary does not exist")
        void delete_notFound_returns404() throws Exception {
            willThrow(new BeneficiaryNotFoundException(99))
                    .given(beneficiaryService).deleteBeneficiary(99);

            mockMvc.perform(delete("/beneficiaries/99"))
                    .andExpect(status().isNotFound());
        }
    }
}
