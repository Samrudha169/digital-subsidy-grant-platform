package com.dsgp.authentication.service;

import com.dsgp.authentication.dto.LoginRequest;
import com.dsgp.authentication.dto.LoginResponse;
import com.dsgp.authentication.dto.OfficerLoginRequest;
import com.dsgp.authentication.dto.OfficerLoginResponse;
import com.dsgp.authentication.entity.Officer;
import com.dsgp.authentication.entity.OfficerRole;
import com.dsgp.authentication.repository.OfficerRepository;
import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.repository.BeneficiaryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final OfficerRepository officerRepository;
    private final PasswordEncoder passwordEncoder;
    private final OfficerOtpService officerOtpService;

    public AuthServiceImpl(
            BeneficiaryRepository beneficiaryRepository,
            OfficerRepository officerRepository,
            PasswordEncoder passwordEncoder,
            OfficerOtpService officerOtpService) {

        this.beneficiaryRepository = beneficiaryRepository;
        this.officerRepository     = officerRepository;
        this.passwordEncoder       = passwordEncoder;
        this.officerOtpService     = officerOtpService;
    }

    // ── Beneficiary login (unchanged) ─────────────────────────────────────────

    @Override
    public LoginResponse login(LoginRequest request) {

        Beneficiary beneficiary = beneficiaryRepository
                .findFirstByEmail(request.getEmail())
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

        // ── Email-verification guard for newly registered accounts ────────────
        //
        // Accounts registered before this feature was introduced will have
        //   emailVerified = false, otpCode = null  → allow login (legacy)
        //
        // Accounts registered after this feature was introduced and not yet
        // verified will have:
        //   emailVerified = false, otpCode != null → block login
        //
        if (!beneficiary.isEmailVerified() && beneficiary.getOtpCode() != null) {
            return new LoginResponse(
                    false,
                    "Please verify your email before logging in. "
                            + "Check your inbox for the verification code.",
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

    /**
     * {@inheritDoc}
     *
     * <p>Two-step flow for non-ADMIN officers:
     * <ol>
     *   <li>Validate username + password.</li>
     *   <li>If credentials are valid and role is {@code FIELD_OFFICER},
     *       {@code DISTRICT_OFFICER}, or {@code FINANCE_APPROVER}: issue an OTP
     *       to the officer's registered email and return
     *       {@code otpRequired = true}.  No session data should be stored yet.</li>
     *   <li>ADMIN accounts bypass OTP and receive the full session response
     *       immediately.</li>
     * </ol>
     */
    @Override
    public OfficerLoginResponse officerLogin(OfficerLoginRequest request) {

        Officer officer = officerRepository
                .findByUsername(request.getUsername())
                .orElse(null);

        if (officer == null || !officer.isActive()) {
            return failureResponse("Invalid username or password");
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                officer.getPassword())) {

            return failureResponse("Invalid username or password");
        }

        // ── ADMIN: bypass OTP, grant session immediately ──────────────────────
        if (officer.getRole() == OfficerRole.ADMIN) {
            return new OfficerLoginResponse(
                    true,
                    "Login successful",
                    officer.getId(),
                    officer.getUsername(),
                    officer.getFullName(),
                    officer.getRole(),
                    officer.getDistrict(),
                    false               // otpRequired = false for ADMIN
            );
        }

        // ── Non-ADMIN: issue OTP and require second factor ────────────────────
        try {
            officerOtpService.sendOtp(officer.getId());
        } catch (IllegalStateException e) {
            // Officer has no registered email — cannot issue OTP
            log.warn(
                    "OTP send failed for officer id={} username={}: {}",
                    officer.getId(),
                    officer.getUsername(),
                    e.getMessage()
            );
            return failureResponse(
                    "Your account does not have a registered email address. "
                    + "Please contact your administrator."
            );
        }

        // Return officer identity so the frontend can route the OTP step,
        // but do NOT store session data yet — the second factor must succeed first.
        return new OfficerLoginResponse(
                true,
                "Password verified. Please enter the OTP sent to your registered email.",
                officer.getId(),
                officer.getUsername(),
                officer.getFullName(),
                officer.getRole(),
                officer.getDistrict(),
                true                    // otpRequired = true
        );
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Builds a standardised failure response with all identity fields null. */
    private OfficerLoginResponse failureResponse(String message) {
        return new OfficerLoginResponse(
                false,
                message,
                null, null, null, null, null,
                false
        );
    }
}