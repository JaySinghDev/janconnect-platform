package com.janconnect.exception;

public class FileUploadException extends BaseException {

    public FileUploadException(String message) {
        super(message, ErrorCode.FILE_UPLOAD_FAILED, 400);
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, ErrorCode.FILE_UPLOAD_FAILED, 400, cause);
    }
}
