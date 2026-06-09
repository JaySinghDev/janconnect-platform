package com.janconnect.controller;

import com.janconnect.exception.InvalidTokenException;
import com.janconnect.dto.request.ForgotPasswordRequest;
import com.janconnect.dto.request.LoginRequest;
import com.janconnect.dto.request.RefreshTokenRequest;
import com.janconnect.dto.request.RegisterRequest;
import com.janconnect.dto.request.ResetPasswordRequest;
import com.janconnect.dto.response.ApiResponse;
import com.janconnect.dto.response.OtpResponse;
import com.janconnect.dto.response.TokenResponse;
import com.janconnect.dto.response.UserResponse;
import com.janconnect.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration and JWT authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @SecurityRequirements
    @Operation(
            summary = "Register a new user",
            description = "Creates a new citizen account with ROLE_USER. " +
                          "Returns the persisted user details. Call /login to obtain JWT tokens.")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        return ApiResponse.created("User registered successfully", authService.register(request));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(
            summary = "Login and obtain JWT tokens",
            description = "Accepts username or email with password. Returns access + refresh tokens.")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Login successful", authService.login(request));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(
            summary = "Refresh an expired access token",
            description = "Provide a valid refresh token to obtain a new access + refresh token pair.")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok("Token refreshed", authService.refreshToken(request));
    }

    @PostMapping("/forgot-password")
    @SecurityRequirements
    @Operation(
            summary = "Request a password reset OTP",
            description = "Generates a 6-digit OTP valid for 10 minutes and sends it to the registered email.")
    public ResponseEntity<ApiResponse<OtpResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ApiResponse.ok("OTP generated", authService.forgotPassword(request));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout",
            description = "Blacklists the current access token. Subsequent requests with this token will be rejected.")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new InvalidTokenException("No valid Bearer token provided");
        }
        authService.logout(authHeader.substring(7));
        return ApiResponse.noContent();
    }

    @PostMapping("/reset-password")
    @SecurityRequirements
    @Operation(
            summary = "Reset password using OTP",
            description = "Validates the OTP and updates the account password.")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.noContent();
    }
}
