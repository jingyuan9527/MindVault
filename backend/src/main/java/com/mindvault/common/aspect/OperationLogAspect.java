package com.mindvault.common.aspect;

import com.mindvault.common.annotation.OperationLog;
import com.mindvault.operationlog.OperationLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    private final OperationLogService operationLogService;

    public OperationLogAspect(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();
        String args = Arrays.stream(joinPoint.getArgs())
                .map(a -> a != null ? a.toString() : "null")
                .collect(Collectors.joining(", "));

        log.debug("→ {}({}) [traceId={}]", methodName, truncate(args, 200), MDC.get("traceId"));

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            log.debug("← {} 完成 ({}ms) [traceId={}]", methodName, duration, MDC.get("traceId"));

            try {
                String module = operationLog.module();
                String action = operationLog.action();
                String description = operationLog.description();
                Long entityId = extractEntityId(joinPoint.getArgs());
                if (!module.isEmpty() && !action.isEmpty()) {
                    operationLogService.log(module, action, entityId, description);
                }
            } catch (Exception e) {
                log.warn("记录操作日志失败: {}", e.getMessage());
            }

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("✗ {} 异常 ({}ms): {} [traceId={}]", methodName, duration, e.getMessage(), MDC.get("traceId"), e);
            throw e;
        }
    }

    private Long extractEntityId(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg instanceof Long id) return id;
            if (arg instanceof Number n) return n.longValue();
        }
        return null;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}