package com.dsgp.authentication.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Request body for {@code POST /auth/officer-resend-otp}.
 *
 * <p>Allows an officer who did not receive or whose OTP expired to request
 * a fresh code.  The previous OTP is always invalidated before the new one
 * is dispatched.
 */
@Data
public class OfficerResendOtpRequest {

    @NotNull(message = "Officer ID is required")
    @Positive(message = "Officer ID must be a positive number")
    private Long officerId;
}
