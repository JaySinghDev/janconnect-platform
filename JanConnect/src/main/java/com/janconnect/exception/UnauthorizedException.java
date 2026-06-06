package com.janconnect.exception;

public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(message, ErrorCode.UNAUTHORIZED, 401);
    }
}
