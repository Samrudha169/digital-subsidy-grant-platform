package com.dsgp.authentication.service;

import com.dsgp.authentication.dto.AdminOfficerRequest;
import com.dsgp.authentication.entity.Officer;
import com.dsgp.authentication.entity.OfficerRole;
import com.dsgp.authentication.repository.OfficerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;



import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link AdminOfficerServiceImpl}.
 *
 * <p>Covers the {@code validateRequest()} guard logic, with particular focus
 * on the email-mandatory rule introduced for OTP-based officer roles.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminOfficerServiceImpl")
class AdminOfficerServiceImplTest {

    @Mock
    private OfficerRepository officerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminOfficerServiceImpl service;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private AdminOfficerRequest validRequest(OfficerRole role) {
        return new AdminOfficerRequest(
                "officer.user",
                "password123",
                "Test Officer",
                "officer@dsgp.gov.in",
                role,
                "Pune"
        );
    }

    private AdminOfficerRequest requestWithoutEmail(OfficerRole role) {
        return new AdminOfficerRequest(
                "officer.user",
                "password123",
                "Test Officer",
                null,           // no email
                role,
                "Pune"
        );
    }

    private AdminOfficerRequest requestWithBlankEmail(OfficerRole role) {
        return new AdminOfficerRequest(
                "officer.user",
                "password123",
                "Test Officer",
                "   ",          // blank email
                role,
                "Pune"
        );
    }

    // ── validateRequest – email mandatory for OTP roles ──────────────────────

    @Nested
    @DisplayName("email validation")
    class EmailValidation {

        @ParameterizedTest(name = "null email rejected for role {0}")
        @EnumSource(value = OfficerRole.class,
                names = {"FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER"})
        @DisplayName("null email is rejected for OTP-based roles")
        void nullEmailRejectedForOtpRoles(OfficerRole role) {
            AdminOfficerRequest request = requestWithoutEmail(role);

            assertThatThrownBy(() -> service.createOfficer(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email is required for officer OTP login.");
        }

        @ParameterizedTest(name = "blank email rejected for role {0}")
        @EnumSource(value = OfficerRole.class,
                names = {"FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER"})
        @DisplayName("blank email is rejected for OTP-based roles")
        void blankEmailRejectedForOtpRoles(OfficerRole role) {
            AdminOfficerRequest request = requestWithBlankEmail(role);

            assertThatThrownBy(() -> service.createOfficer(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email is required for officer OTP login.");
        }

        @Test
        @DisplayName("null email is ALLOWED for ADMIN role")
        void nullEmailAllowedForAdmin() {
            // Stub repository so the flow reaches the password check, not a DB call
            given(officerRepository.existsByUsername(anyString())).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("encoded");

            Officer savedAdmin = Officer.builder()
                    .id(10L)
                    .username("admin.user")
                    .fullName("Admin User")
                    .email(null)
                    .role(OfficerRole.ADMIN)
                    .district(null)
                    .active(true)
                    .build();

            given(officerRepository.save(any(Officer.class))).willReturn(savedAdmin);

            AdminOfficerRequest request = requestWithoutEmail(OfficerRole.ADMIN);

            // Must NOT throw the email validation exception
            assertThatCode(() -> service.createOfficer(request))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "valid email accepted for role {0}")
        @EnumSource(value = OfficerRole.class,
                names = {"FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER"})
        @DisplayName("a non-blank email passes validation for OTP-based roles")
        void validEmailPassesForOtpRoles(OfficerRole role) {
            given(officerRepository.existsByUsername(anyString())).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("encoded");

            Officer saved = Officer.builder()
                    .id(1L)
                    .username("officer.user")
                    .fullName("Test Officer")
                    .email("officer@dsgp.gov.in")
                    .role(role)
                    .district("Pune")
                    .active(true)
                    .build();

            given(officerRepository.save(any(Officer.class))).willReturn(saved);

            assertThatCode(() -> service.createOfficer(validRequest(role)))
                    .doesNotThrowAnyException();
        }
    }

    // ── validateRequest – pre-existing validations preserved ─────────────────

    @Nested
    @DisplayName("pre-existing validation")
    class PreExistingValidation {

        @Test
        @DisplayName("null request throws")
        void nullRequestThrows() {
            assertThatThrownBy(() -> service.createOfficer(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Officer information is required.");
        }

        @Test
        @DisplayName("blank username throws")
        void blankUsernameThrows() {
            AdminOfficerRequest request = new AdminOfficerRequest(
                    "  ", "pass", "Full Name",
                    "e@x.com", OfficerRole.FIELD_OFFICER, "Pune"
            );

            assertThatThrownBy(() -> service.createOfficer(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Username is required.");
        }

        @Test
        @DisplayName("blank fullName throws")
        void blankFullNameThrows() {
            AdminOfficerRequest request = new AdminOfficerRequest(
                    "user", "pass", "  ",
                    "e@x.com", OfficerRole.FIELD_OFFICER, "Pune"
            );

            assertThatThrownBy(() -> service.createOfficer(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Full name is required.");
        }

        @Test
        @DisplayName("null role throws")
        void nullRoleThrows() {
            AdminOfficerRequest request = new AdminOfficerRequest(
                    "user", "pass", "Full Name",
                    "e@x.com", null, "Pune"
            );

            assertThatThrownBy(() -> service.createOfficer(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Officer role is required.");
        }

        @Test
        @DisplayName("duplicate username throws")
        void duplicateUsernameThrows() {
            given(officerRepository.existsByUsername("officer.user"))
                    .willReturn(true);

            assertThatThrownBy(() ->
                    service.createOfficer(validRequest(OfficerRole.FIELD_OFFICER)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Username is already taken.");
        }

        @Test
        @DisplayName("missing password on create throws")
        void missingPasswordOnCreateThrows() {
            given(officerRepository.existsByUsername(anyString())).willReturn(false);

            AdminOfficerRequest request = new AdminOfficerRequest(
                    "officer.user", "", "Test Officer",
                    "e@x.com", OfficerRole.FIELD_OFFICER, "Pune"
            );

            assertThatThrownBy(() -> service.createOfficer(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Password is required when creating an officer.");
        }

        @Test
        @DisplayName("updateOfficer also enforces email validation")
        void updateOfficerEnforcesEmailValidation() {
            // validateRequest() is the first thing called in updateOfficer();
            // it throws before any repository lookup, so no stub is needed.
            AdminOfficerRequest request = requestWithoutEmail(OfficerRole.DISTRICT_OFFICER);

            assertThatThrownBy(() -> service.updateOfficer(1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email is required for officer OTP login.");
        }
    }
}
