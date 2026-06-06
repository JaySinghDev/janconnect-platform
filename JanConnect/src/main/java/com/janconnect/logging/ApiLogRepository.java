package com.janconnect.logging;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApiLogRepository extends JpaRepository<ApiLog, Long> {

    // ── Lookup ────────────────────────────────────────────────────────────────

    List<ApiLog> findByTraceId(String traceId);

    Page<ApiLog> findAllByOrderByTimestampDesc(Pageable pageable);

    // ── Filter by status ──────────────────────────────────────────────────────

    Page<ApiLog> findByResponseStatus(int status, Pageable pageable);

    Page<ApiLog> findByResponseStatusBetween(int min, int max, Pageable pageable);

    Page<ApiLog> findByResponseStatusGreaterThanEqualOrderByTimestampDesc(int status, Pageable pageable);

    long countByResponseStatus(int status);

    long countByResponseStatusBetween(int min, int max);

    // ── Filter by method / URI ────────────────────────────────────────────────

    Page<ApiLog> findByMethod(String method, Pageable pageable);

    Page<ApiLog> findByUriContaining(String uri, Pageable pageable);

    Page<ApiLog> findByMethodAndResponseStatus(String method, int status, Pageable pageable);

    // ── Filter by client ──────────────────────────────────────────────────────

    Page<ApiLog> findByClientIp(String clientIp, Pageable pageable);

    long countByClientIp(String clientIp);

    // ── Time range ────────────────────────────────────────────────────────────

    Page<ApiLog> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime from, LocalDateTime to, Pageable pageable);

    long countByTimestampBetween(LocalDateTime from, LocalDateTime to);

    // ── Performance ───────────────────────────────────────────────────────────

    Page<ApiLog> findByDurationMsGreaterThanOrderByDurationMsDesc(long thresholdMs, Pageable pageable);

    @Query("SELECT AVG(a.durationMs) FROM ApiLog a")
    Double avgDuration();

    @Query("SELECT AVG(a.durationMs) FROM ApiLog a WHERE a.uri = :uri")
    Double avgDurationByUri(@Param("uri") String uri);

    @Query("SELECT AVG(a.durationMs) FROM ApiLog a WHERE a.timestamp BETWEEN :from AND :to")
    Double avgDurationBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT MAX(a.durationMs) FROM ApiLog a WHERE a.uri = :uri")
    Long maxDurationByUri(@Param("uri") String uri);

    // ── Error analysis ────────────────────────────────────────────────────────

    @Query("SELECT a FROM ApiLog a WHERE a.responseStatus >= 400 ORDER BY a.timestamp DESC")
    Page<ApiLog> findRecentErrors(Pageable pageable);

    Page<ApiLog> findByExceptionClassIsNotNullOrderByTimestampDesc(Pageable pageable);

    long countByExceptionClassIsNotNull();

    // ── Aggregation / analytics ───────────────────────────────────────────────

    /** Hits and average duration per endpoint, sorted by hit count descending */
    @Query("""
            SELECT a.uri, a.method,
                   COUNT(a)          AS hits,
                   AVG(a.durationMs) AS avgMs,
                   MAX(a.durationMs) AS maxMs
            FROM ApiLog a
            GROUP BY a.uri, a.method
            ORDER BY hits DESC
            """)
    List<Object[]> findEndpointStats(Pageable pageable);

    /** How many requests per HTTP status code */
    @Query("SELECT a.responseStatus, COUNT(a) FROM ApiLog a GROUP BY a.responseStatus ORDER BY a.responseStatus")
    List<Object[]> countGroupedByStatus();

    /** How many requests per HTTP method */
    @Query("SELECT a.method, COUNT(a) FROM ApiLog a GROUP BY a.method ORDER BY COUNT(a) DESC")
    List<Object[]> countGroupedByMethod();

    /** Top N callers by request volume */
    @Query("SELECT a.clientIp, COUNT(a) AS hits FROM ApiLog a GROUP BY a.clientIp ORDER BY hits DESC")
    List<Object[]> findTopCallers(Pageable pageable);

    /** Slowest individual requests */
    @Query("SELECT a FROM ApiLog a ORDER BY a.durationMs DESC")
    List<ApiLog> findSlowestRequests(Pageable pageable);

    /** Error rate (4xx+5xx count) within a time window */
    @Query("""
            SELECT COUNT(a) FROM ApiLog a
            WHERE a.responseStatus >= 400
              AND a.timestamp BETWEEN :from AND :to
            """)
    long countErrorsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Total request count within a time window */
    @Query("SELECT COUNT(a) FROM ApiLog a WHERE a.timestamp BETWEEN :from AND :to")
    long countRequestsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
