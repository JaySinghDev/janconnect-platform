package com.janconnect.exception;

public enum ErrorCode {

    RESOURCE_NOT_FOUND("ERR_001", "Resource not found"),
    DUPLICATE_RESOURCE("ERR_002", "Resource already exists"),
    VALIDATION_FAILED("ERR_003", "Validation failed"),
    INVALID_REQUEST("ERR_004", "Invalid request"),
    ACCESS_DENIED("ERR_005", "Access denied"),
    UNAUTHORIZED("ERR_006", "Unauthorized access"),
    BUSINESS_ERROR("ERR_007", "Business rule violation"),
    FILE_UPLOAD_FAILED("ERR_008", "File upload failed"),
    EXTERNAL_SERVICE_ERROR("ERR_009", "External service error"),
    INTERNAL_SERVER_ERROR("ERR_010", "Internal server error");

    private final String code;
    private final String description;

    ErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
