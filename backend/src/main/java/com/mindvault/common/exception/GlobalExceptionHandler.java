package com.mindvault.common.exception;

import com.mindvault.common.dto.ApiResponse;
import com.mindvault.common.service.MetricsService;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理器
 *
 * 通过 @RestControllerAdvice 拦截所有 Controller 层异常，
 * 统一转换为 ApiResponse 格式返回，避免敏感信息泄漏。
 *
 * 支持的异常类型：
 * - 参数校验失败（MethodArgumentNotValidException / ConstraintViolationException）
 * - 参数错误（IllegalArgumentException / MissingServletRequestParameterException / MethodArgumentTypeMismatchException）
 * - 运行时异常（RuntimeException）→ 500
 * - 未知异常（Exception）→ 500
 *
 * 所有异常记录 TraceId，方便链路追踪。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MetricsService metricsService;

    public GlobalExceptionHandler(ObjectProvider<MetricsService> metricsServiceProvider) {
        this.metricsService = metricsServiceProvider.getIfAvailable();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(java.util.stream.Collectors.joining("; "));
        log.warn("参数校验失败 [traceId: {}]: {}", MDC.get("traceId"), errors);
        return ApiResponse.error(400, "参数校验失败: " + errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException e) {
        log.warn("约束校验失败 [traceId: {}]: {}", MDC.get("traceId"), e.getMessage());
        return ApiResponse.error(400, "参数校验失败: " + e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数错误 [traceId: {}]: {}", MDC.get("traceId"), e.getMessage());
        return ApiResponse.error(400, e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数 [traceId: {}]: {}", MDC.get("traceId"), e.getMessage());
        return ApiResponse.error(400, "缺少参数: " + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型错误 [traceId: {}]: {}", MDC.get("traceId"), e.getMessage());
        return ApiResponse.error(400, "参数 " + e.getName() + " 类型错误");
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleRuntime(RuntimeException e) {
        log.error("运行时异常 [traceId: {}]: {}", MDC.get("traceId"), e.getMessage(), e);
        if (metricsService != null) {
            metricsService.recordApiError("UNKNOWN", "runtime", 500);
        }
        return ApiResponse.error(500, "服务器内部错误");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnknown(Exception e) {
        log.error("未知异常 [traceId: {}]: {}", MDC.get("traceId"), e.getMessage(), e);
        if (metricsService != null) {
            metricsService.recordApiError("UNKNOWN", "unknown", 500);
        }
        return ApiResponse.error(500, "服务器内部错误");
    }
}
