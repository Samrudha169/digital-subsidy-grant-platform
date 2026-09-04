package com.dsgp.scheme.service;

import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.beneficiary.repository.SchemeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SchemeServiceImpl implements SchemeService {

    private final SchemeRepository schemeRepository;

    public SchemeServiceImpl(SchemeRepository schemeRepository) {
        this.schemeRepository = schemeRepository;
    }

    @Override
    public List<Scheme> getAllSchemes() {
        return schemeRepository.findAll();
    }

    @Override
    public List<Scheme> getActiveSchemes() {
        return schemeRepository.findByActiveTrue();
    }

    @Override
    public Scheme getSchemeById(Long schemeId) {
        return schemeRepository.findById(schemeId)
                .orElseThrow(() ->
                        new RuntimeException("Scheme not found"));
    }
}