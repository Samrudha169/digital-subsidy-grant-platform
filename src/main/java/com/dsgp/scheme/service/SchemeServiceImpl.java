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
    public Scheme createScheme(Scheme scheme) {
        return schemeRepository.save(scheme);
    }

    @Override
    public Scheme getSchemeById(Long id) {
        return schemeRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Scheme not found with id: " + id));
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
    public Scheme updateScheme(Long id, Scheme updatedScheme) {

        Scheme existingScheme = schemeRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Scheme not found with id: " + id));

        existingScheme.setSchemeName(updatedScheme.getSchemeName());
        existingScheme.setDescription(updatedScheme.getDescription());
        existingScheme.setMinAge(updatedScheme.getMinAge());
        existingScheme.setMaxAge(updatedScheme.getMaxAge());
        existingScheme.setMaxAnnualIncome(updatedScheme.getMaxAnnualIncome());
        existingScheme.setMaxLandHolding(updatedScheme.getMaxLandHolding());
        existingScheme.setRequiredCategory(updatedScheme.getRequiredCategory());
        existingScheme.setGrantAmount(updatedScheme.getGrantAmount());
        existingScheme.setActive(updatedScheme.getActive());

        return schemeRepository.save(existingScheme);
    }

    @Override
    public void deleteScheme(Long id) {

        Scheme existingScheme = schemeRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Scheme not found with id: " + id));

        existingScheme.setActive(false);

        schemeRepository.save(existingScheme);
    }
}