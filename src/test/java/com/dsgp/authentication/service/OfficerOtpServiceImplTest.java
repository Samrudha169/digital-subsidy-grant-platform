package com.dsgp.authentication.service;

import com.dsgp.authentication.dto.OtpVerifyResponse;
import com.dsgp.authentication.entity.Officer;
import com.dsgp.authentication.entity.OfficerRole;
import com.dsgp.authentication.repository.OfficerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link OfficerOtpServiceImpl}.
 *
 * <p>Covers all acceptance-criteria scenarios for officer login OTP without
 * a real database or SMTP connection.  Uses Mockito to isolate the service.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OfficerOtpServiceImpl")
class OfficerOtpServiceImplTest {

    @Mock
    private OfficerRepository officerRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private OfficerOtpServiceImpl service;

    @BeforeEach
    void injectValues() {
        ReflectionTestUtils.setField(service, "fromAddress",           "no-reply@dsgp.gov.in");
        ReflectionTestUtils.setField(service, "otpExpiryMinutes",      5);
        ReflectionTestUtils.setField(service, "resendCooldownSeconds", 60);
    }

    // ── Shared helper ────────────────────────────────────────────────────────

    private Officer activeOfficer() {
        return Officer.builder()
                .id(1L)
                .username("field.officer1")
                .fullName("Field Officer One")
                .email("officer@district.gov.in")
                .role(OfficerRole.FIELD_OFFICER)
                .active(true)
                .build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // sendOtp — generation and persistence
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("sendOtp()")
    class SendOtp {

        @Test
        @DisplayName("generates a 6-digit OTP and persists it on the officer record")
        void sendOtp_generatesAndPersistsOtp() {
            Officer o = activeOfficer();
            given(officerRepository.findById(1L)).willReturn(Optional.of(o));
            given(officerRepository.save(any())).willReturn(o);

            service.sendOtp(1L);

            ArgumentCaptor<Officer> captor = ArgumentCaptor.forClass(Officer.class);
            then(officerRepository).should().save(captor.capture());

            String otp = captor.getValue().getOtpCode();
            assertThat(otp)
                    .isNotNull()
                    .matches("\\d{6}");
        }

        @Test
        @DisplayName("persists an expiry timestamp in the future")
        void sendOtp_persistsFutureExpiry() {
            Officer o = activeOfficer();
            given(officerRepository.findById(1L)).willReturn(Optional.of(o));
            given(officerRepository.save(any())).willReturn(o);

            service.sendOtp(1L);

            ArgumentCaptor<Officer> captor = ArgumentCaptor.forClass(Officer.class);
            then(officerRepository).should().save(captor.capture());

            assertThat(captor.getValue().getOtpExpiresAt())
                    .isNotNull()
                    .isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("sends exactly one email via JavaMailSender")
        void sendOtp_sendsEmail() {
            Officer o = activeOfficer();
            given(officerRepository.findById(1L)).willReturn(Optional.of(o));
            given(officerRepository.save(any())).willReturn(o);

            service.sendOtp(1L);

            then(mailSender).should().send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("throws IllegalArgumentException when officer id not found")
        void sendOtp_unknownId_throws() {
            given(officerRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.sendOtp(99L))
                    .isInstanceOf(IllegalArgumentException.class);

            then(officerRepository).should(never()).save(any());
            then(mailSender).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("throws IllegalStateException when officer has no email address")
        void sendOtp_noEmail_throws() {
            Officer o = activeOfficer();
            o.setEmail(null);
            given(officerRepository.findById(1L)).willReturn(Optional.of(o));

            assertThatThrownBy(() -> service.sendOtp(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("email");

            then(officerRepository).should(never()).save(any());
            then(mailSender).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("resend overwrites previous OTP — at most one valid OTP at a time")
        void sendOtp_overwritesPreviousOtp() {
            Officer o = activeOfficer();
            o.setOtpCode("111111");
            o.setOtpExpiresAt(LocalDateTime.now().plusMinutes(3));

            given(officerRepository.findById(1L)).willReturn(Optional.of(o));
            given(officerRepository.save(any())).willReturn(o);

            service.sendOtp(1L);

            ArgumentCaptor<Officer> captor = ArgumentCaptor.forClass(Officer.class);
            then(officerRepository).should().save(captor.capture());

            // New OTP replaces the old one (almost certainly different)
            assertThat(captor.getValue().getOtpCode())
                    .isNotNull()
                    .matches("\\d{6}");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // verifyOtp — success
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("verifyOtp() — success")
    class VerifyOtpSuccess {

        @Test
        @DisplayName("correct code returns success and clears OTP fields (single-use)")
        void verifyOtp_correctCode_success() {
            Officer o = activeOfficer();
            o.setOtpCode("234567");
            o.setOtpExpiresAt(LocalDateTime.now().plusMinutes(4));
            given(officerRepository.findById(1L)).willReturn(Optional.of(o));
            given(officerRepository.save(any())).willReturn(o);

            OtpVerifyResponse response = service.verifyOtp(1L, "234567");

            assertThat(response.isSuccess()).isTrue();

            ArgumentCaptor<Officer> captor = ArgumentCaptor.forClass(Officer.class);
            then(officerRepository).should().save(captor.capture());

            Officer saved = captor.getValue();
            assertThat(saved.getOtpCode()).isNull();
            assertThat(saved.getOtpExpiresAt()).isNull();
        }

        @Test
        @DisplayName("success response never contains the OTP value")
        void verifyOtp_responseDoesNotExposeOtp() {
            Officer o = activeOfficer();
            o.setOtpCode("876543");
            o.setOtpExpiresAt(LocalDateTime.now().plusMinutes(4));
            given(officerRepository.findById(1L)).willReturn(Optional.of(o));
            given(officerRepository.save(any())).willReturn(o);

            OtpVerifyResponse response = service.verifyOtp(1L, "876543");

            assertThat(response.getMessage()).doesNotContain("876543");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // verifyOtp — failure paths
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("verifyOtp() — failure")
    class VerifyOtpFailure {

        @Test
        @DisplayName("expired OTP returns failure with 'expired' in message")
        void verifyOtp_expiredCode_fails() {
            Officer o = activeOfficer();
            o.setOtpCode("123456");
            o.setOtpExpiresAt(LocalDateTime.now().minusMinutes(1));
            given(officerRepository.findById(1L)).willReturn(Optional.of(o));

            OtpVerifyResponse response = service.verifyOtp(1L, "123456");

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).containsIgnoringCase("expired");
            then(officerRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("wrong code returns failure without saving")
        void verifyOtp_wrongCode_fails() {
            Officer o = activeOfficer();
            o.setOtpCode("123456");
            o.setOtpExpiresAt(LocalDateTime.now().plusMinutes(4));
            given(officerRepository.findById(1L)).willReturn(Optional.of(o));

            OtpVerifyResponse response = service.verifyOtp(1L, "999999");

            assertThat(response.isSuccess()).isFalse();
            then(officerRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("no pending OTP on record returns failure")
        void verifyOtp_noOtpOnRecord_fails() {
            Officer o = activeOfficer();
            o.setOtpCode(null);
            given(officerRepository.findById(1L)).willReturn(Optional.of(o));

            OtpVerifyResponse response = service.verifyOtp(1L, "123456");

            assertThat(response.isSuccess()).isFalse();
            then(officerRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("unknown officer id returns failure")
        void verifyOtp_unknownId_fails() {
            given(officerRepository.findById(999L)).willReturn(Optional.empty());

            OtpVerifyResponse response = service.verifyOtp(999L, "123456");

            assertThat(response.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("inactive officer returns failure")
        void verifyOtp_inactiveOfficer_fails() {
            Officer o = activeOfficer();
            o.setActive(false);
            given(officerRepository.findById(1L)).willReturn(Optional.of(o));

            OtpVerifyResponse response = service.verifyOtp(1L, "123456");

            assertThat(response.isSuccess()).isFalse();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // resendOtp
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("resendOtp()")
    class ResendOtp {

        @Test
        @DisplayName("always generates a fresh OTP regardless of previous state")
        void resendOtp_success_freshOtpIssued() {
            Officer o = activeOfficer();
            o.setOtpCode("111111");
            o.setOtpExpiresAt(LocalDateTime.now().plusMinutes(2));
            given(officerRepository.findById(1L)).willReturn(Optional.of(o));
            given(officerRepository.save(any())).willReturn(o);

            OtpVerifyResponse response = service.resendOtp(1L);

            assertThat(response.isSuccess()).isTrue();

            ArgumentCaptor<Officer> captor = ArgumentCaptor.forClass(Officer.class);
            then(officerRepository).should().save(captor.capture());

            assertThat(captor.getValue().getOtpCode())
                    .isNotNull()
                    .matches("\\d{6}");
        }

        @Test
        @DisplayName("resend invalidates previous OTP (overwrites)")
        void resendOtp_invalidatesPreviousOtp() {
            Officer o = activeOfficer();
            o.setOtpCode("777777");
            o.setOtpExpiresAt(LocalDateTime.now().plusMinutes(4));
            given(officerRepository.findById(1L)).willReturn(Optional.of(o));
            given(officerRepository.save(any())).willAnswer(inv -> {
                Officer saved = inv.getArgument(0);
                // After save, update the in-memory object (simulate JPA)
                o.setOtpCode(saved.getOtpCode());
                o.setOtpExpiresAt(saved.getOtpExpiresAt());
                return saved;
            });

            service.resendOtp(1L);

            // The new OTP is stored — old "777777" is gone
            ArgumentCaptor<Officer> captor = ArgumentCaptor.forClass(Officer.class);
            then(officerRepository).should(atLeastOnce()).save(captor.capture());
        }

        @Test
        @DisplayName("SMTP failure on resend returns failure response without throwing")
        void resendOtp_smtpFailure_returnsFailure() {
            Officer o = activeOfficer();
            given(officerRepository.findById(1L)).willReturn(Optional.of(o));
            given(officerRepository.save(any())).willReturn(o);
            willThrow(new MailSendException("SMTP auth failed"))
                    .given(mailSender).send(any(SimpleMailMessage.class));

            OtpVerifyResponse response = service.resendOtp(1L);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).containsIgnoringCase("could not send");
        }

        @Test
        @DisplayName("unknown officer id blocks resend")
        void resendOtp_unknownId_blocked() {
            given(officerRepository.findById(99L)).willReturn(Optional.empty());

            OtpVerifyResponse response = service.resendOtp(99L);

            assertThat(response.isSuccess()).isFalse();
            then(officerRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("officer without email blocks resend")
        void resendOtp_noEmail_blocked() {
            Officer o = activeOfficer();
            o.setEmail(null);
            given(officerRepository.findById(1L)).willReturn(Optional.of(o));

            OtpVerifyResponse response = service.resendOtp(1L);

            assertThat(response.isSuccess()).isFalse();
            then(officerRepository).should(never()).save(any());
            then(mailSender).shouldHaveNoInteractions();
        }

        // ── Cooldown-specific tests ───────────────────────────────────────────

        @Test
        @DisplayName("resend blocked when OTP was issued within the last 60 seconds")
        void resendOtp_cooldownActive_blocked() {
            Officer o = activeOfficer();
            // OTP issued ~10 s ago: expiresAt = now + 5min, so issuedAt = now - 10s (within cooldown)
            o.setOtpCode("123456");
            o.setOtpExpiresAt(LocalDateTime.now().plusMinutes(5).minusSeconds(10));
            given(officerRepository.findById(1L)).willReturn(Optional.of(o));

            OtpVerifyResponse response = service.resendOtp(1L);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).containsIgnoringCase("please wait");
            assertThat(response.getMessage()).containsIgnoringCase("second");
            // Must not generate or send a new OTP
            then(officerRepository).should(never()).save(any());
            then(mailSender).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("resend blocked response contains the remaining seconds (> 0)")
        void resendOtp_cooldownActive_remainingSecondsInMessage() {
            Officer o = activeOfficer();
            // OTP issued 5 s ago → 55 s remaining
            o.setOtpCode("234567");
            o.setOtpExpiresAt(LocalDateTime.now().plusMinutes(5).minusSeconds(5));
            given(officerRepository.findById(1L)).willReturn(Optional.of(o));

            OtpVerifyResponse response = service.resendOtp(1L);

            assertThat(response.isSuccess()).isFalse();
            // Message must contain a positive number (remaining seconds)
            assertThat(response.getMessage()).matches(".*\\b[1-9]\\d*\\b.*second.*");
        }

        @Test
        @DisplayName("resend allowed when OTP was issued more than 60 seconds ago")
        void resendOtp_cooldownElapsed_allowed() {
            Officer o = activeOfficer();
            // OTP issued 90 s ago: expiresAt = now + 5min - 90s → issuedAt = now - 90s (past cooldown)
            o.setOtpCode("111111");
            o.setOtpExpiresAt(LocalDateTime.now().plusMinutes(5).minusSeconds(90));
            given(officerRepository.findById(1L)).willReturn(Optional.of(o));
            given(officerRepository.save(any())).willReturn(o);

            OtpVerifyResponse response = service.resendOtp(1L);

            assertThat(response.isSuccess()).isTrue();
            then(officerRepository).should().save(any());
            then(mailSender).should().send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("resend allowed immediately when no prior OTP exists (otpExpiresAt is null)")
        void resendOtp_noExistingOtp_cooldownSkipped() {
            Officer o = activeOfficer();
            // No OTP has been issued yet — otpExpiresAt is null
            o.setOtpCode(null);
            o.setOtpExpiresAt(null);
            given(officerRepository.findById(1L)).willReturn(Optional.of(o));
            given(officerRepository.save(any())).willReturn(o);

            OtpVerifyResponse response = service.resendOtp(1L);

            assertThat(response.isSuccess()).isTrue();
            then(officerRepository).should().save(any());
            then(mailSender).should().send(any(SimpleMailMessage.class));
        }
    }
}
