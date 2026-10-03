package com.dsgp.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response body for {@code POST /auth/verify-email} and
 * {@code POST /auth/resend-email-otp}.
 */
@Data
@AllArgsConstructor
public class OtpVerifyResponse {

    /** {@code true} if the operation succeeded; {@code false} otherwise. */
    private boolean success;

    /** Human-readable status message — safe to display in the UI. */
    private String message;
}
