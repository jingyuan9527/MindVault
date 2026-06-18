package com.mindvault.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CircuitBreakerConfig {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerConfig.class);

    private final Map<Long, ModelState> modelStates = new ConcurrentHashMap<>();

    private static final int FAILURE_THRESHOLD = 3;
    private static final int COOLDOWN_SECONDS = 60;
    private static final int HALF_OPEN_MAX = 2;

    public boolean isAvailable(Long modelId) {
        ModelState state = modelStates.get(modelId);
        if (state == null) return true;

        if (state.status == Status.OPEN) {
            if (Instant.now().getEpochSecond() - state.lastFailureTime >= COOLDOWN_SECONDS) {
                state.status = Status.HALF_OPEN;
                state.halfOpenAttempts.set(0);
                log.info("熔断器半开: modelId={}", modelId);
                return true;
            }
            log.debug("熔断器闭合: modelId={}", modelId);
            return false;
        }

        if (state.status == Status.HALF_OPEN) {
            if (state.halfOpenAttempts.incrementAndGet() > HALF_OPEN_MAX) {
                return false;
            }
            return true;
        }

        return true;
    }

    public void recordSuccess(Long modelId) {
        ModelState state = modelStates.get(modelId);
        if (state != null) {
            if (state.status == Status.HALF_OPEN) {
                log.info("熔断器恢复: modelId={}", modelId);
            }
            state.failureCount.set(0);
            state.status = Status.CLOSED;
            state.halfOpenAttempts.set(0);
        }
    }

    public void recordFailure(Long modelId) {
        ModelState state = modelStates.computeIfAbsent(modelId, k -> new ModelState());
        int failures = state.failureCount.incrementAndGet();
        state.lastFailureTime = Instant.now().getEpochSecond();

        log.warn("模型调用失败: modelId={}, 连续失败次数={}/{}", modelId, failures, FAILURE_THRESHOLD);

        if (failures >= FAILURE_THRESHOLD) {
            state.status = Status.OPEN;
            log.warn("熔断器触发: modelId={}，冷却 {} 秒", modelId, COOLDOWN_SECONDS);
        }
    }

    public void reset(Long modelId) {
        modelStates.remove(modelId);
    }

    private enum Status { CLOSED, OPEN, HALF_OPEN }

    private static class ModelState {
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger halfOpenAttempts = new AtomicInteger(0);
        volatile Status status = Status.CLOSED;
        volatile long lastFailureTime = 0;
    }
}