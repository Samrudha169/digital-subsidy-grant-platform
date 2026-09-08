package com.dsgp.authentication.controller;

import com.dsgp.authentication.dto.LoginRequest;
import com.dsgp.authentication.dto.LoginResponse;
import com.dsgp.authentication.dto.OfficerLoginRequest;
import com.dsgp.authentication.dto.OfficerLoginResponse;
import com.dsgp.authentication.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller for both beneficiaries and government officers.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /auth/login}         — beneficiary login (unchanged)</li>
 *   <li>{@code POST /auth/officer-login} — officer login (new)</li>
 * </ul>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ── Beneficiary login (unchanged) ─────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);

        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    // ── Officer login ─────────────────────────────────────────────────────────

    /**
     * Authenticates a government officer (Field, District, or Finance).
     *
     * @param request officer username + password
     * @return 200 with role and district on success; 401 on failure
     */
    @PostMapping("/officer-login")
    public ResponseEntity<OfficerLoginResponse> officerLogin(
            @Valid @RequestBody OfficerLoginRequest request) {

        OfficerLoginResponse response = authService.officerLogin(request);

        if (!response.isSuccess()) {
            return ResponseEntity.status(401).body(response);
        }

        return ResponseEntity.ok(response);
    }
}