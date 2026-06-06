package com.janconnect.exception;

import com.janconnect.dto.response.ValidationError;
import lombok.Getter;

import java.util.List;

@Getter
public class ValidationException extends BaseException {

    private final List<ValidationError> errors;

    public ValidationException(List<ValidationError> errors) {
        super("Validation failed", ErrorCode.VALIDATION_FAILED, 400);
        this.errors = List.copyOf(errors);
    }

    public ValidationException(String field, String message) {
        super("Validation failed", ErrorCode.VALIDATION_FAILED, 400);
        this.errors = List.of(new ValidationError(field, message));
    }
}
