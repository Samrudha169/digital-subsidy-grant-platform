package com.dsgp.verification.controller;

import com.dsgp.GovernmentSchemeApplication;
import com.dsgp.beneficiary.security.SecurityConfig;
import com.dsgp.config.ApiErrorResponse;
import com.dsgp.config.GlobalExceptionHandler;
import com.dsgp.verification.dto.VerificationActionRequest;
import com.dsgp.verification.dto.VerificationStatusResponse;
import com.dsgp.verification.exception.InvalidVerificationTransitionException;
import com.dsgp.verification.service.VerificationService;
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
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc slice tests for {@link VerificationController}.
 *
 * <p>The service is fully mocked. Tests verify HTTP status codes, response
 * JSON shape, and exception-to-HTTP mapping for the verification endpoints.
 */
@WebMvcTest(controllers = VerificationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ApiErrorResponse.class})
@ContextConfiguration(classes = {GovernmentSchemeApplication.class, VerificationController.class,
        SecurityConfig.class, GlobalExceptionHandler.class, ApiErrorResponse.class})
@ActiveProfiles("test")
@WithMockUser
@DisplayName("VerificationController")
class VerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VerificationService verificationService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // for LocalDateTime serialisation
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private VerificationStatusResponse statusResponse(String status) {
        return VerificationStatusResponse.builder()
                .applicationId(1L)
                .beneficiaryId(101)
                .beneficiaryName("Ravi Kumar")
                .schemeId(1L)
                .schemeName("PM-KISAN Samman Nidhi")
                .applicationStatus(status)
                .applicationDate(LocalDateTime.now())
                .history(List.of())
                .build();
    }

    private VerificationActionRequest actionReq(String by, String remarks) {
        VerificationActionRequest r = new VerificationActionRequest();
        r.setPerformedBy(by);
        r.setRemarks(remarks);
        return r;
    }

    // ── GET /verification/applications/{id} ──────────────────────────────────

    @Nested
    @DisplayName("GET /verification/applications/{id}")
    class GetStatus {

        @Test
        @DisplayName("returns 200 with status and empty history")
        void getStatus_returns200() throws Exception {
            given(verificationService.getStatus(1L)).willReturn(statusResponse("PENDING"));

            mockMvc.perform(get("/verification/applications/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.applicationId").value(1))
                    .andExpect(jsonPath("$.applicationStatus").value("PENDING"))
                    .andExpect(jsonPath("$.beneficiaryId").value(101))
                    .andExpect(jsonPath("$.history", hasSize(0)));
        }
    }

    // ── POST /verification/applications/{id}/start ────────────────────────────

    @Nested
    @DisplayName("POST .../start")
    class Start {

        @Test
        @DisplayName("returns 200 OK with UNDER_REVIEW status after start")
        void start_valid_returns200() throws Exception {
            given(verificationService.startVerification(eq(1L), any()))
                    .willReturn(statusResponse("UNDER_REVIEW"));

            mockMvc.perform(post("/verification/applications/1/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(actionReq("officer1", null))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.applicationStatus").value("UNDER_REVIEW"));
        }

        @Test
        @DisplayName("returns 400 when performedBy is missing")
        void start_missingPerformedBy_returns400() throws Exception {
            VerificationActionRequest bad = new VerificationActionRequest();
            // performedBy is null — fails @NotBlank

            mockMvc.perform(post("/verification/applications/1/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 on invalid transition (ineligible)")
        void start_ineligible_returns400() throws Exception {
            given(verificationService.startVerification(eq(1L), any()))
                    .willThrow(new InvalidVerificationTransitionException("Beneficiary is INELIGIBLE"));

            mockMvc.perform(post("/verification/applications/1/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(actionReq("officer1", null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("INELIGIBLE")));
        }
    }

    // ── Field Officer endpoints ────────────────────────────────────────────────

    @Nested
    @DisplayName("POST .../field-approve")
    class FieldApprove {

        @Test
        @DisplayName("returns 200 OK with FIELD_APPROVED status")
        void fieldApprove_valid_returns200() throws Exception {
            given(verificationService.approveAtField(eq(1L), any()))
                    .willReturn(statusResponse("FIELD_APPROVED"));

            mockMvc.perform(post("/verification/applications/1/field-approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(actionReq("officer1", "All ok"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.applicationStatus").value("FIELD_APPROVED"));
        }

        @Test
        @DisplayName("returns 400 on wrong status transition")
        void fieldApprove_wrongStatus_returns400() throws Exception {
            given(verificationService.approveAtField(eq(1L), any()))
                    .willThrow(new InvalidVerificationTransitionException("Requires UNDER_REVIEW"));

            mockMvc.perform(post("/verification/applications/1/field-approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(actionReq("officer1", null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("UNDER_REVIEW")));
        }
    }

    @Nested
    @DisplayName("POST .../field-reject")
    class FieldReject {

        @Test
        @DisplayName("returns 200 OK with REJECTED status")
        void fieldReject_valid_returns200() throws Exception {
            given(verificationService.rejectAtField(eq(1L), any()))
                    .willReturn(statusResponse("REJECTED"));

            mockMvc.perform(post("/verification/applications/1/field-reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(actionReq("officer1", "Invalid docs"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.applicationStatus").value("REJECTED"));
        }
    }

    @Nested
    @DisplayName("POST .../escalate")
    class Escalate {

        @Test
        @DisplayName("returns 200 OK with ESCALATED status")
        void escalate_valid_returns200() throws Exception {
            given(verificationService.escalateAtField(eq(1L), any()))
                    .willReturn(statusResponse("ESCALATED"));

            mockMvc.perform(post("/verification/applications/1/escalate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(actionReq("officer1", "Needs district"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.applicationStatus").value("ESCALATED"));
        }
    }

    // ── District Officer endpoints ─────────────────────────────────────────────

    @Nested
    @DisplayName("POST .../district-approve")
    class DistrictApprove {

        @Test
        @DisplayName("returns 200 OK with DISTRICT_APPROVED status")
        void districtApprove_valid_returns200() throws Exception {
            given(verificationService.approveAtDistrict(eq(1L), any()))
                    .willReturn(statusResponse("DISTRICT_APPROVED"));

            mockMvc.perform(post("/verification/applications/1/district-approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(actionReq("district1", null))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.applicationStatus").value("DISTRICT_APPROVED"));
        }
    }

    @Nested
    @DisplayName("POST .../district-reject")
    class DistrictReject {

        @Test
        @DisplayName("returns 200 OK with REJECTED status")
        void districtReject_valid_returns200() throws Exception {
            given(verificationService.rejectAtDistrict(eq(1L), any()))
                    .willReturn(statusResponse("REJECTED"));

            mockMvc.perform(post("/verification/applications/1/district-reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(actionReq("district1", "Rejected reason"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.applicationStatus").value("REJECTED"));
        }
    }

    // ── Finance Approver endpoints ─────────────────────────────────────────────

    @Nested
    @DisplayName("POST .../finance-approve")
    class FinanceApprove {

        @Test
        @DisplayName("returns 200 OK with APPROVED status")
        void financeApprove_valid_returns200() throws Exception {
            given(verificationService.approveAtFinance(eq(1L), any()))
                    .willReturn(statusResponse("APPROVED"));

            mockMvc.perform(post("/verification/applications/1/finance-approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(actionReq("finance1", null))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.applicationStatus").value("APPROVED"));
        }
    }

    @Nested
    @DisplayName("POST .../finance-reject")
    class FinanceReject {

        @Test
        @DisplayName("returns 200 OK with REJECTED status")
        void financeReject_valid_returns200() throws Exception {
            given(verificationService.rejectAtFinance(eq(1L), any()))
                    .willReturn(statusResponse("REJECTED"));

            mockMvc.perform(post("/verification/applications/1/finance-reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(actionReq("finance1", "Docs insufficient"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.applicationStatus").value("REJECTED"));
        }
    }
}
