package com.dsgp.authentication.service;

import com.dsgp.authentication.dto.AdminOfficerRequest;
import com.dsgp.authentication.dto.AdminOfficerResponse;
import com.dsgp.authentication.entity.Officer;
import com.dsgp.authentication.entity.OfficerRole;
import com.dsgp.authentication.repository.OfficerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminOfficerServiceImpl implements AdminOfficerService {

    private final OfficerRepository officerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<AdminOfficerResponse> getAllOfficers() {

        return officerRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOfficerResponse getOfficerById(Long id) {

        Officer officer = officerRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Officer not found with ID: " + id
                        )
                );

        return toResponse(officer);
    }

    @Override
    public AdminOfficerResponse createOfficer(
            AdminOfficerRequest request
    ) {

        validateRequest(request);

        if (officerRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException(
                    "Username is already taken."
            );
        }

        if (request.getPassword() == null ||
                request.getPassword().isBlank()) {

            throw new IllegalArgumentException(
                    "Password is required when creating an officer."
            );
        }

        Officer officer = Officer.builder()
                .username(request.getUsername().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .email(normalize(request.getEmail()))
                .role(request.getRole())
                .district(normalize(request.getDistrict()))
                .active(true)
                .build();

        Officer savedOfficer = officerRepository.save(officer);

        return toResponse(savedOfficer);
    }

    @Override
    public AdminOfficerResponse updateOfficer(
            Long id,
            AdminOfficerRequest request
    ) {

        validateRequest(request);

        Officer officer = officerRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Officer not found with ID: " + id
                        )
                );

        if (officerRepository.existsByUsernameAndIdNot(
                request.getUsername(),
                id
        )) {
            throw new IllegalArgumentException(
                    "Username is already taken."
            );
        }

        officer.setUsername(request.getUsername().trim());
        officer.setFullName(request.getFullName().trim());
        officer.setEmail(normalize(request.getEmail()));
        officer.setRole(request.getRole());
        officer.setDistrict(normalize(request.getDistrict()));

        /*
         * Password is optional during update.
         * If Admin leaves it empty, the existing password remains unchanged.
         */
        if (request.getPassword() != null &&
                !request.getPassword().isBlank()) {

            officer.setPassword(
                    passwordEncoder.encode(request.getPassword())
            );
        }

        Officer savedOfficer = officerRepository.save(officer);

        return toResponse(savedOfficer);
    }

    @Override
    public AdminOfficerResponse deactivateOfficer(Long id) {

        Officer officer = officerRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Officer not found with ID: " + id
                        )
                );

        officer.setActive(false);

        Officer savedOfficer = officerRepository.save(officer);

        return toResponse(savedOfficer);
    }

    @Override
    public AdminOfficerResponse reactivateOfficer(Long id) {

        Officer officer = officerRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Officer not found with ID: " + id
                        )
                );

        officer.setActive(true);

        Officer savedOfficer = officerRepository.save(officer);

        return toResponse(savedOfficer);
    }

    private void validateRequest(AdminOfficerRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Officer information is required."
            );
        }

        if (request.getUsername() == null ||
                request.getUsername().isBlank()) {

            throw new IllegalArgumentException(
                    "Username is required."
            );
        }

        if (request.getFullName() == null ||
                request.getFullName().isBlank()) {

            throw new IllegalArgumentException(
                    "Full name is required."
            );
        }

        if (request.getRole() == null) {
            throw new IllegalArgumentException(
                    "Officer role is required."
            );
        }

        /*
         * Email is mandatory for OTP-based roles.
         * FIELD_OFFICER, DISTRICT_OFFICER, and FINANCE_APPROVER
         * authenticate via email OTP, so an email address must
         * always be provided when creating or updating an officer
         * with one of these roles.
         *
         * ADMIN uses a different login path and is excluded.
         */
        if (request.getRole() != OfficerRole.ADMIN &&
                (request.getEmail() == null ||
                        request.getEmail().isBlank())) {

            throw new IllegalArgumentException(
                    "Email is required for officer OTP login."
            );
        }
    }

    private String normalize(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private AdminOfficerResponse toResponse(Officer officer) {

        return new AdminOfficerResponse(
                officer.getId(),
                officer.getUsername(),
                officer.getFullName(),
                officer.getEmail(),
                officer.getRole(),
                officer.getDistrict(),
                officer.isActive()
        );
    }
}