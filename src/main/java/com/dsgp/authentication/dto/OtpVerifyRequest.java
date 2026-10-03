package com.dsgp.authentication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request body for {@code POST /auth/verify-email}.
 *
 * <p>The registrant submits their email address together with the
 * 6-digit OTP that was sent to that address immediately after
 * successful beneficiary registration.
 */
@Data
public class OtpVerifyRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    private String email;

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "\\d{6}", message = "OTP must be exactly 6 digits")
    private String otp;
}
