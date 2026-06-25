package com.mindvault.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.auth.config.UserContext;
import com.mindvault.common.annotation.OperationLog;
import com.mindvault.common.service.SnapshotProvider;
import com.mindvault.common.service.RequestHelper;
import com.mindvault.operationlog.OperationLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    private final OperationLogService operationLogService;
    private final SnapshotProvider snapshotProvider;
    private final RequestHelper requestHelper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OperationLogAspect(OperationLogService operationLogService,
                              SnapshotProvider snapshotProvider,
                              RequestHelper requestHelper) {
        this.operationLogService = operationLogService;
        this.snapshotProvider = snapshotProvider;
        this.requestHelper = requestHelper;
    }

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();
        String args = Arrays.stream(joinPoint.getArgs())
                .map(a -> a != null ? a.toString() : "null")
                .collect(Collectors.joining(", "));

        log.debug("→ {}({}) [traceId={}]", methodName, truncate(args, 200), MDC.get("traceId"));

        String module = operationLog.module();
        String action = operationLog.action();
        String actionType = operationLog.actionType();
        Class<?> entityType = operationLog.entityType();
        String entityId = extractEntityId(joinPoint.getArgs());
        String summary = resolveSummary(operationLog.summary(), joinPoint);

        String beforeSnapshot = null;
        if ("UPDATE".equals(actionType) || "DELETE".equals(actionType)) {
            beforeSnapshot = snapshotProvider.getSnapshot(entityType, entityId);
        }

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("✗ {} 异常 ({}ms): {} [traceId={}]", methodName, duration, e.getMessage(), MDC.get("traceId"), e);

            recordLog(module, action, actionType, entityId, summary, beforeSnapshot,
                    null, "FAILURE", e.getMessage(), duration, joinPoint);
            throw e;
        }

        long duration = System.currentTimeMillis() - start;
        log.debug("← {} 完成 ({}ms) [traceId={}]", methodName, duration, MDC.get("traceId"));

        String afterSnapshot = null;
        if ("CREATE".equals(actionType) || "UPDATE".equals(actionType)) {
            if ("UPDATE".equals(actionType) && result != null) {
                afterSnapshot = toJson(result);
            } else if ("CREATE".equals(actionType) && result != null) {
                afterSnapshot = toJson(result);
                String newId = extractEntityIdFromResult(result);
                if (newId != null) entityId = newId;
            }
        }

        recordLog(module, action, actionType, entityId, summary, beforeSnapshot,
                afterSnapshot, "SUCCESS", null, duration, joinPoint);

        return result;
    }

    private void recordLog(String module, String action, String actionType, String entityId,
                           String summary, String beforeSnapshot, String afterSnapshot,
                           String result, String errorMessage, long duration,
                           ProceedingJoinPoint joinPoint) {
        try {
            if (module.isEmpty() || action.isEmpty()) return;

            UserContext.UserInfo user = UserContext.get();
            String operator = user != null ? user.username() : "system";
            Long operatorId = user != null ? user.userId() : null;
            String ip = requestHelper.getClientIp();
            String remark = user != null && "api".equals(user.role()) ? "API Token" : null;

            com.mindvault.operationlog.entity.OperationLog logEntry =
                    new com.mindvault.operationlog.entity.OperationLog();
            logEntry.setModule(module);
            logEntry.setAction(action);
            logEntry.setActionType(actionType);
            logEntry.setEntityId(entityId);
            logEntry.setSummary(summary);
            logEntry.setBeforeSnapshot(beforeSnapshot);
            logEntry.setAfterSnapshot(afterSnapshot);
            logEntry.setOperator(operator);
            logEntry.setOperatorId(operatorId);
            logEntry.setIpAddress(ip);
            logEntry.setResult(result);
            logEntry.setErrorMessage(errorMessage);
            logEntry.setDurationMs(duration);
            logEntry.setRemark(remark);
            logEntry.setCreatedAt(LocalDateTime.now());

            operationLogService.log(logEntry);
        } catch (Exception e) {
            log.warn("记录操作日志失败: {}", e.getMessage());
        }
    }

    private String extractEntityId(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg instanceof Long id) return String.valueOf(id);
            if (arg instanceof String s && s.matches("\\d+")) return s;
            if (arg instanceof Number n) return String.valueOf(n.longValue());
        }
        return null;
    }

    private String extractEntityIdFromResult(Object result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            var tree = objectMapper.readTree(json);
            var idNode = tree.get("id");
            if (idNode != null && !idNode.isNull()) {
                return idNode.asText();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String resolveSummary(String summary, ProceedingJoinPoint joinPoint) {
        if (!summary.isEmpty()) return summary;
        OperationLog ann = ((MethodSignature) joinPoint.getSignature()).getMethod()
                .getAnnotation(OperationLog.class);
        if (ann != null) {
            String action = ann.action();
            if (!action.isEmpty()) return action;
        }
        return joinPoint.getSignature().getName();
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("对象序列化失败: {}", e.getMessage());
            return null;
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}