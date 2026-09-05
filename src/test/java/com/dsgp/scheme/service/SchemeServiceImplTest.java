package com.dsgp.scheme.service;

import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.beneficiary.repository.SchemeRepository;
import com.dsgp.scheme.dto.SchemeRequest;
import com.dsgp.scheme.dto.SchemeResponse;
import com.dsgp.scheme.exception.SchemeNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link SchemeServiceImpl}.
 *
 * <p>All repository interactions are mocked. Tests verify service-layer
 * logic: mapping, delegation, and exception propagation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SchemeServiceImpl")
class SchemeServiceImplTest {

    @Mock
    private SchemeRepository schemeRepository;

    @InjectMocks
    private SchemeServiceImpl schemeService;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private Scheme pmKisanEntity() {
        Scheme s = new Scheme();
        s.setId(1L);
        s.setSchemeName("PM-KISAN Samman Nidhi");
        s.setDescription("Income support scheme");
        s.setMinAge(18);
        s.setMaxAge(60);
        s.setMaxAnnualIncome(new BigDecimal("150000.00"));
        s.setMaxLandHolding(new BigDecimal("2.0000"));
        s.setRequiredCategory("SC/ST");
        s.setGrantAmount(new BigDecimal("6000.00"));
        s.setActive(true);
        return s;
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

    // ── createScheme ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createScheme")
    class CreateScheme {

        @Test
        @DisplayName("persists scheme and returns mapped response")
        void createScheme_savesAndReturnsResponse() {
            Scheme saved = pmKisanEntity();
            given(schemeRepository.save(any(Scheme.class))).willReturn(saved);

            SchemeResponse result = schemeService.createScheme(pmKisanRequest());

            then(schemeRepository).should().save(any(Scheme.class));
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getSchemeName()).isEqualTo("PM-KISAN Samman Nidhi");
            assertThat(result.getMinAge()).isEqualTo(18);
            assertThat(result.getMaxAge()).isEqualTo(60);
            assertThat(result.getMaxAnnualIncome()).isEqualByComparingTo("150000.00");
            assertThat(result.getMaxLandHolding()).isEqualByComparingTo("2.0000");
            assertThat(result.getRequiredCategory()).isEqualTo("SC/ST");
            assertThat(result.getGrantAmount()).isEqualByComparingTo("6000.00");
            assertThat(result.getActive()).isTrue();
        }

        @Test
        @DisplayName("maps null thresholds (NSP-style) correctly")
        void createScheme_nullThresholds_areMappedAsNull() {
            Scheme nsp = new Scheme();
            nsp.setId(2L);
            nsp.setSchemeName("National Scholarship Portal");
            nsp.setActive(true);
            given(schemeRepository.save(any(Scheme.class))).willReturn(nsp);

            SchemeRequest req = new SchemeRequest();
            req.setSchemeName("National Scholarship Portal");
            req.setActive(true);

            SchemeResponse result = schemeService.createScheme(req);

            assertThat(result.getMinAge()).isNull();
            assertThat(result.getMaxAge()).isNull();
            assertThat(result.getMaxAnnualIncome()).isNull();
            assertThat(result.getMaxLandHolding()).isNull();
            assertThat(result.getRequiredCategory()).isNull();
            assertThat(result.getGrantAmount()).isNull();
        }
    }

    // ── getAllActiveSchemes ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllActiveSchemes")
    class GetAllActiveSchemes {

        @Test
        @DisplayName("returns all schemes from repository")
        void getAllActive_returnsAllSchemes() {
            given(schemeRepository.findByActiveTrue()).willReturn(List.of(pmKisanEntity()));

            List<SchemeResponse> result = schemeService.getAllActiveSchemes();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSchemeName()).isEqualTo("PM-KISAN Samman Nidhi");
        }

        @Test
        @DisplayName("returns empty list when no active schemes exist")
        void getAllActive_emptyList_whenNoneActive() {
            given(schemeRepository.findByActiveTrue()).willReturn(List.of());

            List<SchemeResponse> result = schemeService.getAllActiveSchemes();

            assertThat(result).isEmpty();
        }
    }

    // ── getSchemeById ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSchemeById")
    class GetSchemeById {

        @Test
        @DisplayName("returns scheme when found")
        void getById_found_returnsResponse() {
            given(schemeRepository.findById(1L)).willReturn(Optional.of(pmKisanEntity()));

            SchemeResponse result = schemeService.getSchemeById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getSchemeName()).isEqualTo("PM-KISAN Samman Nidhi");
        }

        @Test
        @DisplayName("throws SchemeNotFoundException when not found")
        void getById_notFound_throwsException() {
            given(schemeRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> schemeService.getSchemeById(99L))
                    .isInstanceOf(SchemeNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // ── updateScheme ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateScheme")
    class UpdateScheme {

        @Test
        @DisplayName("updates fields and returns updated response")
        void update_found_updatesAndReturns() {
            Scheme existing = pmKisanEntity();
            Scheme updated  = pmKisanEntity();
            updated.setSchemeName("PM-KISAN Updated");
            updated.setGrantAmount(new BigDecimal("7000.00"));

            given(schemeRepository.findById(1L)).willReturn(Optional.of(existing));
            given(schemeRepository.save(any(Scheme.class))).willReturn(updated);

            SchemeRequest req = pmKisanRequest();
            req.setSchemeName("PM-KISAN Updated");
            req.setGrantAmount(new BigDecimal("7000.00"));

            SchemeResponse result = schemeService.updateScheme(1L, req);

            assertThat(result.getSchemeName()).isEqualTo("PM-KISAN Updated");
            assertThat(result.getGrantAmount()).isEqualByComparingTo("7000.00");
        }

        @Test
        @DisplayName("throws SchemeNotFoundException when scheme does not exist")
        void update_notFound_throws() {
            given(schemeRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> schemeService.updateScheme(99L, pmKisanRequest()))
                    .isInstanceOf(SchemeNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // ── deactivateScheme ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("deactivateScheme")
    class DeactivateScheme {

        @Test
        @DisplayName("sets active=false and saves")
        void deactivate_setsActiveFalse() {
            Scheme active = pmKisanEntity();
            given(schemeRepository.findById(1L)).willReturn(Optional.of(active));
            given(schemeRepository.save(any(Scheme.class))).willAnswer(inv -> inv.getArgument(0));

            schemeService.deactivateScheme(1L);

            then(schemeRepository).should().save(argThat(s -> !s.getActive()));
        }

        @Test
        @DisplayName("throws SchemeNotFoundException when scheme not found")
        void deactivate_notFound_throws() {
            given(schemeRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> schemeService.deactivateScheme(99L))
                    .isInstanceOf(SchemeNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }
}
