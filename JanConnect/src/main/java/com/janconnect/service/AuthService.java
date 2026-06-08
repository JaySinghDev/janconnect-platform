package com.janconnect.service;

import com.janconnect.dto.request.LoginRequest;
import com.janconnect.dto.request.RefreshTokenRequest;
import com.janconnect.dto.request.RegisterRequest;
import com.janconnect.dto.response.TokenResponse;
import com.janconnect.dto.response.UserResponse;

public interface AuthService {

    /** Authenticate and return JWT token pair. */
    TokenResponse login(LoginRequest request);

    /** Create a new user account and return the persisted user details. */
    UserResponse register(RegisterRequest request);

    /** Exchange a valid refresh token for a new access + refresh token pair. */
    TokenResponse refreshToken(RefreshTokenRequest request);
}
