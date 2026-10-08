package com.dsgp.authentication.service;

import com.dsgp.authentication.dto.OtpVerifyResponse;

/**
 * Contract for the email OTP service used during officer login.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Generate a secure 6-digit OTP for the officer login flow</li>
 *   <li>Persist the OTP and its expiry on the officer record</li>
 *   <li>Send the OTP to the officer's registered email address</li>
 *   <li>Verify a submitted OTP against the stored value (single-use)</li>
 *   <li>Clear OTP data after successful verification</li>
 *   <li>Invalidate any previous OTP when a new one is issued (resend)</li>
 * </ul>
 *
 * <p>The raw OTP value is <strong>never</strong> logged or returned in any
 * API response.  No JWT or session is issued before OTP verification succeeds.
 *
 * <p>Applicable roles: {@code FIELD_OFFICER}, {@code DISTRICT_OFFICER},
 * {@code FINANCE_APPROVER}.  {@code ADMIN} logins bypass OTP entirely.
 */
public interface OfficerOtpService {

    /**
     * Generates a fresh 6-digit OTP, persists it on the officer record,
     * and dispatches it via SMTP to the officer's registered email address.
     *
     * <p>Any previously-issued OTP for this officer is overwritten (invalidated).
     *
     * @param officerId the officer's primary key
     * @throws IllegalArgumentException if no officer exists for the id
     * @throws IllegalStateException    if the officer has no email address on record
     * @throws org.springframework.mail.MailException if the email cannot be sent
     */
    void sendOtp(Long officerId);

    /**
     * Verifies the submitted OTP for the given officer.
     *
     * <p>On success: clears {@code otpCode} and {@code otpExpiresAt} on the officer
     * record (single-use guarantee).
     *
     * @param officerId the officer's primary key
     * @param otp       the 6-digit code submitted by the officer
     * @return a response indicating success or the reason for failure
     */
    OtpVerifyResponse verifyOtp(Long officerId, String otp);

    /**
     * Resends a fresh OTP to the officer's registered email address,
     * subject to a configurable cooldown period (default 60 s).
     *
     * <p>If the cooldown window has not yet elapsed since the last OTP was
     * issued, no new OTP is generated and no email is sent.  The response
     * includes the exact number of seconds remaining in the cooldown.
     *
     * <p>On success: the previous OTP is always invalidated before the new
     * one is stored — there is never more than one valid OTP at a time.
     *
     * @param officerId the officer's primary key
     * @return a response indicating success or the reason for rejection
     *         (including remaining cooldown seconds on failure)
     */
    OtpVerifyResponse resendOtp(Long officerId);
}
