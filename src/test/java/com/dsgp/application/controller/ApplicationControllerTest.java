package com.dsgp.application.controller;

import com.dsgp.application.dto.ApplicationRequest;
import com.dsgp.application.dto.ApplicationResponse;
import com.dsgp.application.exception.ApplicationException;
import com.dsgp.application.service.ApplicationService;
import com.dsgp.GovernmentSchemeApplication;
import com.dsgp.beneficiary.exception.BeneficiaryNotFoundException;
import com.dsgp.beneficiary.security.SecurityConfig;
import com.dsgp.config.ApiErrorResponse;
import com.dsgp.config.GlobalExceptionHandler;
import com.dsgp.scheme.exception.SchemeNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc slice tests for {@link ApplicationController}.
 *
 * <p>Follows the same pattern as {@code SchemeControllerTest} and
 * {@code BeneficiaryControllerTest}.  Service is fully mocked.
 */
@WebMvcTest(controllers = ApplicationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ApiErrorResponse.class})
@ContextConfiguration(classes = {GovernmentSchemeApplication.class, ApplicationController.class,
        SecurityConfig.class, GlobalExceptionHandler.class, ApiErrorResponse.class})
@ActiveProfiles("test")
@WithMockUser
@DisplayName("ApplicationController")
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplicationService applicationService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private ApplicationResponse successResponse() {
        return ApplicationResponse.builder()
                .applicationId(999L)
                .beneficiaryId(101)
                .beneficiaryName("Ravi Kumar")
                .schemeId(1L)
                .schemeName("PM-KISAN Samman Nidhi")
                .applicationStatus("PENDING")
                .eligibilityScore(80)
                .applicationDate(LocalDateTime.now())
                .build();
    }

    private ApplicationRequest validRequest() {
        ApplicationRequest req = new ApplicationRequest();
        req.setBeneficiaryId(101);
        req.setSchemeId(1L);
        return req;
    }

    // ── POST /applications ────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /applications")
    class PostApplication {

        @Test
        @DisplayName("returns 201 Created with PENDING status on successful submission")
        void submit_eligible_returns201() throws Exception {
            given(applicationService.submitApplication(any(ApplicationRequest.class)))
                    .willReturn(successResponse());

            mockMvc.perform(post("/applications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.applicationId").value(999))
                    .andExpect(jsonPath("$.applicationStatus").value("PENDING"))
                    .andExpect(jsonPath("$.eligibilityScore").value(80))
                    .andExpect(jsonPath("$.beneficiaryId").value(101))
                    .andExpect(jsonPath("$.schemeId").value(1))
                    .andExpect(jsonPath("$.schemeName").value("PM-KISAN Samman Nidhi"));
        }

        @Test
        @DisplayName("returns 400 Bad Request when beneficiaryId is missing")
        void submit_missingBeneficiaryId_returns400() throws Exception {
            ApplicationRequest req = new ApplicationRequest();
            req.setSchemeId(1L);   // no beneficiaryId

            mockMvc.perform(post("/applications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'beneficiaryId')]").exists());
        }

        @Test
        @DisplayName("returns 400 Bad Request when schemeId is missing")
        void submit_missingSchemeId_returns400() throws Exception {
            ApplicationRequest req = new ApplicationRequest();
            req.setBeneficiaryId(101);  // no schemeId

            mockMvc.perform(post("/applications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'schemeId')]").exists());
        }

        @Test
        @DisplayName("returns 404 Not Found when beneficiary does not exist")
        void submit_beneficiaryNotFound_returns404() throws Exception {
            given(applicationService.submitApplication(any(ApplicationRequest.class)))
                    .willThrow(new BeneficiaryNotFoundException("Beneficiary not found with ID: 101"));

            mockMvc.perform(post("/applications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(containsString("101")));
        }

        @Test
        @DisplayName("returns 404 Not Found when scheme does not exist")
        void submit_schemeNotFound_returns404() throws Exception {
            given(applicationService.submitApplication(any(ApplicationRequest.class)))
                    .willThrow(new SchemeNotFoundException(1L));

            mockMvc.perform(post("/applications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(containsString("1")));
        }

        @Test
        @DisplayName("returns 422 Unprocessable Entity when eligibility not yet checked")
        void submit_eligibilityNotChecked_returns422() throws Exception {
            given(applicationService.submitApplication(any(ApplicationRequest.class)))
                    .willThrow(new ApplicationException("Eligibility has not been checked"));

            mockMvc.perform(post("/applications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value(containsString("Eligibility has not been checked")));
        }

        @Test
        @DisplayName("returns 422 Unprocessable Entity when beneficiary is INELIGIBLE")
        void submit_ineligible_returns422() throws Exception {
            given(applicationService.submitApplication(any(ApplicationRequest.class)))
                    .willThrow(new ApplicationException(
                            "Beneficiary is not eligible for scheme 'PM-KISAN Samman Nidhi'. " +
                            "Eligibility score: 40/100 (minimum required: 60)."));

            mockMvc.perform(post("/applications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value(containsString("not eligible")))
                    .andExpect(jsonPath("$.message").value(containsString("40")));
        }

        @Test
        @DisplayName("returns 422 Unprocessable Entity on duplicate application")
        void submit_duplicate_returns422() throws Exception {
            given(applicationService.submitApplication(any(ApplicationRequest.class)))
                    .willThrow(new ApplicationException(
                            "An application for scheme 'PM-KISAN Samman Nidhi' already exists for this beneficiary."));

            mockMvc.perform(post("/applications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value(containsString("already exists")));
        }
    }
}
