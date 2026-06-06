package com.janconnect.exception;

public class ExternalServiceException extends BaseException {

    public ExternalServiceException(String message) {
        super(message, ErrorCode.EXTERNAL_SERVICE_ERROR, 502);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, ErrorCode.EXTERNAL_SERVICE_ERROR, 502, cause);
    }
}
