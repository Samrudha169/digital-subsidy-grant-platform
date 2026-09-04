package com.dsgp.scheme.service;

import com.dsgp.beneficiary.entity.Scheme;

import java.util.List;

public interface SchemeService {

    List<Scheme> getAllSchemes();

    List<Scheme> getActiveSchemes();

    Scheme getSchemeById(Long schemeId);
}