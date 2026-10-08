package com.dsgp.authentication.controller;

import com.dsgp.authentication.dto.LoginRequest;
import com.dsgp.authentication.dto.LoginResponse;
import com.dsgp.authentication.dto.OfficerLoginRequest;
import com.dsgp.authentication.dto.OfficerLoginResponse;
import com.dsgp.authentication.dto.OfficerOtpVerifyRequest;
import com.dsgp.authentication.dto.OfficerResendOtpRequest;
import com.dsgp.authentication.dto.OtpVerifyRequest;
import com.dsgp.authentication.dto.OtpVerifyResponse;
import com.dsgp.authentication.dto.ResendOtpRequest;
import com.dsgp.authentication.service.AuthService;
import com.dsgp.authentication.service.EmailOtpService;
import com.dsgp.authentication.service.OfficerOtpService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller for both beneficiaries and government officers.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /auth/login}                — beneficiary login</li>
 *   <li>{@code POST /auth/officer-login}         — officer step 1: username + password</li>
 *   <li>{@code POST /auth/officer-verify-otp}    — officer step 2: OTP verification</li>
 *   <li>{@code POST /auth/officer-resend-otp}    — officer OTP resend</li>
 *   <li>{@code POST /auth/verify-email}          — beneficiary email OTP verification</li>
 *   <li>{@code POST /auth/resend-email-otp}      — beneficiary OTP resend</li>
 * </ul>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailOtpService emailOtpService;
    private final OfficerOtpService officerOtpService;

    public AuthController(AuthService authService,
                          EmailOtpService emailOtpService,
                          OfficerOtpService officerOtpService) {
        this.authService       = authService;
        this.emailOtpService   = emailOtpService;
        this.officerOtpService = officerOtpService;
    }

    // ── Beneficiary login (unchanged) ─────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);

        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    // ── Officer login — Step 1: username + password ───────────────────────────

    /**
     * Authenticates a government officer by username and password.
     *
     * <p>For {@code FIELD_OFFICER}, {@code DISTRICT_OFFICER}, and
     * {@code FINANCE_APPROVER}: returns {@code otpRequired=true} and sends a
     * 6-digit OTP to the officer's registered email.
     * The client must then call {@code POST /auth/officer-verify-otp}.
     *
     * <p>For {@code ADMIN}: returns full session data immediately (no OTP).
     *
     * @param request officer username + password
     * @return 200 with role and OTP flag on success; 401 on failure
     */
    @PostMapping("/officer-login")
    public ResponseEntity<OfficerLoginResponse> officerLogin(
            @Valid @RequestBody OfficerLoginRequest request) {

        OfficerLoginResponse response = authService.officerLogin(request);

        if (!response.isSuccess()) {
            return ResponseEntity.status(401).body(response);
        }

        return ResponseEntity.ok(response);
    }

    // ── Officer login — Step 2: OTP verification ──────────────────────────────

    /**
     * Verifies the 6-digit OTP submitted by the officer after step 1.
     *
     * <p>On success: the OTP is cleared (single-use) and the response
     * carries {@code success=true}.  The frontend should then store the
     * officer session and navigate to the dashboard.
     *
     * <p>On failure: returns 400 with an error message.  The officer may
     * retry or request a resend.
     *
     * @param request {@code { officerId, otp }}
     * @return 200 on verified; 400 with error message otherwise
     */
    @PostMapping("/officer-verify-otp")
    public ResponseEntity<OtpVerifyResponse> officerVerifyOtp(
            @Valid @RequestBody OfficerOtpVerifyRequest request) {

        OtpVerifyResponse response =
                officerOtpService.verifyOtp(request.getOfficerId(), request.getOtp());

        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    // ── Officer login — OTP resend ────────────────────────────────────────────

    /**
     * Resends a fresh OTP to the officer's registered email address.
     *
     * <p>The previous OTP is always invalidated before the new one is sent.
     *
     * @param request {@code { officerId }}
     * @return 200 on resent; 400 on failure
     */
    @PostMapping("/officer-resend-otp")
    public ResponseEntity<OtpVerifyResponse> officerResendOtp(
            @Valid @RequestBody OfficerResendOtpRequest request) {

        OtpVerifyResponse response =
                officerOtpService.resendOtp(request.getOfficerId());

        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    // ── Email OTP verification (beneficiary registration flow) ────────────────

    /**
     * Verifies the 6-digit OTP submitted by a newly-registered beneficiary.
     *
     * <p>On success: {@code emailVerified} is set to {@code true} and the OTP
     * is cleared from the beneficiary record.
     *
     * @param request {@code { email, otp }}
     * @return 200 with {@code success=true} on verified; 400 with error message otherwise
     */
    @PostMapping("/verify-email")
    public ResponseEntity<OtpVerifyResponse> verifyEmail(
            @Valid @RequestBody OtpVerifyRequest request) {

        OtpVerifyResponse response =
                emailOtpService.verifyOtp(request.getEmail(), request.getOtp());

        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Resends a fresh OTP to the beneficiary's registered email address.
     *
     * <p>Subject to a cooldown period (default 60 s) to prevent abuse.
     *
     * @param request {@code { email }}
     * @return 200 with {@code success=true} if resent; 400 if cooldown active or
     *         account not found
     */
    @PostMapping("/resend-email-otp")
    public ResponseEntity<OtpVerifyResponse> resendEmailOtp(
            @Valid @RequestBody ResendOtpRequest request) {

        OtpVerifyResponse response =
                emailOtpService.resendOtp(request.getEmail());

        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }
}