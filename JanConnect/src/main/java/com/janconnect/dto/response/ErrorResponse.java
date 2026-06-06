package com.janconnect.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.janconnect.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();
    private final int status;
    private final String error;
    private final String errorCode;
    private final String message;
    private final String path;
    private final String traceId;
    private final List<ValidationError> errors;

    public static ErrorResponse of(int status, String error, ErrorCode errorCode,
                                   String message, String path, String traceId) {
        return ErrorResponse.builder()
                .status(status)
                .error(error)
                .errorCode(errorCode.getCode())
                .message(message)
                .path(path)
                .traceId(traceId)
                .build();
    }

    public static ErrorResponse ofWithErrors(int status, String error, ErrorCode errorCode,
                                              String message, String path, String traceId,
                                              List<ValidationError> errors) {
        return ErrorResponse.builder()
                .status(status)
                .error(error)
                .errorCode(errorCode.getCode())
                .message(message)
                .path(path)
                .traceId(traceId)
                .errors(errors)
                .build();
    }
}
