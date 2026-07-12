package com.susumonitor.server.common;

import org.springframework.http.HttpStatus;

/**
 * Project-wide error codes defined by the API contract.
 */
public enum ErrorCode {

    SUCCESS(0, "success", HttpStatus.OK),
    BAD_REQUEST(40000, "bad request", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST_PARAMETER(40002, "invalid request parameter", HttpStatus.BAD_REQUEST),
    RESOURCE_CONFLICT(40900, "resource conflict", HttpStatus.CONFLICT),
    INTERNAL_SERVER_ERROR(50000, "internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    DATABASE_ERROR(50001, "database error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;

    private final String message;

    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
