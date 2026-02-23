package com.collabstack.editor.service;

import com.collabstack.editor.dto.request.LoginRequest;
import com.collabstack.editor.dto.request.RegisterRequest;
import com.collabstack.editor.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
