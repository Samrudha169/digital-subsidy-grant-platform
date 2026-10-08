package com.dsgp.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response body for {@code POST /auth/login}.
 *
 * <p>When {@code otpRequired = true} the password check passed and a login OTP
 * has been dispatched to {@code email}.  In this case {@code beneficiaryId} and
 * {@code name} are {@code null} — the session is granted only after the OTP is
 * successfully verified via {@code POST /auth/verify-login-otp}.
 *
 * <p>When {@code otpRequired = false} (legacy / unverified-blocked path) the
 * response carries the normal success/failure fields.
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    private boolean success;
    private String message;
    private Integer beneficiaryId;
    private String name;

    /**
     * {@code true} when the caller must proceed to the OTP verification step.
     * The frontend should show an OTP input and call
     * {@code POST /auth/verify-login-otp}.
     */
    private boolean otpRequired;

    /**
     * The email address to which the login OTP was sent.
     * Present only when {@code otpRequired = true}.
     */
    private String email;
}