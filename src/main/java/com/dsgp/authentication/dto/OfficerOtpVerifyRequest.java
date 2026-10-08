package com.dsgp.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Request body for {@code POST /auth/officer-verify-otp}.
 *
 * <p>The officer submits their id (returned by the first-step
 * {@code /auth/officer-login} response) together with the 6-digit OTP
 * sent to their registered email address.
 */
@Data
public class OfficerOtpVerifyRequest {

    @NotNull(message = "Officer ID is required")
    @Positive(message = "Officer ID must be a positive number")
    private Long officerId;

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "\\d{6}", message = "OTP must be exactly 6 digits")
    private String otp;
}
