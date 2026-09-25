package com.dsgp.disbursement.controller;

import com.dsgp.disbursement.dto.DisbursementAnalyticsResponse;
import com.dsgp.disbursement.service.DisbursementAnalyticsService;
import com.dsgp.disbursement.service.DisbursementExcelExportService;
import com.dsgp.disbursement.service.DisbursementPdfExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Exposes disbursement analytics.
 *
 * With server.servlet.context-path=/api/v1, the effective URL is:
 *   GET /api/v1/disbursements/analytics
 */
@RestController
@RequestMapping("/disbursements")
@RequiredArgsConstructor
public class DisbursementAnalyticsController {

    private final DisbursementAnalyticsService disbursementAnalyticsService;
    private final DisbursementExcelExportService disbursementExcelExportService;
    private final DisbursementPdfExportService disbursementPdfExportService;

    /*
     * GET /api/v1/disbursements/analytics
     *
     * Returns total sanctioned, planned, released, remaining amounts
     * and breakdowns by scheme and state.
     */
    @GetMapping("/analytics")
    public ResponseEntity<DisbursementAnalyticsResponse> getAnalytics() {
        return ResponseEntity.ok(
                disbursementAnalyticsService.getAnalytics()
        );
    }

    /*
     * GET /api/v1/disbursements/analytics/export/excel
     *
     * Generates and downloads a .xlsx report with three sheets:
     * Summary, Released by Scheme, Released by State.
     */
    @GetMapping("/analytics/export/excel")
    public ResponseEntity<byte[]> exportExcel() throws IOException {

        byte[] excelBytes = disbursementExcelExportService.generateExcelReport();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"disbursement-analytics.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    /*
     * GET /api/v1/disbursements/analytics/export/pdf
     *
     * Generates and downloads a PDF report with three sections:
     * Summary, Released by Scheme, Released by State.
     */
    @GetMapping("/analytics/export/pdf")
    public ResponseEntity<byte[]> exportPdf() throws IOException {

        byte[] pdfBytes = disbursementPdfExportService.generatePdfReport();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"disbursement-analytics.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
