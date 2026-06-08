package com.janconnect.exception;

public class TokenExpiredException extends BaseException {

    public TokenExpiredException() {
        super("Authentication token has expired", ErrorCode.EXPIRED_TOKEN, 401);
    }
}
