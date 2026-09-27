package com.dsgp.disbursement.service;

import com.dsgp.disbursement.repository.DisbursementPlanRepository;
import com.dsgp.disbursement.repository.DisbursementStageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;

/**
 * Unit tests for {@link DisbursementPdfExportService}.
 *
 * <p>OpenPDF generates a compressed (FlateDecode) PDF; content streams are
 * zlib-compressed and cannot be searched as plain text in raw bytes.
 * Structural assertions are used instead: they target metadata and cross-
 * reference markers that OpenPDF writes as uncompressed ASCII in every PDF.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DisbursementPdfExportService")
class DisbursementPdfExportServiceTest {

    // Real analytics service with mocked repositories ─────────────────────────
    @Mock
    private DisbursementPlanRepository disbursementPlanRepository;

    @Mock
    private DisbursementStageRepository disbursementStageRepository;

    @InjectMocks
    private DisbursementAnalyticsService analyticsService;

    // Service under test ──────────────────────────────────────────────────────
    private DisbursementPdfExportService pdfExportService;

    @BeforeEach
    void setUp() {
        given(disbursementPlanRepository.findAll()).willReturn(List.of());
        given(disbursementStageRepository.findAll()).willReturn(List.of());

        pdfExportService = new DisbursementPdfExportService(analyticsService);
    }

    // ── Generation ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generatePdfReport()")
    class GeneratePdfReport {

        @Test
        @DisplayName("returns a non-null, non-empty byte array")
        void generatedOutput_isNonEmpty() throws IOException {
            byte[] result = pdfExportService.generatePdfReport();

            assertThat(result).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("does not throw when no disbursement data exists")
        void noData_doesNotThrow() {
            assertThatCode(() -> pdfExportService.generatePdfReport())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("output starts with the PDF magic bytes (%PDF)")
        void output_startsWithPdfMagicBytes() throws IOException {
            byte[] result = pdfExportService.generatePdfReport();

            assertThat(result).hasSizeGreaterThan(4);
            String header = new String(result, 0, 4, java.nio.charset.StandardCharsets.US_ASCII);
            assertThat(header).isEqualTo("%PDF");
        }

        @Test
        @DisplayName("output is large enough to contain meaningful content")
        void output_hasMinimumContentSize() throws IOException {
            byte[] result = pdfExportService.generatePdfReport();

            // A bare PDF with title, date, and tables is well over 1 KB
            assertThat(result.length).isGreaterThan(1024);
        }

        @Test
        @DisplayName("output ends with the PDF end-of-file marker (%%EOF)")
        void output_endsWithEofMarker() throws IOException {
            byte[] result = pdfExportService.generatePdfReport();

            // %%EOF is written as ASCII at the very end of every valid PDF
            String tail = new String(result, Math.max(0, result.length - 20),
                    Math.min(20, result.length), java.nio.charset.StandardCharsets.ISO_8859_1);
            assertThat(tail).contains("%%EOF");
        }

        @Test
        @DisplayName("output contains the PDF xref keyword (cross-reference table)")
        void output_containsXrefKeyword() throws IOException {
            byte[] result = pdfExportService.generatePdfReport();

            String raw = new String(result, java.nio.charset.StandardCharsets.ISO_8859_1);
            assertThat(raw).contains("xref");
        }

        @Test
        @DisplayName("output contains the PDF startxref keyword")
        void output_containsStartxrefKeyword() throws IOException {
            byte[] result = pdfExportService.generatePdfReport();

            String raw = new String(result, java.nio.charset.StandardCharsets.ISO_8859_1);
            assertThat(raw).contains("startxref");
        }

        @Test
        @DisplayName("output contains the OpenPDF producer string in the Info dictionary")
        void output_containsOpenPdfProducerString() throws IOException {
            byte[] result = pdfExportService.generatePdfReport();

            // OpenPDF writes /Producer(OpenPDF x.y.z) uncompressed in the trailer
            String raw = new String(result, java.nio.charset.StandardCharsets.ISO_8859_1);
            assertThat(raw).contains("OpenPDF");
        }
    }
}
