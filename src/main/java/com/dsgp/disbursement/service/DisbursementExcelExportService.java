package com.dsgp.disbursement.service;

import com.dsgp.disbursement.dto.DisbursementAnalyticsResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Builds a downloadable .xlsx report from the existing
 * {@link DisbursementAnalyticsResponse} data.
 *
 * <p>Three sheets are produced:
 * <ol>
 *   <li>Summary       – sanctioned, planned, released, remaining</li>
 *   <li>By Scheme     – released amount per scheme</li>
 *   <li>By State      – released amount per state</li>
 * </ol>
 *
 * No analytics calculations are performed here; all figures come
 * from {@link DisbursementAnalyticsService#getAnalytics()}.
 */
@Service
@RequiredArgsConstructor
public class DisbursementExcelExportService {

    private final DisbursementAnalyticsService analyticsService;

    /**
     * Generates the workbook and returns its raw bytes.
     *
     * @return byte array containing a valid .xlsx file
     * @throws IOException if workbook serialisation fails
     */
    public byte[] generateExcelReport() throws IOException {

        DisbursementAnalyticsResponse data = analyticsService.getAnalytics();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            CellStyle headerStyle  = buildHeaderStyle(workbook);
            CellStyle labelStyle   = buildLabelStyle(workbook);
            CellStyle amountStyle  = buildAmountStyle(workbook);
            CellStyle titleStyle   = buildTitleStyle(workbook);

            writeSummarySheet(workbook, data, titleStyle, headerStyle, labelStyle, amountStyle);
            writeBreakdownSheet(workbook, "Released by Scheme",
                    data.getReleasedByScheme(), "Scheme",
                    titleStyle, headerStyle, labelStyle, amountStyle);
            writeBreakdownSheet(workbook, "Released by State",
                    data.getReleasedByState(), "State",
                    titleStyle, headerStyle, labelStyle, amountStyle);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ── Sheet builders ─────────────────────────────────────────────────────────

    private void writeSummarySheet(
            XSSFWorkbook workbook,
            DisbursementAnalyticsResponse data,
            CellStyle titleStyle,
            CellStyle headerStyle,
            CellStyle labelStyle,
            CellStyle amountStyle) {

        Sheet sheet = workbook.createSheet("Summary");
        sheet.setColumnWidth(0, 32 * 256);
        sheet.setColumnWidth(1, 22 * 256);

        int row = 0;

        // Title row
        Row titleRow = sheet.createRow(row++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Disbursement Analytics Report — " + LocalDate.now());
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        row++; // blank

        // Column headers
        Row hdrRow = sheet.createRow(row++);
        createCell(hdrRow, 0, "Metric",  headerStyle);
        createCell(hdrRow, 1, "Amount (INR)", headerStyle);

        // Data rows
        writeAmountRow(sheet, row++, "Total Sanctioned", data.getTotalSanctioned(), labelStyle, amountStyle);
        writeAmountRow(sheet, row++, "Total Planned",    data.getTotalPlanned(),    labelStyle, amountStyle);
        writeAmountRow(sheet, row++, "Total Released",   data.getTotalReleased(),   labelStyle, amountStyle);
        writeAmountRow(sheet, row,   "Total Remaining",  data.getTotalRemaining(),  labelStyle, amountStyle);
    }

    private void writeBreakdownSheet(
            XSSFWorkbook workbook,
            String sheetName,
            Map<String, BigDecimal> breakdown,
            String keyColumnLabel,
            CellStyle titleStyle,
            CellStyle headerStyle,
            CellStyle labelStyle,
            CellStyle amountStyle) {

        Sheet sheet = workbook.createSheet(sheetName);
        sheet.setColumnWidth(0, 36 * 256);
        sheet.setColumnWidth(1, 22 * 256);

        int row = 0;

        // Title
        Row titleRow = sheet.createRow(row++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(sheetName + " — " + LocalDate.now());
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        row++; // blank

        // Column headers
        Row hdrRow = sheet.createRow(row++);
        createCell(hdrRow, 0, keyColumnLabel,    headerStyle);
        createCell(hdrRow, 1, "Released (INR)",  headerStyle);

        if (breakdown == null || breakdown.isEmpty()) {
            Row emptyRow = sheet.createRow(row);
            createCell(emptyRow, 0, "No data available.", labelStyle);
            return;
        }

        // Data rows — sorted descending by amount
        breakdown.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .forEach(entry ->
                        writeAmountRow(sheet, sheet.getLastRowNum() + 1,
                                entry.getKey(), entry.getValue(), labelStyle, amountStyle)
                );
    }

    // ── Row / cell helpers ─────────────────────────────────────────────────────

    private void writeAmountRow(Sheet sheet, int rowIndex,
                                String label, BigDecimal amount,
                                CellStyle labelStyle, CellStyle amountStyle) {
        Row row = sheet.createRow(rowIndex);
        createCell(row, 0, label, labelStyle);
        Cell amtCell = row.createCell(1);
        amtCell.setCellValue(amount != null ? amount.doubleValue() : 0.0);
        amtCell.setCellStyle(amountStyle);
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    // ── Cell style factories ───────────────────────────────────────────────────

    private CellStyle buildTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }

    private CellStyle buildHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle buildLabelStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        return style;
    }

    private CellStyle buildAmountStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        return style;
    }
}
