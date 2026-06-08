package com.janconnect.exception;

import com.janconnect.dto.response.ErrorResponse;
import com.janconnect.dto.response.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TRACE_ID = "traceId";

    // ── Custom exceptions ─────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        logWarn(ex, request);
        return build(HttpStatus.NOT_FOUND, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {
        logWarn(ex, request);
        return build(HttpStatus.CONFLICT, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex, HttpServletRequest request) {
        logWarn(ex, request);
        return build(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex, HttpServletRequest request) {
        logWarn(ex, request);
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedException ex, HttpServletRequest request) {
        logWarn(ex, request);
        return build(HttpStatus.UNAUTHORIZED, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(
            InvalidTokenException ex, HttpServletRequest request) {
        logWarn(ex, request);
        return build(HttpStatus.UNAUTHORIZED, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpired(
            TokenExpiredException ex, HttpServletRequest request) {
        logWarn(ex, request);
        return build(HttpStatus.UNAUTHORIZED, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            ForbiddenException ex, HttpServletRequest request) {
        logWarn(ex, request);
        return build(HttpStatus.FORBIDDEN, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ErrorResponse> handleFileUpload(
            FileUploadException ex, HttpServletRequest request) {
        logWarn(ex, request);
        return build(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalService(
            ExternalServiceException ex, HttpServletRequest request) {
        logWarn(ex, request);
        return build(HttpStatus.BAD_GATEWAY, ex.getErrorCode(), ex.getMessage(), request);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            ValidationException ex, HttpServletRequest request) {
        logWarn(ex, request);
        return buildWithErrors(HttpStatus.BAD_REQUEST, ex.getErrorCode(),
                ex.getMessage(), request, ex.getErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ValidationError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        log.warn("[{}] {} {} MethodArgumentNotValid — {} field error(s)",
                MDC.get(TRACE_ID), request.getMethod(), request.getRequestURI(), errors.size());
        return buildWithErrors(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED,
                "Validation failed", request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        List<ValidationError> errors = ex.getConstraintViolations().stream()
                .map(cv -> new ValidationError(
                        cv.getPropertyPath().toString(),
                        cv.getMessage()))
                .toList();
        log.warn("[{}] {} {} ConstraintViolation — {} violation(s)",
                MDC.get(TRACE_ID), request.getMethod(), request.getRequestURI(), errors.size());
        return buildWithErrors(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED,
                "Validation failed", request, errors);
    }

    // ── Spring Security ───────────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("[{}] {} {} AccessDenied — {}",
                MDC.get(TRACE_ID), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, "Access denied", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("[{}] {} {} AuthenticationException — {}",
                MDC.get(TRACE_ID), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Authentication required", request);
    }

    // ── Data layer ────────────────────────────────────────────────────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("[{}] {} {} DataIntegrityViolation — {}",
                MDC.get(TRACE_ID), request.getMethod(), request.getRequestURI(),
                ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_RESOURCE,
                "Data integrity violation — record may already exist", request);
    }

    // ── Catch-all ─────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(
            Exception ex, HttpServletRequest request) {
        log.error("[{}] {} {} Unhandled exception — {}",
                MDC.get(TRACE_ID), request.getMethod(), request.getRequestURI(),
                ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred", request);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> build(HttpStatus status, ErrorCode errorCode,
                                                 String message, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                status.value(), status.getReasonPhrase(),
                errorCode, message,
                request.getRequestURI(), MDC.get(TRACE_ID));
        return ResponseEntity.status(status).body(body);
    }

    private ResponseEntity<ErrorResponse> buildWithErrors(HttpStatus status, ErrorCode errorCode,
                                                           String message, HttpServletRequest request,
                                                           List<ValidationError> errors) {
        ErrorResponse body = ErrorResponse.ofWithErrors(
                status.value(), status.getReasonPhrase(),
                errorCode, message,
                request.getRequestURI(), MDC.get(TRACE_ID), errors);
        return ResponseEntity.status(status).body(body);
    }

    private void logWarn(BaseException ex, HttpServletRequest request) {
        log.warn("[{}] {} {} {} — {}",
                MDC.get(TRACE_ID), request.getMethod(), request.getRequestURI(),
                ex.getClass().getSimpleName(), ex.getMessage());
    }
}
