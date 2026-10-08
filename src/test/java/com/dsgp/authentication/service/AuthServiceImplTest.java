package com.dsgp.authentication.service;

import com.dsgp.authentication.dto.LoginRequest;
import com.dsgp.authentication.dto.LoginResponse;
import com.dsgp.authentication.repository.OfficerRepository;
import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.RegistrationStatus;
import com.dsgp.beneficiary.repository.BeneficiaryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link AuthServiceImpl#login(LoginRequest)}.
 *
 * Covers the two-step beneficiary login flow:
 *  1. Email + password validation (unchanged guard)
 *  2. Verified accounts: login OTP dispatched, otpRequired=true returned; session NOT granted.
 *  3. Legacy accounts (emailVerified=false, otpCode=null) pass through immediately.
 *  4. Newly-registered but unverified accounts remain blocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl - beneficiary login")
class AuthServiceImplTest {

    @Mock private BeneficiaryRepository beneficiaryRepository;
    @Mock private OfficerRepository officerRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private OfficerOtpService officerOtpService;
    @Mock private EmailOtpService emailOtpService;

    @InjectMocks
    private AuthServiceImpl authService;

    // ── Shared builders ───────────────────────────────────────────────────────

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    private Beneficiary verifiedBeneficiary() {
        return Beneficiary.builder()
                .id(10).fullName("Verified User").email("verified@example.com")
                .password("$2a$10$hashed").govId("GOV001").contact("9000000001")
                .age(30).address("1 Main Rd").schemeName("PM-KISAN")
                .registrationStatus(RegistrationStatus.ACTIVE).emailVerified(true).build();
    }

    private Beneficiary legacyBeneficiary() {
        return Beneficiary.builder()
                .id(11).fullName("Legacy User").email("legacy@example.com")
                .password("$2a$10$hashed").govId("GOV002").contact("9000000002")
                .age(40).address("2 Old Rd").schemeName("NSP")
                .registrationStatus(RegistrationStatus.ACTIVE).emailVerified(false).build();
    }

    private Beneficiary unverifiedBeneficiary() {
        Beneficiary b = Beneficiary.builder()
                .id(12).fullName("Unverified User").email("unverified@example.com")
                .password("$2a$10$hashed").govId("GOV003").contact("9000000003")
                .age(25).address("3 New Rd").schemeName("PM-KISAN")
                .registrationStatus(RegistrationStatus.PENDING).emailVerified(false).build();
        b.setOtpCode("999888");
        return b;
    }

    // ════════════════════════════════════════════════════════════════════════
    // Credential validation failures
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Credential validation")
    class CredentialValidation {

        @Test
        @DisplayName("returns failure when email is not registered")
        void login_unknownEmail_returnsFailure() {
            given(beneficiaryRepository.findFirstByEmail("nobody@x.com")).willReturn(Optional.empty());

            LoginResponse r = authService.login(loginRequest("nobody@x.com", "pass"));

            assertThat(r.isSuccess()).isFalse();
            assertThat(r.isOtpRequired()).isFalse();
            then(emailOtpService).should(never()).sendLoginOtp(anyString());
        }

        @Test
        @DisplayName("returns failure when password does not match")
        void login_wrongPassword_returnsFailure() {
            Beneficiary b = verifiedBeneficiary();
            given(beneficiaryRepository.findFirstByEmail("verified@example.com")).willReturn(Optional.of(b));
            given(passwordEncoder.matches("wrong", b.getPassword())).willReturn(false);

            LoginResponse r = authService.login(loginRequest("verified@example.com", "wrong"));

            assertThat(r.isSuccess()).isFalse();
            assertThat(r.isOtpRequired()).isFalse();
            then(emailOtpService).should(never()).sendLoginOtp(anyString());
        }

        @Test
        @DisplayName("blocks login when account is registered but email not yet verified")
        void login_unverifiedAccount_blocked() {
            Beneficiary b = unverifiedBeneficiary();
            given(beneficiaryRepository.findFirstByEmail("unverified@example.com")).willReturn(Optional.of(b));
            given(passwordEncoder.matches("pass", b.getPassword())).willReturn(true);

            LoginResponse r = authService.login(loginRequest("unverified@example.com", "pass"));

            assertThat(r.isSuccess()).isFalse();
            assertThat(r.getMessage()).containsIgnoringCase("verify your email");
            assertThat(r.isOtpRequired()).isFalse();
            then(emailOtpService).should(never()).sendLoginOtp(anyString());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Legacy account - immediate pass-through (backward compatibility)
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Legacy account login")
    class LegacyLogin {

        @Test
        @DisplayName("grants session immediately for legacy accounts (emailVerified=false, otpCode=null)")
        void login_legacyAccount_grantsSessionImmediately() {
            Beneficiary b = legacyBeneficiary();
            given(beneficiaryRepository.findFirstByEmail("legacy@example.com")).willReturn(Optional.of(b));
            given(passwordEncoder.matches("pass", b.getPassword())).willReturn(true);

            LoginResponse r = authService.login(loginRequest("legacy@example.com", "pass"));

            assertThat(r.isSuccess()).isTrue();
            assertThat(r.isOtpRequired()).isFalse();
            assertThat(r.getBeneficiaryId()).isEqualTo(11);
            assertThat(r.getName()).isEqualTo("Legacy User");
            then(emailOtpService).should(never()).sendLoginOtp(anyString());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Two-step login - verified accounts
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Two-step login (verified accounts)")
    class TwoStepLogin {

        @Test
        @DisplayName("dispatches login OTP and returns otpRequired=true for verified accounts")
        void login_verifiedAccount_dispatchesOtpAndReturnsOtpRequired() {
            Beneficiary b = verifiedBeneficiary();
            given(beneficiaryRepository.findFirstByEmail("verified@example.com")).willReturn(Optional.of(b));
            given(passwordEncoder.matches("pass", b.getPassword())).willReturn(true);
            willDoNothing().given(emailOtpService).sendLoginOtp("verified@example.com");

            LoginResponse r = authService.login(loginRequest("verified@example.com", "pass"));

            assertThat(r.isSuccess()).isTrue();
            assertThat(r.isOtpRequired()).isTrue();
            assertThat(r.getEmail()).isEqualTo("verified@example.com");
            // Session NOT granted yet - beneficiaryId and name must be null
            assertThat(r.getBeneficiaryId()).isNull();
            assertThat(r.getName()).isNull();
            then(emailOtpService).should().sendLoginOtp("verified@example.com");
        }

        @Test
        @DisplayName("returns failure when OTP dispatch throws (e.g. SMTP down)")
        void login_otpSendFails_returnsFailure() {
            Beneficiary b = verifiedBeneficiary();
            given(beneficiaryRepository.findFirstByEmail("verified@example.com")).willReturn(Optional.of(b));
            given(passwordEncoder.matches("pass", b.getPassword())).willReturn(true);
            willThrow(new IllegalStateException("SMTP unavailable"))
                    .given(emailOtpService).sendLoginOtp("verified@example.com");

            LoginResponse r = authService.login(loginRequest("verified@example.com", "pass"));

            assertThat(r.isSuccess()).isFalse();
            assertThat(r.isOtpRequired()).isFalse();
            assertThat(r.getBeneficiaryId()).isNull();
        }
    }
}