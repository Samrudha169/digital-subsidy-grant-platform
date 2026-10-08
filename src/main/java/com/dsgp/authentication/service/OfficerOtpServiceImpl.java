package com.dsgp.authentication.service;

import com.dsgp.authentication.dto.OtpVerifyResponse;
import com.dsgp.authentication.entity.Officer;
import com.dsgp.authentication.repository.OfficerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Default implementation of {@link OfficerOtpService}.
 *
 * <p>OTP lifecycle for officer login:
 * <ol>
 *   <li>After successful username/password authentication, {@code sendOtp(officerId)}
 *       is called — a 6-digit OTP is generated, written to the officer row, and
 *       dispatched via SMTP.</li>
 *   <li>On the next request the officer submits the code.  {@code verifyOtp()}
 *       checks the code and expiry, then clears both fields (single-use).</li>
 *   <li>{@code resendOtp()} always overwrites the previous code before
 *       sending — there is never more than one valid OTP for an officer.</li>
 * </ol>
 *
 * <p>The raw OTP is <strong>never</strong> written to logs or returned in
 * any API response.  Only a masked version of the email is logged.
 *
 * <p>No JWT or session is issued before {@code verifyOtp()} succeeds.
 *
 * <p>OTP expiry defaults to 5 minutes (overridden by
 * {@code app.officer-otp.expiry-minutes}).  This is intentionally shorter
 * than the beneficiary registration OTP (10 min) because the officer is
 * expected to be at their desk when logging in.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfficerOtpServiceImpl implements OfficerOtpService {

    private final OfficerRepository officerRepository;
    private final JavaMailSender mailSender;

    /** Sender address — shared with beneficiary OTP config. */
    @Value("${app.otp.from-address:no-reply@dsgp.gov.in}")
    private String fromAddress;

    /** OTP lifetime in minutes for officer logins (default 5 min). */
    @Value("${app.officer-otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    /**
     * Minimum seconds that must elapse between resend requests.
     * Read from {@code app.officer-otp.resend-cooldown-seconds}.
     * Default 60 s — identical to the beneficiary registration OTP cooldown.
     */
    @Value("${app.officer-otp.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // -------------------------------------------------------------------------
    // PUBLIC API
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Finds the officer by id, generates and stores a new OTP (overwriting
     * any previous one), then sends it.  If the SMTP send fails the OTP is
     * still persisted so the officer can request a resend.
     */
    @Override
    @Transactional
    public void sendOtp(Long officerId) {

        Officer officer = findOfficerOrThrow(officerId);

        if (officer.getEmail() == null || officer.getEmail().isBlank()) {
            throw new IllegalStateException(
                    "Officer account has no registered email address. "
                    + "Contact your administrator to add an email before logging in."
            );
        }

        String otp = generateOtp();

        officer.setOtpCode(otp);
        officer.setOtpExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes));

        officerRepository.save(officer);

        // Dispatch email — MailException propagates to caller (AuthController)
        dispatchOtpEmail(officer.getEmail(), officer.getFullName(), otp);

        log.info(
                "Login OTP issued for officer id={} email={} (expires in {} min)",
                officerId,
                maskEmail(officer.getEmail()),
                otpExpiryMinutes
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Verification logic:
     * <ul>
     *   <li>No OTP on record — not expected during login flow.</li>
     *   <li>OTP expired — reject with expiry message.</li>
     *   <li>OTP mismatch — reject with invalid-code message.</li>
     *   <li>OTP matches and not expired — clear OTP fields (single-use).</li>
     * </ul>
     */
    @Override
    @Transactional
    public OtpVerifyResponse verifyOtp(Long officerId, String otp) {

        Officer officer = officerRepository.findById(officerId).orElse(null);

        if (officer == null || !officer.isActive()) {
            return new OtpVerifyResponse(false, "Officer account not found or inactive.");
        }

        if (officer.getOtpCode() == null) {
            return new OtpVerifyResponse(
                    false,
                    "No verification code found. Please request a new OTP."
            );
        }

        if (LocalDateTime.now().isAfter(officer.getOtpExpiresAt())) {
            log.warn(
                    "Expired OTP submitted for officer id={} email={}",
                    officerId,
                    maskEmail(officer.getEmail())
            );
            return new OtpVerifyResponse(
                    false,
                    "The verification code has expired. Please request a new one."
            );
        }

        if (!officer.getOtpCode().equals(otp)) {
            log.warn(
                    "Incorrect OTP submitted for officer id={} email={}",
                    officerId,
                    maskEmail(officer.getEmail())
            );
            return new OtpVerifyResponse(false, "Invalid verification code. Please try again.");
        }

        // -- Success: clear OTP (single-use guarantee) ------------------------
        officer.setOtpCode(null);
        officer.setOtpExpiresAt(null);
        officerRepository.save(officer);

        log.info(
                "Login OTP verified for officer id={} email={}",
                officerId,
                maskEmail(officer.getEmail())
        );

        return new OtpVerifyResponse(true, "OTP verified. Login successful.");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resend is blocked if the cooldown window (default 60 s) has not yet
     * elapsed since the last OTP was issued.  The issue time is derived from
     * {@code otpExpiresAt - otpExpiryMinutes}.  When no OTP exists yet the
     * cooldown is not applied.
     *
     * <p>On success: a fresh OTP overwrites the previous one (invalidating it)
     * and is dispatched via SMTP.
     */
    @Override
    @Transactional
    public OtpVerifyResponse resendOtp(Long officerId) {

        Officer officer = officerRepository.findById(officerId).orElse(null);

        if (officer == null || !officer.isActive()) {
            return new OtpVerifyResponse(false, "Officer account not found or inactive.");
        }

        if (officer.getEmail() == null || officer.getEmail().isBlank()) {
            return new OtpVerifyResponse(
                    false,
                    "No email address is registered for this account. Contact your administrator."
            );
        }

        // -- Cooldown check ---------------------------------------------------
        // If a previous OTP exists, derive when it was issued and compare to now.
        if (officer.getOtpExpiresAt() != null) {

            LocalDateTime issuedAt = officer.getOtpExpiresAt()
                    .minusMinutes(otpExpiryMinutes);

            LocalDateTime earliestResend = issuedAt.plusSeconds(resendCooldownSeconds);

            if (LocalDateTime.now().isBefore(earliestResend)) {

                long secondsRemaining = ChronoUnit.SECONDS.between(
                        LocalDateTime.now(), earliestResend);

                log.warn(
                        "Resend OTP blocked — cooldown active for officer id={} email={} "
                        + "({} s remaining)",
                        officerId,
                        maskEmail(officer.getEmail()),
                        secondsRemaining
                );

                return new OtpVerifyResponse(
                        false,
                        "Please wait " + secondsRemaining
                                + " second(s) before requesting another code."
                );
            }
        }

        // -- Cooldown passed: issue fresh OTP (invalidates previous) ----------
        String otp = generateOtp();

        officer.setOtpCode(otp);
        officer.setOtpExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes));

        officerRepository.save(officer);

        try {
            dispatchOtpEmail(officer.getEmail(), officer.getFullName(), otp);
        } catch (MailException e) {
            log.error(
                    "Failed to resend login OTP email to officer id={} email={}: {}",
                    officerId,
                    maskEmail(officer.getEmail()),
                    e.getMessage()
            );
            return new OtpVerifyResponse(
                    false,
                    "Could not send the verification email. Please try again shortly."
            );
        }

        log.info(
                "Login OTP resent for officer id={} email={} (expires in {} min)",
                officerId,
                maskEmail(officer.getEmail()),
                otpExpiryMinutes
        );

        return new OtpVerifyResponse(true, "A new verification code has been sent to your email.");
    }

    // -------------------------------------------------------------------------
    // PRIVATE HELPERS
    // -------------------------------------------------------------------------

    /** Generates a cryptographically random 6-digit OTP (000000–999999). */
    private String generateOtp() {
        int code = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    /** Sends the officer login OTP email. The OTP is embedded in the body only — never in logs. */
    private void dispatchOtpEmail(String toEmail, String fullName, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("DSGP — Officer Login Verification Code");
        message.setText(buildEmailBody(fullName, otp));

        mailSender.send(message);
    }

    /** Constructs the plain-text officer OTP email body. */
    private String buildEmailBody(String fullName, String otp) {
        return String.format(
                "Dear %s,%n%n"
                + "A login attempt was made for your DSGP Officer account.%n%n"
                + "Your one-time verification code is:%n%n"
                + "    %s%n%n"
                + "This code is valid for %d minutes and can only be used once.%n%n"
                + "If you did not attempt to log in, please contact your administrator immediately.%n%n"
                + "-- DSGP Security Team",
                fullName, otp, otpExpiryMinutes);
    }

    /** Finds an officer by id or throws if not found. */
    private Officer findOfficerOrThrow(Long officerId) {
        return officerRepository.findById(officerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Officer not found with id: " + officerId
                ));
    }

    /**
     * Returns a masked email address for safe logging (e.g. {@code us***@gmail.com}).
     * The OTP value itself is never logged.
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String masked = local.length() <= 2
                ? local.charAt(0) + "***"
                : local.substring(0, 2) + "***";
        return masked + "@" + parts[1];
    }
}
