package com.janconnect.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.janconnect.dto.response.ErrorResponse;
import com.janconnect.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException ex) throws IOException {

        log.warn("[{}] Unauthorized {} {}: {}",
                MDC.get("traceId"), request.getMethod(), request.getRequestURI(), ex.getMessage());

        ErrorResponse body = ErrorResponse.of(
                HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                ErrorCode.UNAUTHORIZED,
                "Authentication required — provide a valid Bearer token or API key",
                request.getRequestURI(), MDC.get("traceId"));

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
