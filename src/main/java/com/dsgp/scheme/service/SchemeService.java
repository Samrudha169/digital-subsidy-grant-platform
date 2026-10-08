package com.dsgp.scheme.service;

import com.dsgp.scheme.dto.SchemeRequest;
import com.dsgp.scheme.dto.SchemeResponse;

import java.util.List;

/**
 * Service interface for government scheme management.
 *
 * <p>All operations use {@link SchemeRequest} / {@link SchemeResponse} DTOs
 * so that the raw {@link com.dsgp.beneficiary.entity.Scheme} entity is never
 * exposed outside the service boundary.</p>
 */
public interface SchemeService {

    /**
     * Creates a new scheme from the request DTO.
     */
    SchemeResponse createScheme(SchemeRequest request);

    /**
     * Returns all active schemes.
     */
    List<SchemeResponse> getAllActiveSchemes();

    /**
     * Returns all schemes, including inactive schemes.
     *
     * <p>Used by the Admin side for scheme management.</p>
     */
    List<SchemeResponse> getAllSchemes();

    /**
     * Returns the scheme with the given ID.
     */
    SchemeResponse getSchemeById(Long id);

    /**
     * Updates an existing scheme.
     */
    SchemeResponse updateScheme(Long id, SchemeRequest request);

    /**
     * Soft-deactivates a scheme.
     */
    void deactivateScheme(Long id);

    /**
     * Reactivates a previously deactivated scheme.
     */
    SchemeResponse reactivateScheme(Long id);
}