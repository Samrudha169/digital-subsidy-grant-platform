package com.dsgp.disbursement.service;

import com.dsgp.disbursement.dto.DisbursementAnalyticsResponse;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Generates a downloadable PDF report of disbursement analytics
 * using OpenPDF (com.github.librepdf:openpdf).
 *
 * <p>Three sections are produced:
 * <ol>
 *   <li>Summary       – sanctioned, planned, released, remaining</li>
 *   <li>By Scheme     – released amount per scheme</li>
 *   <li>By State      – released amount per state</li>
 * </ol>
 *
 * All data comes from {@link DisbursementAnalyticsService#getAnalytics()}.
 * No analytics calculations are performed here.
 */
@Service
@RequiredArgsConstructor
public class DisbursementPdfExportService {

    private final DisbursementAnalyticsService analyticsService;

    // ── Font constants ─────────────────────────────────────────────────────────

    private static final Font FONT_TITLE   = new Font(Font.HELVETICA, 18, Font.BOLD,  new Color(30, 30, 60));
    private static final Font FONT_SECTION = new Font(Font.HELVETICA, 13, Font.BOLD,  new Color(30, 30, 60));
    private static final Font FONT_HEADER  = new Font(Font.HELVETICA, 11, Font.BOLD,  Color.WHITE);
    private static final Font FONT_BODY    = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
    private static final Font FONT_BODY_B  = new Font(Font.HELVETICA, 10, Font.BOLD,  Color.DARK_GRAY);
    private static final Font FONT_META    = new Font(Font.HELVETICA, 10, Font.ITALIC, Color.GRAY);

    private static final Color COLOR_HEADER_BG = new Color(55, 65, 100);
    private static final Color COLOR_ROW_ALT   = new Color(245, 246, 250);

    /**
     * Generates the PDF and returns its raw bytes.
     *
     * @return byte array containing a valid PDF file
     * @throws IOException if document serialisation fails
     */
    public byte[] generatePdfReport() throws IOException {

        DisbursementAnalyticsResponse data = analyticsService.getAnalytics();

        Document document = new Document(PageSize.A4, 40, 40, 50, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            addTitle(document);
            addSummarySection(document, data);
            addBreakdownSection(document, "Released by Scheme",
                    "Scheme", data.getReleasedByScheme());
            addBreakdownSection(document, "Released by State",
                    "State", data.getReleasedByState());

        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }

        return out.toByteArray();
    }

    // ── Section builders ───────────────────────────────────────────────────────

    private void addTitle(Document doc) throws DocumentException {

        Paragraph title = new Paragraph("Disbursement Analytics Report", FONT_TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4);
        doc.add(title);

        Paragraph dateLine = new Paragraph(
                "Generated on: " + LocalDate.now(), FONT_META);
        dateLine.setAlignment(Element.ALIGN_CENTER);
        dateLine.setSpacingAfter(20);
        doc.add(dateLine);
    }

    private void addSummarySection(Document doc,
                                   DisbursementAnalyticsResponse data) throws DocumentException {

        doc.add(sectionHeading("Summary"));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{55f, 45f});
        table.setSpacingAfter(18);

        addHeaderRow(table, "Metric", "Amount (INR)");
        addDataRow(table, "Total Sanctioned", fmt(data.getTotalSanctioned()), false);
        addDataRow(table, "Total Planned",    fmt(data.getTotalPlanned()),    true);
        addDataRow(table, "Total Released",   fmt(data.getTotalReleased()),   false);
        addDataRow(table, "Total Remaining",  fmt(data.getTotalRemaining()),  true);

        doc.add(table);
    }

    private void addBreakdownSection(Document doc, String heading,
                                     String keyLabel,
                                     Map<String, BigDecimal> breakdown) throws DocumentException {

        doc.add(sectionHeading(heading));

        if (breakdown == null || breakdown.isEmpty()) {
            Paragraph empty = new Paragraph("No data available.", FONT_META);
            empty.setSpacingAfter(14);
            doc.add(empty);
            return;
        }

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{60f, 40f});
        table.setSpacingAfter(18);

        addHeaderRow(table, keyLabel, "Released (INR)");

        boolean[] alt = {false};
        breakdown.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .forEach(e -> {
                    addDataRow(table, e.getKey(), fmt(e.getValue()), alt[0]);
                    alt[0] = !alt[0];
                });

        doc.add(table);
    }

    // ── Table helpers ──────────────────────────────────────────────────────────

    private void addHeaderRow(PdfPTable table, String col1, String col2) {

        PdfPCell c1 = headerCell(col1);
        PdfPCell c2 = headerCell(col2);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c1);
        table.addCell(c2);
    }

    private void addDataRow(PdfPTable table, String label,
                            String value, boolean alternate) {

        Color bg = alternate ? COLOR_ROW_ALT : Color.WHITE;

        PdfPCell labelCell = new PdfPCell(new Phrase(label, FONT_BODY));
        labelCell.setBackgroundColor(bg);
        labelCell.setPadding(6);
        labelCell.setBorderColor(new Color(220, 220, 220));

        PdfPCell valueCell = new PdfPCell(new Phrase(value, FONT_BODY_B));
        valueCell.setBackgroundColor(bg);
        valueCell.setPadding(6);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setBorderColor(new Color(220, 220, 220));

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_HEADER));
        cell.setBackgroundColor(COLOR_HEADER_BG);
        cell.setPadding(8);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private Paragraph sectionHeading(String text) {
        Paragraph p = new Paragraph(text, FONT_SECTION);
        p.setSpacingBefore(10);
        p.setSpacingAfter(8);
        return p;
    }

    // ── Amount formatter ───────────────────────────────────────────────────────

    private String fmt(BigDecimal value) {
        if (value == null) return "₹0.00";
        return String.format("₹%,.2f", value);
    }
}
