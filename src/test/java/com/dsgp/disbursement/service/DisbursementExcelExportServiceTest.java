package com.dsgp.disbursement.service;

import com.dsgp.disbursement.dto.DisbursementAnalyticsResponse;
import com.dsgp.disbursement.repository.DisbursementPlanRepository;
import com.dsgp.disbursement.repository.DisbursementStageRepository;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;

/**
 * Unit tests for {@link DisbursementExcelExportService}.
 *
 * <p>These tests use a real {@link DisbursementAnalyticsService} with mocked
 * repositories to produce real analytics data, then verify the generated XLSX
 * workbook structure and content.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DisbursementExcelExportService")
class DisbursementExcelExportServiceTest {

    // Real analytics service with mocked repositories ─────────────────────────
    @Mock
    private DisbursementPlanRepository disbursementPlanRepository;

    @Mock
    private DisbursementStageRepository disbursementStageRepository;

    @InjectMocks
    private DisbursementAnalyticsService analyticsService;

    // The service under test — wired to the real analyticsService ─────────────
    private DisbursementExcelExportService excelExportService;

    @BeforeEach
    void setUp() {
        // Stub repositories to return empty collections so analytics returns zeroed data
        given(disbursementPlanRepository.findAll()).willReturn(List.of());
        given(disbursementStageRepository.findAll()).willReturn(List.of());

        excelExportService = new DisbursementExcelExportService(analyticsService);
    }

    // ── Generation ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateExcelReport()")
    class GenerateExcelReport {

        @Test
        @DisplayName("returns a non-null, non-empty byte array")
        void generatedOutput_isNonEmpty() throws IOException {
            byte[] result = excelExportService.generateExcelReport();

            assertThat(result).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("does not throw when no disbursement data exists")
        void noData_doesNotThrow() {
            assertThatCode(() -> excelExportService.generateExcelReport())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("produces a valid XLSX workbook that Apache POI can open")
        void output_isValidXlsx() throws IOException {
            byte[] result = excelExportService.generateExcelReport();

            assertThatCode(() -> {
                try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(result))) {
                    assertThat(wb).isNotNull();
                }
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("workbook contains exactly 3 sheets")
        void workbook_hasExactlyThreeSheets() throws IOException {
            byte[] result = excelExportService.generateExcelReport();

            try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(result))) {
                assertThat(wb.getNumberOfSheets()).isEqualTo(3);
            }
        }

        @Test
        @DisplayName("first sheet is named 'Summary'")
        void workbook_firstSheetIsSummary() throws IOException {
            byte[] result = excelExportService.generateExcelReport();

            try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(result))) {
                assertThat(wb.getSheetName(0)).isEqualTo("Summary");
            }
        }

        @Test
        @DisplayName("second sheet is named 'Released by Scheme'")
        void workbook_secondSheetIsReleasedByScheme() throws IOException {
            byte[] result = excelExportService.generateExcelReport();

            try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(result))) {
                assertThat(wb.getSheetName(1)).isEqualTo("Released by Scheme");
            }
        }

        @Test
        @DisplayName("third sheet is named 'Released by State'")
        void workbook_thirdSheetIsReleasedByState() throws IOException {
            byte[] result = excelExportService.generateExcelReport();

            try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(result))) {
                assertThat(wb.getSheetName(2)).isEqualTo("Released by State");
            }
        }

        @Test
        @DisplayName("Summary sheet has a header row with 'Metric' and 'Amount (INR)'")
        void summarySheet_hasColumnHeaders() throws IOException {
            byte[] result = excelExportService.generateExcelReport();

            try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(result))) {
                var sheet = wb.getSheetAt(0);
                // Row 0 = title, Row 1 = blank, Row 2 = header
                var headerRow = sheet.getRow(2);
                assertThat(headerRow).isNotNull();
                assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("Metric");
                assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("Amount (INR)");
            }
        }

        @Test
        @DisplayName("Summary sheet contains the 4 standard metric rows")
        void summarySheet_containsFourMetricRows() throws IOException {
            byte[] result = excelExportService.generateExcelReport();

            try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(result))) {
                var sheet = wb.getSheetAt(0);
                // Rows 3-6 are data rows: Sanctioned, Planned, Released, Remaining
                assertThat(sheet.getRow(3).getCell(0).getStringCellValue())
                        .isEqualTo("Total Sanctioned");
                assertThat(sheet.getRow(4).getCell(0).getStringCellValue())
                        .isEqualTo("Total Planned");
                assertThat(sheet.getRow(5).getCell(0).getStringCellValue())
                        .isEqualTo("Total Released");
                assertThat(sheet.getRow(6).getCell(0).getStringCellValue())
                        .isEqualTo("Total Remaining");
            }
        }

        @Test
        @DisplayName("breakdown sheets show 'No data available' when maps are empty")
        void breakdownSheets_showNoDataMessageWhenEmpty() throws IOException {
            byte[] result = excelExportService.generateExcelReport();

            try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(result))) {
                // Sheet 1 (Released by Scheme), row 3 = first data row
                var schemeSheet = wb.getSheetAt(1);
                assertThat(schemeSheet.getRow(3).getCell(0).getStringCellValue())
                        .contains("No data available");
            }
        }
    }
}
