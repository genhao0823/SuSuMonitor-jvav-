package com.susumonitor.server.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts exceptions into the unified API response shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        LOGGER.warn("Business exception: {}", errorCode.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.error(errorCode));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidationException(Exception exception) {
        LOGGER.warn("Validation exception: {}", exception.getMessage());
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_PARAMETER;
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.error(errorCode));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        LOGGER.error("Unhandled exception", exception);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.error(errorCode));
    }
}
