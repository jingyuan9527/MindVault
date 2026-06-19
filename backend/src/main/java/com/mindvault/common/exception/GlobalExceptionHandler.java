package com.mindvault.common.exception;

import com.mindvault.common.dto.ApiResponse;
import com.mindvault.common.service.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MetricsService metricsService;

    public GlobalExceptionHandler(ObjectProvider<MetricsService> metricsServiceProvider) {
        this.metricsService = metricsServiceProvider.getIfAvailable();
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