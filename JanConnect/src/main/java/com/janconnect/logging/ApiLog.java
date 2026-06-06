package com.janconnect.logging;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "jc_api_logs",
    indexes = {
        @Index(name = "idx_api_logs_trace_id",       columnList = "trace_id"),
        @Index(name = "idx_api_logs_timestamp",       columnList = "timestamp"),
        @Index(name = "idx_api_logs_response_status", columnList = "response_status"),
        @Index(name = "idx_api_logs_uri",             columnList = "uri"),
        @Index(name = "idx_api_logs_method",          columnList = "method"),
        @Index(name = "idx_api_logs_client_ip",       columnList = "client_ip"),
        @Index(name = "idx_api_logs_duration_ms",     columnList = "duration_ms")
    }
)
@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class ApiLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Correlation ───────────────────────────────────────────────────────────

    @Column(name = "trace_id", length = 32)
    private String traceId;

    // ── Request ───────────────────────────────────────────────────────────────

    @Column(name = "method", length = 10, nullable = false)
    private String method;

    @Column(name = "uri", length = 500, nullable = false)
    private String uri;

    @Column(name = "full_url", length = 1000)
    private String fullUrl;

    @Column(name = "query_string", length = 1000)
    private String queryString;

    @Column(name = "protocol", length = 20)
    private String protocol;

    @Column(name = "server_host", length = 255)
    private String serverHost;

    @Column(name = "server_port")
    private int serverPort;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "request_content_type", length = 100)
    private String requestContentType;

    /** JSON map of request headers; sensitive values replaced with *** */
    @Column(name = "request_headers", columnDefinition = "TEXT")
    private String requestHeaders;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    /** Raw byte length of the request body before truncation */
    @Column(name = "request_size")
    private long requestSize;

    // ── Response ──────────────────────────────────────────────────────────────

    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Column(name = "response_content_type", length = 100)
    private String responseContentType;

    /** JSON map of response headers */
    @Column(name = "response_headers", columnDefinition = "TEXT")
    private String responseHeaders;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    /** Raw byte length of the response body before truncation */
    @Column(name = "response_size")
    private long responseSize;

    // ── Performance ───────────────────────────────────────────────────────────

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    // ── Error ─────────────────────────────────────────────────────────────────

    /** Fully-qualified class name of any unhandled exception */
    @Column(name = "exception_class", length = 255)
    private String exceptionClass;

    /** Exception message, truncated to 500 chars */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    // ── Meta ──────────────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
}
