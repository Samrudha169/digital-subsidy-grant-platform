package com.dsgp.authentication.controller;

import com.dsgp.authentication.dto.LoginRequest;
import com.dsgp.authentication.dto.LoginResponse;
import com.dsgp.authentication.dto.OfficerLoginRequest;
import com.dsgp.authentication.dto.OfficerLoginResponse;
import com.dsgp.authentication.dto.OtpVerifyRequest;
import com.dsgp.authentication.dto.OtpVerifyResponse;
import com.dsgp.authentication.dto.ResendOtpRequest;
import com.dsgp.authentication.service.AuthService;
import com.dsgp.authentication.service.EmailOtpService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller for both beneficiaries and government officers.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /auth/login}              — beneficiary login</li>
 *   <li>{@code POST /auth/officer-login}       — officer login (unchanged)</li>
 *   <li>{@code POST /auth/verify-email}        — email OTP verification</li>
 *   <li>{@code POST /auth/resend-email-otp}    — resend OTP</li>
 * </ul>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailOtpService emailOtpService;

    public AuthController(AuthService authService,
                          EmailOtpService emailOtpService) {
        this.authService     = authService;
        this.emailOtpService = emailOtpService;
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

    // ── Officer login (unchanged) ─────────────────────────────────────────────

    /**
     * Authenticates a government officer (Field, District, or Finance).
     *
     * @param request officer username + password
     * @return 200 with role and district on success; 401 on failure
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