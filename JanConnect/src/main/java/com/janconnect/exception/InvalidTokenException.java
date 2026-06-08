package com.janconnect.exception;

public class InvalidTokenException extends BaseException {

    public InvalidTokenException(String message) {
        super(message, ErrorCode.INVALID_TOKEN, 401);
    }
}
