package com.dsgp.scheme.service;

import com.dsgp.beneficiary.entity.Scheme;

import java.util.List;

public interface SchemeService {

    Scheme createScheme(Scheme scheme);

    Scheme getSchemeById(Long id);

    List<Scheme> getAllSchemes();

    List<Scheme> getActiveSchemes();

    Scheme updateScheme(Long id, Scheme scheme);

    void deleteScheme(Long id);
}