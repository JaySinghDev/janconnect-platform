package com.janconnect.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Global filter that captures every HTTP request/response cycle, builds an
 * {@link ApiLog} entry, emits it via SLF4J, and persists it to the database.
 *
 * Log level by status:
 *   5xx → ERROR  (full req + res bodies, headers, exception)
 *   4xx → WARN   (full req + res bodies, headers)
 *   2xx/3xx → INFO (method, URI, status, duration, IP)
 *
 * Sensitive request headers (Authorization, Cookie, X-API-KEY, …) are masked
 * with *** before storage. Actuator endpoints are skipped entirely.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiLoggingFilter extends OncePerRequestFilter {

    private static final int MAX_BODY    = 4_000;
    private static final int MAX_HEADERS = 2_000;
    private static final String TRACE_ID = "traceId";
    private static final String MASKED   = "***";

    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "cookie", "set-cookie",
            "x-api-key", "x-auth-token", "proxy-authorization"
    );

    private final ApiLogRepository apiLogRepository;
    private final ObjectMapper     objectMapper;

    // ── Filter lifecycle ──────────────────────────────────────────────────────

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        var req = new ContentCachingRequestWrapper(request);
        var res = new ContentCachingResponseWrapper(response);

        long      start     = System.currentTimeMillis();
        Throwable thrown    = null;

        try {
            chain.doFilter(req, res);
        } catch (ServletException | IOException ex) {
            thrown = ex;
            throw ex;
        } finally {
            long   durationMs = System.currentTimeMillis() - start;
            ApiLog entry      = build(req, res, durationMs, thrown);

            emit(entry);
            persist(entry);
            res.copyBodyToResponse();
        }
    }

    // ── Log construction ──────────────────────────────────────────────────────

    private ApiLog build(ContentCachingRequestWrapper  req,
                         ContentCachingResponseWrapper res,
                         long                          durationMs,
                         Throwable                     thrown) {

        byte[] reqBytes = req.getContentAsByteArray();
        byte[] resBytes = res.getContentAsByteArray();

        return ApiLog.builder()
                // correlation
                .traceId(MDC.get(TRACE_ID))
                // request
                .method(req.getMethod())
                .uri(req.getRequestURI())
                .fullUrl(buildFullUrl(req))
                .queryString(req.getQueryString())
                .protocol(req.getProtocol())
                .serverHost(req.getServerName())
                .serverPort(req.getServerPort())
                .clientIp(resolveClientIp(req))
                .userAgent(req.getHeader("User-Agent"))
                .requestContentType(req.getContentType())
                .requestHeaders(captureRequestHeaders(req))
                .requestBody(readBody(reqBytes))
                .requestSize(reqBytes.length)
                // response
                .responseStatus(res.getStatus())
                .responseContentType(res.getContentType())
                .responseHeaders(captureResponseHeaders(res))
                .responseBody(readBody(resBytes))
                .responseSize(resBytes.length)
                // performance
                .durationMs(durationMs)
                // error
                .exceptionClass(thrown != null ? thrown.getClass().getName() : null)
                .errorMessage(thrown != null ? truncate(thrown.getMessage(), 500) : null)
                .build();
    }

    // ── Emission ──────────────────────────────────────────────────────────────

    private void emit(ApiLog e) {
        int status = e.getResponseStatus();

        if (status >= 500) {
            log.error("[{}] {} {} -> {} {}ms | ip={} size={}b | req={} | res={} | ex={}",
                    e.getTraceId(), e.getMethod(), e.getUri(),
                    status, e.getDurationMs(),
                    e.getClientIp(), e.getResponseSize(),
                    e.getRequestBody(), e.getResponseBody(),
                    e.getExceptionClass());

        } else if (status >= 400) {
            log.warn("[{}] {} {} -> {} {}ms | ip={} | req={} | res={}",
                    e.getTraceId(), e.getMethod(), e.getUri(),
                    status, e.getDurationMs(),
                    e.getClientIp(),
                    e.getRequestBody(), e.getResponseBody());

        } else {
            log.info("[{}] {} {} -> {} {}ms | ip={} | req={}b res={}b",
                    e.getTraceId(), e.getMethod(), e.getUri(),
                    status, e.getDurationMs(),
                    e.getClientIp(),
                    e.getRequestSize(), e.getResponseSize());
        }
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void persist(ApiLog entry) {
        try {
            apiLogRepository.save(entry);
        } catch (Exception ex) {
            log.warn("Failed to persist ApiLog [{} {}]: {}",
                    entry.getMethod(), entry.getUri(), ex.getMessage());
        }
    }

    // ── Header capture ────────────────────────────────────────────────────────

    private String captureRequestHeaders(HttpServletRequest req) {
        Map<String, String> headers = new LinkedHashMap<>();
        Collections.list(req.getHeaderNames()).forEach(name ->
                headers.put(name, isSensitive(name) ? MASKED : req.getHeader(name))
        );
        return toJson(headers, MAX_HEADERS);
    }

    private String captureResponseHeaders(ContentCachingResponseWrapper res) {
        Map<String, String> headers = new LinkedHashMap<>();
        res.getHeaderNames().forEach(name ->
                headers.put(name, isSensitive(name) ? MASKED : res.getHeader(name))
        );
        return toJson(headers, MAX_HEADERS);
    }

    private boolean isSensitive(String headerName) {
        return SENSITIVE_HEADERS.contains(headerName.toLowerCase());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildFullUrl(HttpServletRequest req) {
        StringBuilder url = new StringBuilder(req.getRequestURL());
        if (req.getQueryString() != null) url.append('?').append(req.getQueryString());
        return truncate(url.toString(), 1000);
    }

    private String resolveClientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) return ip.split(",")[0].trim();

        ip = req.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) return ip;

        return req.getRemoteAddr();
    }

    private String readBody(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        String body = new String(bytes, StandardCharsets.UTF_8);
        return body.length() > MAX_BODY ? body.substring(0, MAX_BODY) + "…[truncated]" : body;
    }

    private String toJson(Map<String, String> map, int maxLen) {
        if (map.isEmpty()) return null;
        try {
            String json = objectMapper.writeValueAsString(map);
            return json.length() > maxLen ? json.substring(0, maxLen) + "…}" : json;
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }
}
