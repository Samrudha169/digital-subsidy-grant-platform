package com.dsgp.disbursement.controller;

import com.dsgp.GovernmentSchemeApplication;
import com.dsgp.beneficiary.security.SecurityConfig;
import com.dsgp.config.ApiErrorResponse;
import com.dsgp.config.GlobalExceptionHandler;
import com.dsgp.disbursement.dto.DisbursementAnalyticsResponse;
import com.dsgp.disbursement.service.DisbursementAnalyticsService;
import com.dsgp.disbursement.service.DisbursementExcelExportService;
import com.dsgp.disbursement.service.DisbursementPdfExportService;
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

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc slice tests for {@link DisbursementAnalyticsController}.
 */
@WebMvcTest(controllers = DisbursementAnalyticsController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ApiErrorResponse.class})
@ContextConfiguration(classes = {
        GovernmentSchemeApplication.class,
        DisbursementAnalyticsController.class,
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        ApiErrorResponse.class
})
@ActiveProfiles("test")
@WithMockUser
@DisplayName("DisbursementAnalyticsController")
class DisbursementAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DisbursementAnalyticsService disbursementAnalyticsService;

    @MockBean
    private DisbursementExcelExportService disbursementExcelExportService;

    @MockBean
    private DisbursementPdfExportService disbursementPdfExportService;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private DisbursementAnalyticsResponse analyticsWithData() {
        return DisbursementAnalyticsResponse.builder()
                .totalSanctioned(new BigDecimal("56000.00"))
                .totalPlanned(new BigDecimal("30000.00"))
                .totalReleased(new BigDecimal("10000.00"))
                .totalRemaining(new BigDecimal("46000.00"))
                .releasedByScheme(Map.of("PM-KISAN", new BigDecimal("6000.00"),
                        "NSP", new BigDecimal("4000.00")))
                .releasedByState(Map.of("Maharashtra", new BigDecimal("10000.00")))
                .build();
    }

    private DisbursementAnalyticsResponse emptyAnalytics() {
        return DisbursementAnalyticsResponse.builder()
                .totalSanctioned(BigDecimal.ZERO)
                .totalPlanned(BigDecimal.ZERO)
                .totalReleased(BigDecimal.ZERO)
                .totalRemaining(BigDecimal.ZERO)
                .releasedByScheme(Map.of())
                .releasedByState(Map.of())
                .build();
    }

    // ── GET /disbursements/analytics ──────────────────────────────────────────

    @Nested
    @DisplayName("GET /disbursements/analytics")
    class GetAnalytics {

        @Test
        @DisplayName("returns 200 OK with all four summary totals")
        void returnsOkWithSummaryTotals() throws Exception {
            given(disbursementAnalyticsService.getAnalytics()).willReturn(analyticsWithData());

            mockMvc.perform(get("/disbursements/analytics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalSanctioned").value(56000.00))
                    .andExpect(jsonPath("$.totalPlanned").value(30000.00))
                    .andExpect(jsonPath("$.totalReleased").value(10000.00))
                    .andExpect(jsonPath("$.totalRemaining").value(46000.00));
        }

        @Test
        @DisplayName("returns scheme-wise breakdown in response")
        void returnsReleasedByScheme() throws Exception {
            given(disbursementAnalyticsService.getAnalytics()).willReturn(analyticsWithData());

            mockMvc.perform(get("/disbursements/analytics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.releasedByScheme").isMap())
                    .andExpect(jsonPath("$.releasedByScheme['PM-KISAN']").value(6000.00))
                    .andExpect(jsonPath("$.releasedByScheme['NSP']").value(4000.00));
        }

        @Test
        @DisplayName("returns state-wise breakdown in response")
        void returnsReleasedByState() throws Exception {
            given(disbursementAnalyticsService.getAnalytics()).willReturn(analyticsWithData());

            mockMvc.perform(get("/disbursements/analytics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.releasedByState").isMap())
                    .andExpect(jsonPath("$.releasedByState['Maharashtra']").value(10000.00));
        }

        @Test
        @DisplayName("returns 200 OK with zeros when no disbursement data exists")
        void noData_returnsZeroTotals() throws Exception {
            given(disbursementAnalyticsService.getAnalytics()).willReturn(emptyAnalytics());

            mockMvc.perform(get("/disbursements/analytics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalSanctioned").value(0))
                    .andExpect(jsonPath("$.totalReleased").value(0))
                    .andExpect(jsonPath("$.totalRemaining").value(0));
        }
    }

    // ── GET /disbursements/analytics/export/excel ─────────────────────────────

    @Nested
    @DisplayName("GET /disbursements/analytics/export/excel")
    class ExportExcel {

        @Test
        @DisplayName("returns 200 OK with XLSX content type")
        void returns200WithXlsxContentType() throws Exception {
            byte[] xlsxBytes = new byte[]{0x50, 0x4B, 0x03, 0x04}; // PK zip magic
            given(disbursementExcelExportService.generateExcelReport()).willReturn(xlsxBytes);

            mockMvc.perform(get("/disbursements/analytics/export/excel"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }

        @Test
        @DisplayName("returns Content-Disposition attachment header with .xlsx filename")
        void returns200WithContentDispositionHeader() throws Exception {
            byte[] xlsxBytes = new byte[]{0x50, 0x4B, 0x03, 0x04};
            given(disbursementExcelExportService.generateExcelReport()).willReturn(xlsxBytes);

            mockMvc.perform(get("/disbursements/analytics/export/excel"))
                    .andExpect(header().string("Content-Disposition",
                            containsString("disbursement-analytics.xlsx")));
        }

        @Test
        @DisplayName("returns non-empty body")
        void returns200WithNonEmptyBody() throws Exception {
            byte[] xlsxBytes = new byte[]{0x50, 0x4B, 0x03, 0x04, 0x01, 0x02};
            given(disbursementExcelExportService.generateExcelReport()).willReturn(xlsxBytes);

            mockMvc.perform(get("/disbursements/analytics/export/excel"))
                    .andExpect(status().isOk())
                    .andExpect(content().bytes(xlsxBytes));
        }
    }

    // ── GET /disbursements/analytics/export/pdf ───────────────────────────────

    @Nested
    @DisplayName("GET /disbursements/analytics/export/pdf")
    class ExportPdf {

        @Test
        @DisplayName("returns 200 OK with PDF content type")
        void returns200WithPdfContentType() throws Exception {
            byte[] pdfBytes = "%PDF-1.4 test".getBytes();
            given(disbursementPdfExportService.generatePdfReport()).willReturn(pdfBytes);

            mockMvc.perform(get("/disbursements/analytics/export/pdf"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF));
        }

        @Test
        @DisplayName("returns Content-Disposition attachment header with .pdf filename")
        void returns200WithContentDispositionHeader() throws Exception {
            byte[] pdfBytes = "%PDF-1.4 test".getBytes();
            given(disbursementPdfExportService.generatePdfReport()).willReturn(pdfBytes);

            mockMvc.perform(get("/disbursements/analytics/export/pdf"))
                    .andExpect(header().string("Content-Disposition",
                            containsString("disbursement-analytics.pdf")));
        }

        @Test
        @DisplayName("returns non-empty body")
        void returns200WithNonEmptyBody() throws Exception {
            byte[] pdfBytes = "%PDF-1.4".getBytes();
            given(disbursementPdfExportService.generatePdfReport()).willReturn(pdfBytes);

            mockMvc.perform(get("/disbursements/analytics/export/pdf"))
                    .andExpect(status().isOk())
                    .andExpect(content().bytes(pdfBytes));
        }
    }
}
