package com.dsgp.scheme.service;

import com.dsgp.scheme.dto.SchemeRequest;
import com.dsgp.scheme.dto.SchemeResponse;

import java.util.List;

/**
 * Service interface for government scheme management.
 *
 * <p>All operations use {@link SchemeRequest} / {@link SchemeResponse} DTOs
 * so that the raw {@link com.dsgp.beneficiary.entity.Scheme} entity is never
 * exposed outside the service boundary.
 */
public interface SchemeService {

    /**
     * Creates a new scheme from the request DTO.
     *
     * @param request the scheme data
     * @return the created scheme as a response DTO
     */
    SchemeResponse createScheme(SchemeRequest request);

    /**
     * Returns all active schemes.
     *
     * @return list of active scheme responses
     */
    List<SchemeResponse> getAllActiveSchemes();

    /**
     * Returns the scheme with the given ID.
     *
     * @param id the scheme primary key
     * @return the scheme as a response DTO
     * @throws com.dsgp.scheme.exception.SchemeNotFoundException if not found
     */
    SchemeResponse getSchemeById(Long id);

    /**
     * Updates an existing scheme.
     *
     * @param id      the scheme primary key
     * @param request the updated scheme data
     * @return the updated scheme as a response DTO
     * @throws com.dsgp.scheme.exception.SchemeNotFoundException if not found
     */
    SchemeResponse updateScheme(Long id, SchemeRequest request);

    /**
     * Soft-deactivates a scheme (sets {@code active = false}).
     *
     * @param id the scheme primary key
     * @throws com.dsgp.scheme.exception.SchemeNotFoundException if not found
     */
    void deactivateScheme(Long id);
}