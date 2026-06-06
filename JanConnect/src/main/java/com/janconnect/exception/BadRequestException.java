package com.janconnect.exception;

public class BadRequestException extends BaseException {

    public BadRequestException(String message) {
        super(message, ErrorCode.INVALID_REQUEST, 400);
    }
}
