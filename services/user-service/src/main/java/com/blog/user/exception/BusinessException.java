package com.blog.user.exception;

public class BusinessException extends RuntimeException {

    private final int httpStatus;
    private final String code;

    public BusinessException(int httpStatus, String code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(400, "BAD_REQUEST", message);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(401, "UNAUTHORIZED", message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(409, "CONFLICT", message);
    }

    public int getHttpStatus() { return httpStatus; }
    public String getCode() { return code; }
}
