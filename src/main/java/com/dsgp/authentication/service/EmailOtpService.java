package com.dsgp.authentication.service;

import com.dsgp.authentication.dto.OtpVerifyResponse;

/**
 * Contract for the email OTP verification service used during
 * new beneficiary registration.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Generate a secure 6-digit OTP</li>
 *   <li>Persist the OTP and its expiry on the beneficiary record</li>
 *   <li>Send the OTP to the beneficiary's registered email</li>
 *   <li>Verify a submitted OTP against the stored value</li>
 *   <li>Clear OTP data after successful verification</li>
 *   <li>Enforce a resend cooldown to prevent abuse</li>
 * </ul>
 */
public interface EmailOtpService {

    /**
     * Generates a fresh OTP, persists it on the beneficiary record for
     * {@code email}, and dispatches it via SMTP.
     *
     * <p>Called immediately after a new beneficiary is saved.
     *
     * @param email the beneficiary's registered email address
     * @throws IllegalArgumentException if no beneficiary exists for the email
     * @throws org.springframework.mail.MailException if the email cannot be sent
     */
    void sendOtp(String email);

    /**
     * Verifies the submitted OTP for the given email address.
     *
     * <p>On success: sets {@code emailVerified = true} and clears
     * {@code otpCode} and {@code otpExpiresAt} on the beneficiary record.
     *
     * @param email the beneficiary's registered email address
     * @param otp   the 6-digit code submitted by the user
     * @return a response indicating success or the reason for failure
     */
    OtpVerifyResponse verifyOtp(String email, String otp);

    /**
     * Resends a fresh OTP to the given email address, subject to
     * the configured cooldown period.
     *
     * @param email the beneficiary's registered email address
     * @return a response indicating success or the reason for rejection
     */
    OtpVerifyResponse resendOtp(String email);
}
