package com.dsgp.authentication.service;

import com.dsgp.authentication.dto.LoginRequest;
import com.dsgp.authentication.dto.LoginResponse;
import com.dsgp.authentication.dto.OfficerLoginRequest;
import com.dsgp.authentication.dto.OfficerLoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    /**
     * Authenticates a government officer using their username and password.
     *
     * @param request the officer's credentials
     * @return {@link OfficerLoginResponse} containing role, district, and officer identity
     */
    OfficerLoginResponse officerLogin(OfficerLoginRequest request);
}