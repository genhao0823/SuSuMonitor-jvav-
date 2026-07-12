package com.susumonitor.server.common;

/**
 * Unified REST response wrapper.
 *
 * @param <T> response data type
 */
public class ApiResponse<T> {

    private final int code;

    private final String message;

    private final T data;

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
