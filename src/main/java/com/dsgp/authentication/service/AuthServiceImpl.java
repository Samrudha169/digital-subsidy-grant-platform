package com.dsgp.authentication.service;

import com.dsgp.authentication.dto.LoginRequest;
import com.dsgp.authentication.dto.LoginResponse;
import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.repository.BeneficiaryRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(
            BeneficiaryRepository beneficiaryRepository,
            PasswordEncoder passwordEncoder) {

        this.beneficiaryRepository = beneficiaryRepository;
        this.passwordEncoder = passwordEncoder;
    }

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
}