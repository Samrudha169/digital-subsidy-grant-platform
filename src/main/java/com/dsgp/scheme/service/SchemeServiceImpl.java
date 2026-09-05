package com.dsgp.scheme.service;

import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.beneficiary.repository.SchemeRepository;
import com.dsgp.scheme.dto.SchemeRequest;
import com.dsgp.scheme.dto.SchemeResponse;
import com.dsgp.scheme.exception.SchemeNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link SchemeService}.
 *
 * <p>All public methods accept / return DTOs. The raw {@link Scheme} entity
 * is never returned to the controller layer.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SchemeServiceImpl implements SchemeService {

    private final SchemeRepository schemeRepository;

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    public SchemeResponse createScheme(SchemeRequest request) {
        Scheme scheme = toEntity(request);
        return toResponse(schemeRepository.save(scheme));
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<SchemeResponse> getAllActiveSchemes() {
        return schemeRepository.findByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SchemeResponse getSchemeById(Long id) {
        return toResponse(findOrThrow(id));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    public SchemeResponse updateScheme(Long id, SchemeRequest request) {
        Scheme existing = findOrThrow(id);

        existing.setSchemeName(request.getSchemeName());
        existing.setDescription(request.getDescription());
        existing.setMinAge(request.getMinAge());
        existing.setMaxAge(request.getMaxAge());
        existing.setMaxAnnualIncome(request.getMaxAnnualIncome());
        existing.setMaxLandHolding(request.getMaxLandHolding());
        existing.setRequiredCategory(request.getRequiredCategory());
        existing.setGrantAmount(request.getGrantAmount());
        if (request.getActive() != null) {
            existing.setActive(request.getActive());
        }

        return toResponse(schemeRepository.save(existing));
    }

    // ── Deactivate (soft delete) ───────────────────────────────────────────────

    @Override
    public void deactivateScheme(Long id) {
        Scheme existing = findOrThrow(id);
        existing.setActive(false);
        schemeRepository.save(existing);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Scheme findOrThrow(Long id) {
        return schemeRepository.findById(id)
                .orElseThrow(() -> new SchemeNotFoundException(id));
    }

    private Scheme toEntity(SchemeRequest request) {
        Scheme s = new Scheme();
        s.setSchemeName(request.getSchemeName());
        s.setDescription(request.getDescription());
        s.setMinAge(request.getMinAge());
        s.setMaxAge(request.getMaxAge());
        s.setMaxAnnualIncome(request.getMaxAnnualIncome());
        s.setMaxLandHolding(request.getMaxLandHolding());
        s.setRequiredCategory(request.getRequiredCategory());
        s.setGrantAmount(request.getGrantAmount());
        s.setActive(request.getActive() != null ? request.getActive() : true);
        return s;
    }

    private SchemeResponse toResponse(Scheme s) {
        return SchemeResponse.builder()
                .id(s.getId())
                .schemeName(s.getSchemeName())
                .description(s.getDescription())
                .minAge(s.getMinAge())
                .maxAge(s.getMaxAge())
                .maxAnnualIncome(s.getMaxAnnualIncome())
                .maxLandHolding(s.getMaxLandHolding())
                .requiredCategory(s.getRequiredCategory())
                .grantAmount(s.getGrantAmount())
                .active(s.getActive())
                .build();
    }
}