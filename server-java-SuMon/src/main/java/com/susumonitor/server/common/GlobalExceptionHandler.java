package com.susumonitor.server.common;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {
        List<String> invalidFields = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getField)
                .distinct()
                .sorted()
                .toList();
        LOGGER.warn("Request validation failed for fields: {}", invalidFields);

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_PARAMETER;
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.error(errorCode));
    }

    // 只记录约束路径，避免日志输出查询参数或路径参数的实际值。
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException exception) {
        List<String> invalidPaths = exception.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath().toString())
                .distinct()
                .sorted()
                .toList();
        LOGGER.warn("Constraint validation failed for paths: {}", invalidPaths);

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_PARAMETER;
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.error(errorCode));
    }

    /**
     * 将缺失的必填查询参数转换为统一参数错误，不记录请求参数值。
     *
     * @param exception 必填查询参数缺失异常
     * @return 统一参数错误响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException exception) {
        LOGGER.warn("Required request parameter is missing: {}", exception.getParameterName());
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_PARAMETER;
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.error(errorCode));
    }

    /**
     * 将查询参数类型或时间格式转换失败统一映射为参数错误，不记录可能敏感的原始值。
     *
     * @param exception 查询参数类型转换异常
     * @return 统一参数错误响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception) {
        LOGGER.warn("Request parameter type conversion failed: {}", exception.getName());
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_PARAMETER;
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.error(errorCode));
    }

    /**
     * 将无法解析的 JSON 请求体转换为参数错误，不记录可能包含敏感字段的原始请求内容。
     *
     * @param exception JSON 反序列化异常
     * @return 统一参数错误响应
     */
    // 捕获 JSON 语法或类型错误，避免错误进入通用 500 处理。
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception) {
        LOGGER.warn("Request body could not be parsed");
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
