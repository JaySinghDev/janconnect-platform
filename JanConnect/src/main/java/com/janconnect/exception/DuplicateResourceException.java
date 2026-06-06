package com.janconnect.exception;

public class DuplicateResourceException extends BaseException {

    public DuplicateResourceException(String message) {
        super(message, ErrorCode.DUPLICATE_RESOURCE, 409);
    }

    public DuplicateResourceException(String resource, String field, Object value) {
        super(String.format("%s already exists with %s: '%s'", resource, field, value),
                ErrorCode.DUPLICATE_RESOURCE, 409);
    }
}
