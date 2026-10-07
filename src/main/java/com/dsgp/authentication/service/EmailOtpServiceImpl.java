package com.dsgp.authentication.service;

import com.dsgp.authentication.dto.OtpVerifyResponse;
import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.repository.BeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Default implementation of {@link EmailOtpService}.
 *
 * <p>OTP lifecycle:
 * <ol>
 *   <li>Generate a 6-digit code using {@link SecureRandom}.</li>
 *   <li>Write {@code otpCode} + {@code otpExpiresAt} to the {@code beneficiary} row.</li>
 *   <li>Dispatch the code via SMTP using {@link JavaMailSender}.</li>
 *   <li>On verification, confirm the code matches and has not expired.</li>
 *   <li>Set {@code emailVerified = true}; null out {@code otpCode} and
 *       {@code otpExpiresAt}.</li>
 * </ol>
 *
 * <p>The raw OTP value is <strong>never</strong> written to production-style
 * log output.  A masked version (e.g. "******") is used in debug messages.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailOtpServiceImpl implements EmailOtpService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final JavaMailSender mailSender;

    /** Sender address for OTP emails -- read from {@code app.otp.from-address}. */
    @Value("${app.otp.from-address:no-reply@dsgp.gov.in}")
    private String fromAddress;

    /** OTP lifetime in minutes -- read from {@code app.otp.expiry-minutes}. */
    @Value("${app.otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    /**
     * Minimum elapsed seconds before a resend is permitted.
     * Read from {@code app.otp.resend-cooldown-seconds}.
     */
    @Value("${app.otp.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // -------------------------------------------------------------------------
    // PUBLIC API
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Finds the beneficiary by email, generates and stores a new OTP,
     * then sends it.  If the SMTP send fails the OTP is still persisted so
     * the user can request a resend.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendOtp(String email) {

        Beneficiary beneficiary = findBeneficiaryOrThrow(email);

        String otp = generateOtp();

        beneficiary.setOtpCode(otp);
        beneficiary.setOtpExpiresAt(
                LocalDateTime.now().plusMinutes(otpExpiryMinutes)
        );

        beneficiaryRepository.save(beneficiary);

        // Dispatch email -- exception propagates to caller (logged there)
        dispatchOtpEmail(email, otp, beneficiary.getFullName());

        log.info(
                "OTP issued for beneficiary email {} (expires in {} minutes)",
                maskEmail(email),
                otpExpiryMinutes
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Verification logic:
     * <ul>
     *   <li>No OTP on record -- already verified or OTP was never issued.</li>
     *   <li>OTP expired -- reject with expiry message.</li>
     *   <li>OTP mismatch -- reject with invalid-code message.</li>
     *   <li>OTP matches and not expired -- mark verified, clear OTP fields.</li>
     * </ul>
     */
    @Override
    @Transactional
    public OtpVerifyResponse verifyOtp(String email, String otp) {

        Beneficiary beneficiary = beneficiaryRepository
                .findFirstByEmail(email)
                .orElse(null);

        if (beneficiary == null) {
            return new OtpVerifyResponse(false, "No account found for this email address.");
        }

        if (beneficiary.isEmailVerified()) {
            return new OtpVerifyResponse(true, "Email is already verified.");
        }

        if (beneficiary.getOtpCode() == null) {
            return new OtpVerifyResponse(
                    false,
                    "No verification code found. Please request a new OTP."
            );
        }

        if (LocalDateTime.now().isAfter(beneficiary.getOtpExpiresAt())) {
            log.warn(
                    "Expired OTP submitted for email {}",
                    maskEmail(email)
            );
            return new OtpVerifyResponse(
                    false,
                    "The verification code has expired. Please request a new one."
            );
        }

        if (!beneficiary.getOtpCode().equals(otp)) {
            log.warn(
                    "Incorrect OTP submitted for email {}",
                    maskEmail(email)
            );
            return new OtpVerifyResponse(false, "Invalid verification code. Please try again.");
        }

        // -- Success --------------------------------------------------------------
        beneficiary.setEmailVerified(true);
        beneficiary.setOtpCode(null);
        beneficiary.setOtpExpiresAt(null);
        beneficiaryRepository.save(beneficiary);

        log.info(
                "Email verified successfully for beneficiary email {}",
                maskEmail(email)
        );

        return new OtpVerifyResponse(true, "Email verified successfully! You can now log in.");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resend is blocked if:
     * <ul>
     *   <li>Email is already verified.</li>
     *   <li>No beneficiary account exists for the email.</li>
     *   <li>The last OTP was issued within the cooldown window.</li>
     * </ul>
     */
    @Override
    @Transactional
    public OtpVerifyResponse resendOtp(String email) {

        Beneficiary beneficiary = beneficiaryRepository
                .findFirstByEmail(email)
                .orElse(null);

        if (beneficiary == null) {
            return new OtpVerifyResponse(false, "No account found for this email address.");
        }

        if (beneficiary.isEmailVerified()) {
            return new OtpVerifyResponse(true, "Email is already verified.");
        }

        // -- Cooldown check -------------------------------------------------------
        if (beneficiary.getOtpExpiresAt() != null) {

            LocalDateTime earliestResend = beneficiary
                    .getOtpExpiresAt()
                    .minusMinutes(otpExpiryMinutes)
                    .plusSeconds(resendCooldownSeconds);

            if (LocalDateTime.now().isBefore(earliestResend)) {
                log.warn(
                        "Resend OTP blocked -- cooldown active for email {}",
                        maskEmail(email)
                );
                return new OtpVerifyResponse(
                        false,
                        "Please wait " + resendCooldownSeconds
                                + " seconds before requesting another code."
                );
            }
        }

        // -- Issue and send fresh OTP ---------------------------------------------
        String otp = generateOtp();

        beneficiary.setOtpCode(otp);
        beneficiary.setOtpExpiresAt(
                LocalDateTime.now().plusMinutes(otpExpiryMinutes)
        );

        beneficiaryRepository.save(beneficiary);

        try {
            dispatchOtpEmail(email, otp, beneficiary.getFullName());
        } catch (MailException e) {
            log.error(
                    "Failed to resend OTP email to {}: {}",
                    maskEmail(email),
                    e.getMessage()
            );
            return new OtpVerifyResponse(
                    false,
                    "Could not send the verification email. Please try again shortly."
            );
        }

        log.info(
                "OTP resent for email {} (expires in {} minutes)",
                maskEmail(email),
                otpExpiryMinutes
        );

        return new OtpVerifyResponse(
                true,
                "A new verification code has been sent to your email."
        );
    }

    // -------------------------------------------------------------------------
    // PRIVATE HELPERS
    // -------------------------------------------------------------------------

    /**
     * Generates a cryptographically random 6-digit OTP (000000-999999).
     * Uses {@link SecureRandom} -- not {@code Math.random()}.
     */
    private String generateOtp() {
        int code = SECURE_RANDOM.nextInt(1_000_000);   // 0 - 999999
        return String.format("%06d", code);             // zero-pad to 6 digits
    }

    /** Sends the OTP email using the configured {@link JavaMailSender}. */
    private void dispatchOtpEmail(String toEmail, String otp, String fullName) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("DSGP - Your Email Verification Code");
        message.setText(buildEmailBody(fullName, otp));

        mailSender.send(message);
    }

    /** Constructs the plain-text OTP email body. */
    private String buildEmailBody(String fullName, String otp) {
        return String.format(
                "Dear %s,%n%n" +
                "Thank you for registering on the Digital Subsidy & Grant Platform (DSGP).%n%n" +
                "Your email verification code is:%n%n" +
                "    %s%n%n" +
                "This code is valid for %d minutes.%n%n" +
                "If you did not register on DSGP, please ignore this email.%n%n" +
                "-- DSGP Support Team",
                fullName, otp, otpExpiryMinutes);
    }

    /** Finds the beneficiary by email or throws if not found. */
    private Beneficiary findBeneficiaryOrThrow(String email) {
        return beneficiaryRepository
                .findFirstByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No beneficiary account found for email: " + maskEmail(email)
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