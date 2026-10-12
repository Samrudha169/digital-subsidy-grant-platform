package com.dsgp.authentication.service;

import com.dsgp.authentication.dto.LoginRequest;
import com.dsgp.authentication.dto.LoginResponse;
import com.dsgp.authentication.dto.OfficerLoginRequest;
import com.dsgp.authentication.dto.OfficerLoginResponse;
import com.dsgp.authentication.entity.Officer;
import com.dsgp.authentication.repository.OfficerRepository;
import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.repository.BeneficiaryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
        this.officerRepository = officerRepository;
        this.passwordEncoder = passwordEncoder;
        this.officerOtpService = officerOtpService;
        this.emailOtpService = emailOtpService;
    }

    // ── Beneficiary login ─────────────────────────────────────────────────────

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
        // New accounts that have not verified their email have:
        // emailVerified = false AND otpCode != null
        //
        // These accounts must verify their email before logging in.
        if (!beneficiary.isEmailVerified()
                && beneficiary.getOtpCode() != null) {

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

        // ── Legacy accounts ───────────────────────────────────────────────────
        //
        // Preserve the existing backward-compatible behavior.
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

        // ── 24-hour OTP trust window ──────────────────────────────────────────
        //
        // If the beneficiary successfully verified a login OTP within
        // the last 24 hours, another OTP is NOT required.
        if (isOtpTrusted(beneficiary.getLastOtpVerifiedAt())) {

            log.info(
                    "Beneficiary login allowed without OTP within 24-hour trust window: {}",
                    maskEmail(beneficiary.getEmail())
            );

            return new LoginResponse(
                    true,
                    "Login successful",
                    beneficiary.getId(),
                    beneficiary.getFullName(),
                    false,
                    null
            );
        }

        // ── Trust period expired → issue fresh login OTP ──────────────────────
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
                true,
                beneficiary.getEmail()
        );
    }

    // ── Officer login ─────────────────────────────────────────────────────────

    /**
     * Officer login flow:
     *
     * 1. Validate email + password.
     * 2. If the officer successfully verified an OTP within the
     *    previous 24 hours, allow login directly.
     * 3. Otherwise issue an OTP to the registered email.
     * 4. Dashboard access is completed only after OTP verification.
     */
    @Override
    public OfficerLoginResponse officerLogin(OfficerLoginRequest request) {

        Officer officer = officerRepository
                .findByEmail(request.getEmail())
                .orElse(null);

        if (officer == null || !officer.isActive()) {
            return failureResponse("Invalid email or password");
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                officer.getPassword())) {

            return failureResponse("Invalid email or password");
        }

        // ── 24-hour OTP trust window ──────────────────────────────────────────
        //
        // Applies to ALL officer roles:
        // ADMIN
        // FIELD_OFFICER
        // DISTRICT_OFFICER
        // FINANCE_APPROVER
        //
        // No role bypasses OTP on the first login.
        // After successful OTP verification, the officer is trusted
        // for the next 24 hours.
        if (isOtpTrusted(officer.getLastOtpVerifiedAt())) {

            log.info(
                    "Officer login allowed without OTP within 24-hour trust window: "
                            + "officerId={} email={} role={}",
                    officer.getId(),
                    maskEmail(officer.getEmail()),
                    officer.getRole()
            );

            return new OfficerLoginResponse(
                    true,
                    "Login successful",
                    officer.getId(),
                    officer.getUsername(),
                    officer.getFullName(),
                    officer.getRole(),
                    officer.getDistrict(),
                    false
            );
        }

        // ── Trust period expired → issue fresh OTP ────────────────────────────
        try {

            officerOtpService.sendOtp(officer.getId());

        } catch (IllegalStateException e) {

            log.warn(
                    "OTP send failed for officer id={} email={}: {}",
                    officer.getId(),
                    maskEmail(officer.getEmail()),
                    e.getMessage()
            );

            return failureResponse(
                    "Your account does not have a registered email address. "
                            + "Please contact your administrator."
            );
        }

        // Password is correct, but OTP verification is still required.
        return new OfficerLoginResponse(
                true,
                "Password verified. Please enter the OTP sent to your registered email.",
                officer.getId(),
                officer.getUsername(),
                officer.getFullName(),
                officer.getRole(),
                officer.getDistrict(),
                true
        );
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Returns true when the last successful login OTP verification
     * happened within the previous 24 hours.
     */
    private boolean isOtpTrusted(LocalDateTime lastOtpVerifiedAt) {

        if (lastOtpVerifiedAt == null) {
            return false;
        }

        return lastOtpVerifiedAt.isAfter(
                LocalDateTime.now().minusHours(24)
        );
    }

    /**
     * Builds a standardised failure response with all identity fields null.
     */
    private OfficerLoginResponse failureResponse(String message) {

        return new OfficerLoginResponse(
                false,
                message,
                null,
                null,
                null,
                null,
                null,
                false
        );
    }

    /**
     * Returns a masked email address for safe log output.
     * Example: us***@gmail.com
     */
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