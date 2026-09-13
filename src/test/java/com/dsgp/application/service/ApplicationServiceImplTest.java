package com.dsgp.application.service;

import com.dsgp.application.dto.ApplicationRequest;
import com.dsgp.application.dto.ApplicationResponse;
import com.dsgp.application.exception.ApplicationException;
import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.application.entity.SchemeApplication;
import com.dsgp.beneficiary.exception.BeneficiaryNotFoundException;
import com.dsgp.beneficiary.repository.BeneficiaryRepository;
import com.dsgp.beneficiary.repository.SchemeApplicationRepository;
import com.dsgp.beneficiary.repository.SchemeRepository;
import com.dsgp.eligibility.entity.EligibilityResult;
import com.dsgp.eligibility.entity.EligibilityStatus;
import com.dsgp.eligibility.repository.EligibilityResultRepository;
import com.dsgp.scheme.exception.SchemeNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link ApplicationServiceImpl}.
 *
 * <p>All repository interactions are mocked. Tests verify the five
 * business rules and the mapping of the saved entity to the response DTO.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationServiceImpl")
class ApplicationServiceImplTest {

    @Mock private BeneficiaryRepository      beneficiaryRepository;
    @Mock private SchemeRepository           schemeRepository;
    @Mock private EligibilityResultRepository eligibilityResultRepository;
    @Mock private SchemeApplicationRepository applicationRepository;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static final Integer BENEFICIARY_ID = 101;
    private static final Long    SCHEME_ID      = 1L;

    private Beneficiary beneficiary() {
        Beneficiary b = new Beneficiary();
        b.setId(BENEFICIARY_ID);
        b.setFullName("Ravi Kumar");
        return b;
    }

    private Scheme scheme() {
        Scheme s = new Scheme();
        s.setId(SCHEME_ID);
        s.setSchemeName("PM-KISAN Samman Nidhi");
        s.setGrantAmount(new BigDecimal("6000.00"));
        s.setActive(true);
        return s;
    }

    private EligibilityResult eligibleResult() {
        EligibilityResult r = new EligibilityResult();
        r.setBeneficiaryId(BENEFICIARY_ID);
        r.setSchemeId(SCHEME_ID);
        r.setSchemeName("PM-KISAN Samman Nidhi");
        r.setTotalScore(80);
        r.setEligibilityStatus(EligibilityStatus.ELIGIBLE);
        r.setEvaluatedAt(LocalDateTime.now());
        return r;
    }

    private EligibilityResult ineligibleResult() {
        EligibilityResult r = eligibleResult();
        r.setTotalScore(40);
        r.setEligibilityStatus(EligibilityStatus.INELIGIBLE);
        return r;
    }

    private SchemeApplication savedApplication() {
        SchemeApplication app = SchemeApplication.builder()
                .beneficiary(beneficiary())
                .scheme(scheme())
                .applicationStatus("PENDING")
                .build();
        // Simulate @PrePersist
        try {
            java.lang.reflect.Field id = SchemeApplication.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(app, 999L);
            java.lang.reflect.Field date = SchemeApplication.class.getDeclaredField("applicationDate");
            date.setAccessible(true);
            date.set(app, LocalDateTime.now());
        } catch (Exception ignored) {}
        return app;
    }

    private ApplicationRequest request() {
        ApplicationRequest req = new ApplicationRequest();
        req.setBeneficiaryId(BENEFICIARY_ID);
        req.setSchemeId(SCHEME_ID);
        return req;
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("submitApplication — happy path")
    class HappyPath {

        @Test
        @DisplayName("returns ApplicationResponse with PENDING status and eligibility score when ELIGIBLE")
        void submit_eligible_returnsPendingResponse() {
            given(beneficiaryRepository.findById(BENEFICIARY_ID))
                    .willReturn(Optional.of(beneficiary()));
            given(schemeRepository.findById(SCHEME_ID))
                    .willReturn(Optional.of(scheme()));
            given(eligibilityResultRepository.findByBeneficiaryIdAndSchemeId(BENEFICIARY_ID, SCHEME_ID))
                    .willReturn(Optional.of(eligibleResult()));
            given(applicationRepository.findByBeneficiaryIdAndSchemeId(BENEFICIARY_ID, SCHEME_ID))
                    .willReturn(Optional.empty());
            given(applicationRepository.save(any(SchemeApplication.class)))
                    .willReturn(savedApplication());

            ApplicationResponse response = applicationService.submitApplication(request());

            assertThat(response.getApplicationStatus()).isEqualTo("PENDING");
            assertThat(response.getEligibilityScore()).isEqualTo(80);
            assertThat(response.getBeneficiaryId()).isEqualTo(BENEFICIARY_ID);
            assertThat(response.getSchemeId()).isEqualTo(SCHEME_ID);
            assertThat(response.getSchemeName()).isEqualTo("PM-KISAN Samman Nidhi");
            assertThat(response.getBeneficiaryName()).isEqualTo("Ravi Kumar");
        }

        @Test
        @DisplayName("persists SchemeApplication once on successful submission")
        void submit_eligible_savesApplicationOnce() {
            given(beneficiaryRepository.findById(BENEFICIARY_ID))
                    .willReturn(Optional.of(beneficiary()));
            given(schemeRepository.findById(SCHEME_ID))
                    .willReturn(Optional.of(scheme()));
            given(eligibilityResultRepository.findByBeneficiaryIdAndSchemeId(BENEFICIARY_ID, SCHEME_ID))
                    .willReturn(Optional.of(eligibleResult()));
            given(applicationRepository.findByBeneficiaryIdAndSchemeId(BENEFICIARY_ID, SCHEME_ID))
                    .willReturn(Optional.empty());
            given(applicationRepository.save(any(SchemeApplication.class)))
                    .willReturn(savedApplication());

            applicationService.submitApplication(request());

            then(applicationRepository).should(times(1)).save(any(SchemeApplication.class));
        }
    }

    // ── Beneficiary not found ─────────────────────────────────────────────────

    @Nested
    @DisplayName("submitApplication — beneficiary not found")
    class BeneficiaryNotFound {

        @Test
        @DisplayName("throws BeneficiaryNotFoundException when beneficiary ID is unknown")
        void submit_unknownBeneficiary_throws() {
            given(beneficiaryRepository.findById(BENEFICIARY_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.submitApplication(request()))
                    .isInstanceOf(BeneficiaryNotFoundException.class)
                    .hasMessageContaining(String.valueOf(BENEFICIARY_ID));
        }
    }

    // ── Scheme not found ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("submitApplication — scheme not found")
    class SchemeNotFound {

        @Test
        @DisplayName("throws SchemeNotFoundException when scheme ID is unknown")
        void submit_unknownScheme_throws() {
            given(beneficiaryRepository.findById(BENEFICIARY_ID))
                    .willReturn(Optional.of(beneficiary()));
            given(schemeRepository.findById(SCHEME_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.submitApplication(request()))
                    .isInstanceOf(SchemeNotFoundException.class)
                    .hasMessageContaining(String.valueOf(SCHEME_ID));
        }
    }

    // ── Eligibility not checked ───────────────────────────────────────────────

    @Nested
    @DisplayName("submitApplication — eligibility not checked")
    class EligibilityNotChecked {

        @Test
        @DisplayName("throws ApplicationException when no eligibility result exists")
        void submit_noEligibilityResult_throws() {
            given(beneficiaryRepository.findById(BENEFICIARY_ID))
                    .willReturn(Optional.of(beneficiary()));
            given(schemeRepository.findById(SCHEME_ID))
                    .willReturn(Optional.of(scheme()));
            given(eligibilityResultRepository.findByBeneficiaryIdAndSchemeId(BENEFICIARY_ID, SCHEME_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.submitApplication(request()))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining("Eligibility has not been checked");
        }
    }

    // ── Ineligible ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("submitApplication — ineligible beneficiary")
    class IneligibleBeneficiary {

        @Test
        @DisplayName("throws ApplicationException when eligibility status is INELIGIBLE")
        void submit_ineligible_throws() {
            given(beneficiaryRepository.findById(BENEFICIARY_ID))
                    .willReturn(Optional.of(beneficiary()));
            given(schemeRepository.findById(SCHEME_ID))
                    .willReturn(Optional.of(scheme()));
            given(eligibilityResultRepository.findByBeneficiaryIdAndSchemeId(BENEFICIARY_ID, SCHEME_ID))
                    .willReturn(Optional.of(ineligibleResult()));

            assertThatThrownBy(() -> applicationService.submitApplication(request()))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining("not eligible")
                    .hasMessageContaining("40");   // score appears in message
        }

        @Test
        @DisplayName("does NOT save application when beneficiary is INELIGIBLE")
        void submit_ineligible_doesNotSave() {
            given(beneficiaryRepository.findById(BENEFICIARY_ID))
                    .willReturn(Optional.of(beneficiary()));
            given(schemeRepository.findById(SCHEME_ID))
                    .willReturn(Optional.of(scheme()));
            given(eligibilityResultRepository.findByBeneficiaryIdAndSchemeId(BENEFICIARY_ID, SCHEME_ID))
                    .willReturn(Optional.of(ineligibleResult()));

            assertThatThrownBy(() -> applicationService.submitApplication(request()))
                    .isInstanceOf(ApplicationException.class);

            then(applicationRepository).should(never()).save(any());
        }
    }

    // ── Duplicate application ─────────────────────────────────────────────────

    @Nested
    @DisplayName("submitApplication — duplicate application")
    class DuplicateApplication {

        @Test
        @DisplayName("throws ApplicationException when application already exists")
        void submit_duplicate_throws() {
            given(beneficiaryRepository.findById(BENEFICIARY_ID))
                    .willReturn(Optional.of(beneficiary()));
            given(schemeRepository.findById(SCHEME_ID))
                    .willReturn(Optional.of(scheme()));
            given(eligibilityResultRepository.findByBeneficiaryIdAndSchemeId(BENEFICIARY_ID, SCHEME_ID))
                    .willReturn(Optional.of(eligibleResult()));
            given(applicationRepository.findByBeneficiaryIdAndSchemeId(BENEFICIARY_ID, SCHEME_ID))
                    .willReturn(Optional.of(savedApplication()));

            assertThatThrownBy(() -> applicationService.submitApplication(request()))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("does NOT save a second application on duplicate attempt")
        void submit_duplicate_doesNotSave() {
            given(beneficiaryRepository.findById(BENEFICIARY_ID))
                    .willReturn(Optional.of(beneficiary()));
            given(schemeRepository.findById(SCHEME_ID))
                    .willReturn(Optional.of(scheme()));
            given(eligibilityResultRepository.findByBeneficiaryIdAndSchemeId(BENEFICIARY_ID, SCHEME_ID))
                    .willReturn(Optional.of(eligibleResult()));
            given(applicationRepository.findByBeneficiaryIdAndSchemeId(BENEFICIARY_ID, SCHEME_ID))
                    .willReturn(Optional.of(savedApplication()));

            assertThatThrownBy(() -> applicationService.submitApplication(request()))
                    .isInstanceOf(ApplicationException.class);

            then(applicationRepository).should(never()).save(any());
        }
    }
}
