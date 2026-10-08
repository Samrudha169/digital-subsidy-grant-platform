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
    private final EmailOtpService emailOtpService;

    public AuthServiceImpl(
            BeneficiaryRepository beneficiaryRepository,
            OfficerRepository officerRepository,
            PasswordEncoder passwordEncoder,
            OfficerOtpService officerOtpService,
            EmailOtpService emailOtpService) {

        this.beneficiaryRepository = beneficiaryRepository;
        this.officerRepository     = officerRepository;
        this.passwordEncoder       = passwordEncoder;
        this.officerOtpService     = officerOtpService;
        this.emailOtpService       = emailOtpService;
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
                    null,
                    false,
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
                    null,
                    false,
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
                    null,
                    false,
                    null
            );
        }

        // ── Legacy accounts (emailVerified=false, otpCode=null) ───────────────
        // Allowed through without OTP to preserve backward compatibility.
        if (!beneficiary.isEmailVerified()) {
            return new LoginResponse(
                    true,
                    "Login successful",
                    beneficiary.getId(),
                    beneficiary.getFullName(),
                    false,
                    null
            );
        }

        // ── Verified accounts: issue a login OTP (two-step) ──────────────────
        try {
            emailOtpService.sendLoginOtp(beneficiary.getEmail());
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn(
                    "Login OTP send failed for email {}: {}",
                    maskEmail(request.getEmail()),
                    e.getMessage()
            );
            return new LoginResponse(
                    false,
                    "Could not send the login verification code. Please try again.",
                    null,
                    null,
                    false,
                    null
            );
        }

        return new LoginResponse(
                true,
                "Password verified. Please enter the code sent to your email.",
                null,
                null,
                true,                       // otpRequired
                beneficiary.getEmail()      // so the frontend knows where to send verify
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

    /** Returns a masked email address for safe log output (e.g. {@code us***@gmail.com}). */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String masked = local.length() <= 2
                ? local.charAt(0) + "***"
                : local.substring(0, 2) + "***";
        return masked + "@" + parts[1];
    }
}