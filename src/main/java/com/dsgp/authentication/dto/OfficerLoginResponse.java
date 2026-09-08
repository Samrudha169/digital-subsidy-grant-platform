package com.dsgp.authentication.dto;

import com.dsgp.authentication.entity.OfficerRole;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response returned after a successful officer login.
 *
 * <p>Contains the officer's identity and role so the frontend can render
 * the correct dashboard and allow only the appropriate actions.
 */
@Data
@AllArgsConstructor
public class OfficerLoginResponse {

    private boolean success;
    private String message;
    private Long officerId;
    private String username;
    private String fullName;
    private OfficerRole role;
    private String district;
}
