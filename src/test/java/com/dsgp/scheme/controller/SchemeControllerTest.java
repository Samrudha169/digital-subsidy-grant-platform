package com.dsgp.scheme.controller;

import com.dsgp.GovernmentSchemeApplication;
import com.dsgp.beneficiary.security.SecurityConfig;
import com.dsgp.config.ApiErrorResponse;
import com.dsgp.config.GlobalExceptionHandler;
import com.dsgp.scheme.dto.SchemeRequest;
import com.dsgp.scheme.dto.SchemeResponse;
import com.dsgp.scheme.exception.SchemeNotFoundException;
import com.dsgp.scheme.service.SchemeService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc slice tests for {@link SchemeController}.
 *
 * <p>Uses {@code @WebMvcTest} to load only the web layer; the service
 * is fully mocked. Mirrors the pattern used in {@code BeneficiaryControllerTest}.
 */
@WebMvcTest(controllers = SchemeController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ApiErrorResponse.class})
@ContextConfiguration(classes = {GovernmentSchemeApplication.class, SchemeController.class,
        SecurityConfig.class, GlobalExceptionHandler.class, ApiErrorResponse.class})
@ActiveProfiles("test")
@WithMockUser
@DisplayName("SchemeController")
class SchemeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SchemeService schemeService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private SchemeResponse pmKisanResponse() {
        return SchemeResponse.builder()
                .id(1L)
                .schemeName("PM-KISAN Samman Nidhi")
                .description("Income support scheme")
                .minAge(18)
                .maxAge(60)
                .maxAnnualIncome(new BigDecimal("150000.00"))
                .maxLandHolding(new BigDecimal("2.0000"))
                .requiredCategory("SC/ST")
                .grantAmount(new BigDecimal("6000.00"))
                .active(true)
                .build();
    }

    private SchemeRequest pmKisanRequest() {
        SchemeRequest req = new SchemeRequest();
        req.setSchemeName("PM-KISAN Samman Nidhi");
        req.setDescription("Income support scheme");
        req.setMinAge(18);
        req.setMaxAge(60);
        req.setMaxAnnualIncome(new BigDecimal("150000.00"));
        req.setMaxLandHolding(new BigDecimal("2.0000"));
        req.setRequiredCategory("SC/ST");
        req.setGrantAmount(new BigDecimal("6000.00"));
        req.setActive(true);
        return req;
    }

    // ── POST /schemes ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /schemes")
    class PostScheme {

        @Test
        @DisplayName("returns 201 Created with scheme body on valid request")
        void createScheme_validRequest_returns201() throws Exception {
            given(schemeService.createScheme(any(SchemeRequest.class))).willReturn(pmKisanResponse());

            mockMvc.perform(post("/schemes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(pmKisanRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.schemeName").value("PM-KISAN Samman Nidhi"))
                    .andExpect(jsonPath("$.minAge").value(18))
                    .andExpect(jsonPath("$.maxAge").value(60))
                    .andExpect(jsonPath("$.grantAmount").value(6000.00))
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @DisplayName("returns 400 Bad Request when schemeName is blank")
        void createScheme_blankName_returns400() throws Exception {
            SchemeRequest req = new SchemeRequest();
            req.setSchemeName("   ");

            mockMvc.perform(post("/schemes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'schemeName')]").exists());
        }

        @Test
        @DisplayName("returns 400 Bad Request when schemeName is missing")
        void createScheme_missingName_returns400() throws Exception {
            SchemeRequest req = new SchemeRequest();

            mockMvc.perform(post("/schemes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── GET /schemes ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /schemes")
    class GetAllSchemes {

        @Test
        @DisplayName("returns 200 OK with list of active schemes")
        void getAllSchemes_returns200WithList() throws Exception {
            SchemeResponse nsp = SchemeResponse.builder()
                    .id(2L).schemeName("National Scholarship Portal").active(true).build();

            given(schemeService.getAllActiveSchemes()).willReturn(List.of(pmKisanResponse(), nsp));

            mockMvc.perform(get("/schemes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[1].id").value(2));
        }

        @Test
        @DisplayName("returns 200 OK with empty list when no active schemes")
        void getAllSchemes_empty_returns200EmptyList() throws Exception {
            given(schemeService.getAllActiveSchemes()).willReturn(List.of());

            mockMvc.perform(get("/schemes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ── GET /schemes/{id} ────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /schemes/{id}")
    class GetSchemeById {

        @Test
        @DisplayName("returns 200 OK with scheme when found")
        void getById_found_returns200() throws Exception {
            given(schemeService.getSchemeById(1L)).willReturn(pmKisanResponse());

            mockMvc.perform(get("/schemes/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.schemeName").value("PM-KISAN Samman Nidhi"))
                    .andExpect(jsonPath("$.requiredCategory").value("SC/ST"));
        }

        @Test
        @DisplayName("returns 404 Not Found when scheme does not exist")
        void getById_notFound_returns404() throws Exception {
            given(schemeService.getSchemeById(99L))
                    .willThrow(new SchemeNotFoundException(99L));

            mockMvc.perform(get("/schemes/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(containsString("99")));
        }
    }

    // ── PUT /schemes/{id} ────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /schemes/{id}")
    class PutScheme {

        @Test
        @DisplayName("returns 200 OK with updated scheme on valid request")
        void updateScheme_found_returns200() throws Exception {
            SchemeResponse updated = SchemeResponse.builder()
                    .id(1L).schemeName("PM-KISAN Updated").active(true).build();

            given(schemeService.updateScheme(eq(1L), any(SchemeRequest.class))).willReturn(updated);

            SchemeRequest req = pmKisanRequest();
            req.setSchemeName("PM-KISAN Updated");

            mockMvc.perform(put("/schemes/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.schemeName").value("PM-KISAN Updated"));
        }

        @Test
        @DisplayName("returns 404 Not Found when scheme does not exist")
        void updateScheme_notFound_returns404() throws Exception {
            given(schemeService.updateScheme(eq(99L), any(SchemeRequest.class)))
                    .willThrow(new SchemeNotFoundException(99L));

            mockMvc.perform(put("/schemes/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(pmKisanRequest())))
                    .andExpect(status().isNotFound());
        }
    }

    // ── DELETE /schemes/{id} ─────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /schemes/{id}")
    class DeleteScheme {

        @Test
        @DisplayName("returns 204 No Content on successful deactivation")
        void deactivateScheme_found_returns204() throws Exception {
            willDoNothing().given(schemeService).deactivateScheme(1L);

            mockMvc.perform(delete("/schemes/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("returns 404 Not Found when scheme does not exist")
        void deactivateScheme_notFound_returns404() throws Exception {
            willThrow(new SchemeNotFoundException(99L)).given(schemeService).deactivateScheme(99L);

            mockMvc.perform(delete("/schemes/99"))
                    .andExpect(status().isNotFound());
        }
    }
}
