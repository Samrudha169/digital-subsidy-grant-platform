package com.dsgp.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for officer login.
 *
 * <p>Officers authenticate with their assigned {@code username} and
 * {@code password}, unlike beneficiaries who use email.
 */
@Data
public class OfficerLoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
