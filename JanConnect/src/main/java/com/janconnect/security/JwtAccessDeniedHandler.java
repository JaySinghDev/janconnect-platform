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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {

        log.warn("[{}] Access denied {} {}: {}",
                MDC.get("traceId"), request.getMethod(), request.getRequestURI(), ex.getMessage());

        ErrorResponse body = ErrorResponse.of(
                HttpServletResponse.SC_FORBIDDEN, "Forbidden",
                ErrorCode.ACCESS_DENIED,
                "Access denied — insufficient permissions for this resource",
                request.getRequestURI(), MDC.get("traceId"));

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
