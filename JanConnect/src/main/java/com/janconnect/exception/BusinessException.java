package com.janconnect.exception;

public class BusinessException extends BaseException {

    public BusinessException(String message) {
        super(message, ErrorCode.BUSINESS_ERROR, 422);
    }

    public BusinessException(String message, ErrorCode errorCode) {
        super(message, errorCode, 422);
    }
}
