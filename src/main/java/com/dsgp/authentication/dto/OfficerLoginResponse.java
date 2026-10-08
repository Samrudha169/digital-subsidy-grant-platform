package com.dsgp.authentication.dto;

import com.dsgp.authentication.entity.OfficerRole;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response returned after the first step of officer login (username + password).
 *
 * <p>For {@code FIELD_OFFICER}, {@code DISTRICT_OFFICER}, and
 * {@code FINANCE_APPROVER} roles, {@code otpRequired} is set to {@code true}
 * and no session data should be stored until the OTP step is completed.
 *
 * <p>For {@code ADMIN} accounts, {@code otpRequired} is {@code false} and the
 * full session fields are populated — the officer can proceed directly to
 * their dashboard.
 *
 * <p>On failure, only {@code success} and {@code message} are populated;
 * all other fields are {@code null}/{@code false}.
 */
@Data
@AllArgsConstructor
public class OfficerLoginResponse {

    /** {@code true} if username/password authentication succeeded. */
    private boolean success;

    /** Human-readable status message — safe to display in the UI. */
    private String message;

    /**
     * Officer primary key.
     * Present on success for all roles so the OTP step can reference it.
     */
    private Long officerId;

    /** Login username — present on success. */
    private String username;

    /** Display name — present on success. */
    private String fullName;

    /** Officer role — present on success. */
    private OfficerRole role;

    /** District — present on success (null for Finance Approvers). */
    private String district;

    /**
     * {@code true} when the frontend must collect and submit a 6-digit OTP
     * before granting access to the dashboard.
     *
     * <p>Set to {@code true} for {@code FIELD_OFFICER}, {@code DISTRICT_OFFICER},
     * and {@code FINANCE_APPROVER} after successful password verification.
     * {@code false} for {@code ADMIN} (OTP not required).
     */
    private boolean otpRequired;
}
