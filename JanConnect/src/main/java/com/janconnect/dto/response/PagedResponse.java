package com.janconnect.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagedResponse<T> {

    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();

    private final boolean success;
    private final int status;
    private final String message;
    private final List<T> data;
    private final String traceId;

    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean first;
    private final boolean last;

    public static <T> ResponseEntity<PagedResponse<T>> of(Page<T> pageResult) {
        return ResponseEntity.ok(from(pageResult, "Success"));
    }

    public static <T> ResponseEntity<PagedResponse<T>> of(Page<T> pageResult, String message) {
        return ResponseEntity.ok(from(pageResult, message));
    }

    private static <T> PagedResponse<T> from(Page<T> page, String message) {
        return PagedResponse.<T>builder()
                .success(true)
                .status(200)
                .message(message)
                .data(page.getContent())
                .traceId(MDC.get("traceId"))
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}