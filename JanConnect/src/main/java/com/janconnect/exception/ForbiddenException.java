package com.janconnect.exception;

public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super(message, ErrorCode.ACCESS_DENIED, 403);
    }
}
