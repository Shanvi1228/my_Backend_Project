package com.collabstack.storage.service;

import com.collabstack.storage.dto.request.LoginRequest;
import com.collabstack.storage.dto.request.RegisterRequest;
import com.collabstack.storage.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
