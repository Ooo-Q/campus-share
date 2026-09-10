package com.campusshare.service;

import com.campusshare.dto.LoginRequest;
import com.campusshare.dto.RegisterRequest;
import com.campusshare.vo.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);

    AuthResponse register(RegisterRequest request);
}
