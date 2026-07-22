package com.susumonitor.server.common;

import org.springframework.http.HttpStatus;

/**
 * Project-wide error codes defined by the API contract.
 */
public enum ErrorCode {

    SUCCESS(0, "success", HttpStatus.OK),
    BAD_REQUEST(40000, "bad request", HttpStatus.BAD_REQUEST),
    INVALID_USERNAME_OR_PASSWORD(40001, "invalid username or password", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST_PARAMETER(40002, "invalid request parameter", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(40100, "unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(40300, "forbidden", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(40400, "resource not found", HttpStatus.NOT_FOUND),
    RESOURCE_CONFLICT(40900, "resource conflict", HttpStatus.CONFLICT),
    SSH_HOST_KEY_NOT_CONFIRMED(40901, "ssh host key not confirmed", HttpStatus.CONFLICT),
    SSH_HOST_KEY_MISMATCH(40902, "ssh host key mismatch", HttpStatus.CONFLICT),
    SSH_TARGET_FORBIDDEN(40301, "ssh target forbidden", HttpStatus.FORBIDDEN),
    SSH_CONNECTION_LIMIT_REACHED(42900, "ssh connection limit reached", HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_SERVER_ERROR(50000, "internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    DATABASE_ERROR(50001, "database error", HttpStatus.INTERNAL_SERVER_ERROR),
    SSH_CONNECTION_FAILED(50002, "ssh connection failed", HttpStatus.BAD_GATEWAY),
    SSH_CONNECTION_TIMEOUT(50400, "ssh connection timeout", HttpStatus.GATEWAY_TIMEOUT);

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
