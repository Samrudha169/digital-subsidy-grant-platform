package com.dsgp.authentication.service;

import com.dsgp.authentication.dto.LoginRequest;
import com.dsgp.authentication.dto.LoginResponse;
import com.dsgp.authentication.dto.OfficerLoginRequest;
import com.dsgp.authentication.dto.OfficerLoginResponse;
import com.dsgp.authentication.entity.Officer;
import com.dsgp.authentication.repository.OfficerRepository;
import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.repository.BeneficiaryRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final OfficerRepository officerRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(
            BeneficiaryRepository beneficiaryRepository,
            OfficerRepository officerRepository,
            PasswordEncoder passwordEncoder) {

        this.beneficiaryRepository = beneficiaryRepository;
        this.officerRepository     = officerRepository;
        this.passwordEncoder       = passwordEncoder;
    }

    // ── Beneficiary login (unchanged) ─────────────────────────────────────────

    @Override
    public LoginResponse login(LoginRequest request) {

        Beneficiary beneficiary = beneficiaryRepository
                .findByEmail(request.getEmail())
                .orElse(null);

        if (beneficiary == null) {
            return new LoginResponse(
                    false,
                    "Invalid email or password",
                    null,
                    null
            );
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                beneficiary.getPassword())) {

            return new LoginResponse(
                    false,
                    "Invalid email or password",
                    null,
                    null
            );
        }

        return new LoginResponse(
                true,
                "Login successful",
                beneficiary.getId(),
                beneficiary.getFullName()
        );
    }

    // ── Officer login ─────────────────────────────────────────────────────────

    @Override
    public OfficerLoginResponse officerLogin(OfficerLoginRequest request) {

        Officer officer = officerRepository
                .findByUsername(request.getUsername())
                .orElse(null);

        if (officer == null || !officer.isActive()) {
            return new OfficerLoginResponse(
                    false,
                    "Invalid username or password",
                    null, null, null, null, null
            );
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                officer.getPassword())) {

            return new OfficerLoginResponse(
                    false,
                    "Invalid username or password",
                    null, null, null, null, null
            );
        }

        return new OfficerLoginResponse(
                true,
                "Login successful",
                officer.getId(),
                officer.getUsername(),
                officer.getFullName(),
                officer.getRole(),
                officer.getDistrict()
        );
    }
}