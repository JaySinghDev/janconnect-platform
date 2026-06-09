package com.janconnect.service.impl;

import com.janconnect.config.JwtProperties;
import com.janconnect.dto.request.ForgotPasswordRequest;
import com.janconnect.dto.request.LoginRequest;
import com.janconnect.dto.request.RefreshTokenRequest;
import com.janconnect.dto.request.RegisterRequest;
import com.janconnect.dto.request.ResetPasswordRequest;
import com.janconnect.dto.response.OtpResponse;
import com.janconnect.dto.response.TokenResponse;
import com.janconnect.dto.response.UserResponse;
import com.janconnect.entity.PasswordResetOtp;
import com.janconnect.entity.Role;
import com.janconnect.entity.User;
import com.janconnect.exception.BadRequestException;
import com.janconnect.exception.DuplicateResourceException;
import com.janconnect.exception.InvalidTokenException;
import com.janconnect.exception.ResourceNotFoundException;
import com.janconnect.repository.PasswordResetOtpRepository;
import com.janconnect.repository.RoleRepository;
import com.janconnect.repository.UserRepository;
import com.janconnect.security.JanConnectUserDetailsService;
import com.janconnect.security.JwtUtil;
import com.janconnect.security.TokenBlacklistService;
import com.janconnect.service.AuthService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String TRACE_ID        = "traceId";
    private static final int    OTP_EXPIRY_MIN   = 10;

    private final UserRepository               userRepository;
    private final RoleRepository               roleRepository;
    private final PasswordResetOtpRepository   otpRepository;
    private final PasswordEncoder              passwordEncoder;
    private final JwtUtil                      jwtUtil;
    private final JanConnectUserDetailsService userDetailsService;
    private final AuthenticationManager        authenticationManager;
    private final JwtProperties                jwtProperties;
    private final TokenBlacklistService        tokenBlacklistService;

    // ── Login ─────────────────────────────────────────────────────────────────

    @Override
    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(), request.getPassword()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsernameOrEmail());
        log.info("[{}] User authenticated: {}", MDC.get(TRACE_ID), userDetails.getUsername());

        return buildTokenResponse(userDetails);
    }

    // ── Register ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        validateUniqueness(request);

        Role userRole = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role not found with id: " + request.getRoleId()));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .mobileNumber(request.getMobileNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .roles(Set.of(userRole))
                .build();

        User saved = userRepository.save(user);
        log.info("[{}] New user registered: id={} username={}",
                MDC.get(TRACE_ID), saved.getId(), saved.getUsername());

        return UserResponse.from(saved);
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Override
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        try {
            String username = jwtUtil.extractUsername(request.getRefreshToken());
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (!jwtUtil.isTokenValid(request.getRefreshToken(), userDetails)) {
                throw new InvalidTokenException("Refresh token is invalid or expired");
            }

            log.info("[{}] Token refreshed for user: {}", MDC.get(TRACE_ID), username);
            return buildTokenResponse(userDetails);

        } catch (JwtException ex) {
            throw new InvalidTokenException("Refresh token is malformed or has an invalid signature");
        }
    }

    // ── Forgot Password ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public OtpResponse forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No account found with email: " + request.getEmail()));

        // Invalidate any previous unused OTPs for this email
        otpRepository.invalidateAllByEmail(request.getEmail());

        String otp = generateOtp();

        PasswordResetOtp record = PasswordResetOtp.builder()
                .email(request.getEmail())
                .otp(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MIN))
                .used(false)
                .build();

        otpRepository.save(record);
        log.info("[{}] Password reset OTP generated for email: {}",
                MDC.get(TRACE_ID), request.getEmail());

        return OtpResponse.builder()
                .message("OTP sent to " + request.getEmail())
                .otp(otp)                          // remove this line when email/SMS is wired
                .expiresInSeconds(OTP_EXPIRY_MIN * 60L)
                .build();
    }

    // ── Reset Password ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetOtp record = otpRepository
                .findByEmailAndOtpAndUsedFalse(request.getEmail(), request.getOtp())
                .orElseThrow(() -> new BadRequestException("Invalid OTP"));

        if (LocalDateTime.now().isAfter(record.getExpiresAt())) {
            record.setUsed(true);
            otpRepository.save(record);
            throw new BadRequestException("OTP has expired. Please request a new one");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No account found with email: " + request.getEmail()));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        record.setUsed(true);
        otpRepository.save(record);

        log.info("[{}] Password reset successful for email: {}",
                MDC.get(TRACE_ID), request.getEmail());
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Override
    public void logout(String accessToken) {
        try {
            java.time.Instant expiry = jwtUtil.getExpiration(accessToken);
            tokenBlacklistService.blacklist(accessToken, expiry);
            log.info("[{}] Access token blacklisted", MDC.get(TRACE_ID));
        } catch (JwtException ex) {
            throw new InvalidTokenException("Invalid access token");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateUniqueness(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new DuplicateResourceException("Mobile number already registered: " + request.getMobileNumber());
        }
    }

    private TokenResponse buildTokenResponse(UserDetails userDetails) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return TokenResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(userDetails))
                .refreshToken(jwtUtil.generateRefreshToken(userDetails))
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpirationMs() / 1000)
                .username(userDetails.getUsername())
                .roles(roles)
                .build();
    }

    private static String generateOtp() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }
}
