package com.janconnect.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();
    private final boolean success;
    private final int status;
    private final String message;
    private final T data;
    private final String traceId;

    // ── 200 OK ────────────────────────────────────────────────────────────────

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(of(true, HttpStatus.OK, "Success", data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(of(true, HttpStatus.OK, message, data));
    }

    // ── 201 Created ──────────────────────────────────────────────────────────

    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(of(true, HttpStatus.CREATED, "Resource created successfully", data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(String message, T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(of(true, HttpStatus.CREATED, message, data));
    }

    // ── 204 No Content ────────────────────────────────────────────────────────

    public static ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

    // ── 202 Accepted ──────────────────────────────────────────────────────────

    public static <T> ResponseEntity<ApiResponse<T>> accepted(String message, T data) {
        return ResponseEntity.accepted()
                .body(of(true, HttpStatus.ACCEPTED, message, data));
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    private static <T> ApiResponse<T> of(boolean success, HttpStatus status,
                                          String message, T data) {
        return ApiResponse.<T>builder()
                .success(success)
                .status(status.value())
                .message(message)
                .data(data)
                .traceId(MDC.get("traceId"))
                .build();
    }
}
