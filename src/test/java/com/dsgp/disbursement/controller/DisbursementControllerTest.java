package com.dsgp.disbursement.controller;

import com.dsgp.GovernmentSchemeApplication;
import com.dsgp.beneficiary.security.SecurityConfig;
import com.dsgp.config.ApiErrorResponse;
import com.dsgp.config.GlobalExceptionHandler;
import com.dsgp.disbursement.dto.DisbursementStageRequest;
import com.dsgp.disbursement.entity.ComplianceStatus;
import com.dsgp.disbursement.entity.DisbursementPlan;
import com.dsgp.disbursement.entity.DisbursementStage;
import com.dsgp.disbursement.entity.DisbursementStageStatus;
import com.dsgp.disbursement.entity.DisbursementStatus;
import com.dsgp.disbursement.entity.DisbursementType;
import com.dsgp.disbursement.repository.DisbursementPlanRepository;
import com.dsgp.disbursement.service.DisbursementPlanService;
import com.dsgp.disbursement.service.DisbursementStageService;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import com.dsgp.beneficiary.repository.SchemeApplicationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc slice tests for {@link DisbursementController}.
 * Mirrors the conventions used in {@code SchemeControllerTest}.
 */
@WebMvcTest(controllers = DisbursementController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ApiErrorResponse.class})
@ContextConfiguration(classes = {
        GovernmentSchemeApplication.class,
        DisbursementController.class,
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        ApiErrorResponse.class
})
@ActiveProfiles("test")
@WithMockUser
@DisplayName("DisbursementController")
class DisbursementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DisbursementStageService disbursementStageService;

    @MockBean
    private DisbursementPlanRepository disbursementPlanRepository;

    @MockBean
    private DisbursementPlanService disbursementPlanService;

    @MockBean
    private SchemeApplicationRepository schemeApplicationRepository;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private DisbursementPlan plan(Long id, BigDecimal total) {
        DisbursementPlan p = DisbursementPlan.builder()
                .totalAmount(total)
                .releasedAmount(BigDecimal.ZERO)
                .remainingAmount(total)
                .disbursementType(DisbursementType.STAGED)
                .status(DisbursementStatus.PENDING)
                .build();
        p.setId(id);
        return p;
    }

    private DisbursementStage stage(Long id, int stageNum, DisbursementStageStatus status) {
        DisbursementStage s = DisbursementStage.builder()
                .stageNumber(stageNum)
                .amount(new BigDecimal("2000.00"))
                .milestone("Test Milestone " + stageNum)
                .dueDate(LocalDate.now().plusDays(30))
                .status(status)
                .complianceStatus(ComplianceStatus.PENDING)
                .build();
        s.setId(id);
        return s;
    }

    // ── GET /api/disbursements/application/{id}/plan ──────────────────────────

    @Nested
    @DisplayName("GET /api/disbursements/application/{applicationId}/plan")
    class GetPlanByApplicationId {

        @Test
        @DisplayName("returns 200 OK with plan when found")
        void planFound_returns200() throws Exception {
            DisbursementPlan p = plan(1L, new BigDecimal("6000.00"));
            given(disbursementPlanRepository.findByApplication_Id(10L)).willReturn(Optional.of(p));

            mockMvc.perform(get("/api/disbursements/application/10/plan"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.totalAmount").value(6000.00))
                    .andExpect(jsonPath("$.disbursementType").value("STAGED"))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("returns 404 Not Found when no plan exists for the application")
        void planNotFound_returns404() throws Exception {
            given(disbursementPlanRepository.findByApplication_Id(99L)).willReturn(Optional.empty());

            mockMvc.perform(get("/api/disbursements/application/99/plan"))
                    .andExpect(status().isNotFound());
        }
    }

    // ── POST /api/disbursements/application/{id}/plan ─────────────────────────

    @Nested
    @DisplayName("POST /api/disbursements/application/{applicationId}/plan")
    class CreatePlan {

        @Test
        @DisplayName("returns 201 Created with plan body on valid request")
        void validRequest_returns201WithPlan() throws Exception {
            com.dsgp.application.entity.SchemeApplication app =
                    com.dsgp.application.entity.SchemeApplication.builder()
                            .applicationStatus("APPROVED")
                            .sanctionedAmount(new BigDecimal("6000.00"))
                            .build();
            app.setId(10L);

            DisbursementPlan p = plan(1L, new BigDecimal("6000.00"));

            given(schemeApplicationRepository.findById(10L)).willReturn(Optional.of(app));
            given(disbursementPlanService.createPlan(any(), eq(DisbursementType.STAGED)))
                    .willReturn(p);

            mockMvc.perform(post("/api/disbursements/application/10/plan")
                            .param("type", "STAGED"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.totalAmount").value(6000.00));
        }

        @Test
        @DisplayName("returns 400/500 when application is not found")
        void applicationNotFound_returnsError() throws Exception {
            given(schemeApplicationRepository.findById(99L)).willReturn(Optional.empty());

            mockMvc.perform(post("/api/disbursements/application/99/plan")
                            .param("type", "STAGED"))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        @DisplayName("returns 500 when service throws IllegalStateException (e.g., not approved)")
        void notApprovedApplication_returnsError() throws Exception {
            com.dsgp.application.entity.SchemeApplication app =
                    com.dsgp.application.entity.SchemeApplication.builder()
                            .applicationStatus("PENDING")
                            .sanctionedAmount(new BigDecimal("6000.00"))
                            .build();
            app.setId(10L);

            given(schemeApplicationRepository.findById(10L)).willReturn(Optional.of(app));
            given(disbursementPlanService.createPlan(any(), any()))
                    .willThrow(new IllegalStateException("approved application"));

            mockMvc.perform(post("/api/disbursements/application/10/plan")
                            .param("type", "STAGED"))
                    .andExpect(status().is5xxServerError());
        }
    }

    // ── POST /api/disbursements/{planId}/stages ───────────────────────────────

    @Nested
    @DisplayName("POST /api/disbursements/{planId}/stages")
    class CreateStage {

        @Test
        @DisplayName("returns 201 Created with stage on valid request")
        void validRequest_returns201WithStage() throws Exception {
            DisbursementStage s = stage(1L, 1, DisbursementStageStatus.PENDING);
            given(disbursementStageService.createStage(
                    eq(1L), eq(1), any(BigDecimal.class), any(String.class), any(LocalDate.class)))
                    .willReturn(s);

            DisbursementStageRequest req = new DisbursementStageRequest();
            req.setStageNumber(1);
            req.setAmount(new BigDecimal("2000.00"));
            req.setMilestone("First Instalment");
            req.setDueDate(LocalDate.now().plusDays(30));

            mockMvc.perform(post("/api/disbursements/1/stages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.stageNumber").value(1))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("returns 500 when service throws IllegalArgumentException")
        void invalidRequest_returns500OnServiceError() throws Exception {
            given(disbursementStageService.createStage(
                    anyLong(), any(), any(), any(), any()))
                    .willThrow(new IllegalArgumentException("Stage amount exceeds sanctioned amount"));

            DisbursementStageRequest req = new DisbursementStageRequest();
            req.setStageNumber(1);
            req.setAmount(new BigDecimal("99999.00"));
            req.setMilestone("Too big");
            req.setDueDate(LocalDate.now().plusDays(30));

            mockMvc.perform(post("/api/disbursements/1/stages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().is5xxServerError());
        }
    }

    // ── GET /api/disbursements/{planId}/stages ────────────────────────────────

    @Nested
    @DisplayName("GET /api/disbursements/{planId}/stages")
    class GetStages {

        @Test
        @DisplayName("returns 200 OK with list of stages")
        void stagesFound_returns200WithList() throws Exception {
            DisbursementStage s1 = stage(1L, 1, DisbursementStageStatus.PENDING);
            DisbursementStage s2 = stage(2L, 2, DisbursementStageStatus.VERIFIED);
            given(disbursementStageService.getStages(1L)).willReturn(List.of(s1, s2));

            mockMvc.perform(get("/api/disbursements/1/stages"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[1].id").value(2));
        }

        @Test
        @DisplayName("returns 200 OK with empty list when no stages")
        void noStages_returns200WithEmptyList() throws Exception {
            given(disbursementStageService.getStages(1L)).willReturn(List.of());

            mockMvc.perform(get("/api/disbursements/1/stages"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ── PUT /api/disbursements/stages/{stageId}/verify ────────────────────────

    @Nested
    @DisplayName("PUT /api/disbursements/stages/{stageId}/verify")
    class VerifyStage {

        @Test
        @DisplayName("returns 200 OK with verified stage")
        void validStage_returns200WithVerifiedStage() throws Exception {
            DisbursementStage verified = stage(1L, 1, DisbursementStageStatus.VERIFIED);
            given(disbursementStageService.verifyStage(1L)).willReturn(verified);

            mockMvc.perform(put("/api/disbursements/stages/1/verify"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("VERIFIED"));
        }

        @Test
        @DisplayName("returns 500 when service throws for already-released stage")
        void alreadyReleasedStage_returns500() throws Exception {
            given(disbursementStageService.verifyStage(1L))
                    .willThrow(new IllegalStateException("Stage has already been released."));

            mockMvc.perform(put("/api/disbursements/stages/1/verify"))
                    .andExpect(status().is5xxServerError());
        }
    }

    // ── PUT /api/disbursements/stages/{stageId}/release ───────────────────────

    @Nested
    @DisplayName("PUT /api/disbursements/stages/{stageId}/release")
    class ReleaseStage {

        @Test
        @DisplayName("returns 200 OK with released stage")
        void verifiedStage_returns200WithReleasedStage() throws Exception {
            DisbursementStage released = stage(1L, 1, DisbursementStageStatus.RELEASED);
            given(disbursementStageService.releaseStage(1L)).willReturn(released);

            mockMvc.perform(put("/api/disbursements/stages/1/release"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RELEASED"));
        }

        @Test
        @DisplayName("returns 500 when service throws for non-verified stage")
        void pendingStage_returns500() throws Exception {
            given(disbursementStageService.releaseStage(1L))
                    .willThrow(new IllegalStateException("Stage must be VERIFIED before amount can be released."));

            mockMvc.perform(put("/api/disbursements/stages/1/release"))
                    .andExpect(status().is5xxServerError());
        }
    }

    // ── PUT /api/disbursements/stages/{stageId}/compliance ────────────────────

    @Nested
    @DisplayName("PUT /api/disbursements/stages/{stageId}/compliance")
    class UpdateComplianceStatus {

        @Test
        @DisplayName("returns 200 OK with COMPLETED compliance status")
        void completedStatus_returns200() throws Exception {
            DisbursementStage s = stage(1L, 1, DisbursementStageStatus.PENDING);
            s.setComplianceStatus(ComplianceStatus.COMPLETED);
            given(disbursementStageService.updateComplianceStatus(1L, ComplianceStatus.COMPLETED))
                    .willReturn(s);

            mockMvc.perform(put("/api/disbursements/stages/1/compliance")
                            .param("status", "COMPLETED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.complianceStatus").value("COMPLETED"));
        }

        @Test
        @DisplayName("returns 200 OK with NON_COMPLIANT compliance status")
        void nonCompliantStatus_returns200() throws Exception {
            DisbursementStage s = stage(1L, 1, DisbursementStageStatus.PENDING);
            s.setComplianceStatus(ComplianceStatus.NON_COMPLIANT);
            given(disbursementStageService.updateComplianceStatus(1L, ComplianceStatus.NON_COMPLIANT))
                    .willReturn(s);

            mockMvc.perform(put("/api/disbursements/stages/1/compliance")
                            .param("status", "NON_COMPLIANT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.complianceStatus").value("NON_COMPLIANT"));
        }
    }
}
