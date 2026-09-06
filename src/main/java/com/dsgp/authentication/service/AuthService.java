package com.dsgp.authentication.service;

import com.dsgp.authentication.dto.LoginRequest;
import com.dsgp.authentication.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}