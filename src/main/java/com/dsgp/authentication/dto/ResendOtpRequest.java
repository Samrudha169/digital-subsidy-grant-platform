package com.dsgp.authentication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for {@code POST /auth/resend-email-otp}.
 *
 * <p>Allows a newly-registered beneficiary to request a fresh OTP
 * if the original one expired or was not received.
 */
@Data
public class ResendOtpRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    private String email;
}
