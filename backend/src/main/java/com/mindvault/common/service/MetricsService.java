package com.mindvault.common.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Prometheus 指标收集服务
 *
 * 统一管理应用运行指标，通过 Micrometer MeterRegistry 注册到 Prometheus。
 * 指标分类：
 * - API 层面：mindvault.api.calls / mindvault.api.errors（支持 method + path + status 维度）
 * - LLM 层面：mindvault.llm.calls / mindvault.llm.errors / mindvault.llm.duration（支持 provider + model 维度）
 * - Token 层面：mindvault.llm.tokens.input / mindvault.llm.tokens.output
 * - 业务层面：mindvault.backup.count / mindvault.export.count / mindvault.import.count
 * - 系统层面：mindvault.connections.active / mindvault.circuitbreaker.open
 *
 * 各模块通过构造函数注入 MetricsService 后在关键路径调用对应 record* 方法，
 * 由 /api/v1/actuator/prometheus 端点暴露给 Prometheus 采集器。
 */
@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;

    private final Counter apiCallCounter;
    private final Counter apiErrorCounter;
    private final Counter llmCallCounter;
    private final Counter llmErrorCounter;
    private final Counter tokenInputCounter;
    private final Counter tokenOutputCounter;
    private final Counter circuitBreakerOpenCounter;
    private final Counter backupCounter;
    private final Counter exportCounter;
    private final Counter importCounter;
    private final Timer llmCallTimer;
    private final AtomicInteger activeConnections;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.apiCallCounter = Counter.builder("mindvault.api.calls")
                .description("Total API calls")
                .register(meterRegistry);
        this.apiErrorCounter = Counter.builder("mindvault.api.errors")
                .description("Total API errors")
                .register(meterRegistry);
        this.llmCallCounter = Counter.builder("mindvault.llm.calls")
                .description("Total LLM calls")
                .register(meterRegistry);
        this.llmErrorCounter = Counter.builder("mindvault.llm.errors")
                .description("Total LLM errors")
                .register(meterRegistry);
        this.tokenInputCounter = Counter.builder("mindvault.llm.tokens.input")
                .description("Total input tokens")
                .register(meterRegistry);
        this.tokenOutputCounter = Counter.builder("mindvault.llm.tokens.output")
                .description("Total output tokens")
                .register(meterRegistry);
        this.circuitBreakerOpenCounter = Counter.builder("mindvault.circuitbreaker.open")
                .description("Circuit breaker open count")
                .register(meterRegistry);
        this.backupCounter = Counter.builder("mindvault.backup.count")
                .description("Backup count")
                .register(meterRegistry);
        this.exportCounter = Counter.builder("mindvault.export.count")
                .description("Export count")
                .register(meterRegistry);
        this.importCounter = Counter.builder("mindvault.import.count")
                .description("Import count")
                .register(meterRegistry);
        this.llmCallTimer = Timer.builder("mindvault.llm.duration")
                .description("LLM call duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
        this.activeConnections = meterRegistry.gauge("mindvault.connections.active", new AtomicInteger(0));
    }

    /** 记录一次 API 调用（总计数器 + method/path/status 维度） */
    public void recordApiCall(String method, String path, int status) {
        apiCallCounter.increment();
        meterRegistry.counter("mindvault.api.calls.detail",
                "method", method, "path", path, "status", String.valueOf(status)).increment();
    }

    /** 记录一次 API 错误（总计数器 + method/path/status 维度） */
    public void recordApiError(String method, String path, int status) {
        apiErrorCounter.increment();
        meterRegistry.counter("mindvault.api.errors.detail",
                "method", method, "path", path, "status", String.valueOf(status)).increment();
    }

    /** 开始一次 LLM 调用计时（返回 Timer.Sample 用于后续 stop） */
    public Timer.Sample startLlmCall() {
        llmCallCounter.increment();
        return Timer.start(meterRegistry);
    }

    /** 记录一次成功的 LLM 调用，停止计时并记录 provider/model 维度 */
    public void recordLlmCallSuccess(Timer.Sample sample, String provider, String modelName) {
        sample.stop(llmCallTimer);
        meterRegistry.counter("mindvault.llm.calls.detail",
                "provider", provider, "model", modelName).increment();
    }

    /** 记录一次失败的 LLM 调用 */
    public void recordLlmCallError(String provider, String modelName) {
        llmErrorCounter.increment();
        meterRegistry.counter("mindvault.llm.errors.detail",
                "provider", provider, "model", modelName).increment();
    }

    /** 记录 Token 消耗量（输入 + 输出） */
    public void recordTokens(int inputTokens, int outputTokens) {
        tokenInputCounter.increment(inputTokens);
        tokenOutputCounter.increment(outputTokens);
    }

    /** 记录熔断器打开事件 */
    public void recordCircuitBreakerOpen(Long modelId) {
        circuitBreakerOpenCounter.increment();
        meterRegistry.counter("mindvault.circuitbreaker.open.detail",
                "modelId", String.valueOf(modelId)).increment();
    }

    /** 记录一次备份操作 */
    public void recordBackup() {
        backupCounter.increment();
    }

    /** 记录一次导出操作 */
    public void recordExport(String format) {
        exportCounter.increment();
        meterRegistry.counter("mindvault.export.count.detail",
                "format", format).increment();
    }

    /** 记录一次导入操作 */
    public void recordImport() {
        importCounter.increment();
    }

    /** 活跃连接数 +1（由 LoggingFilter 在请求开始时调用） */
    public void incrementActiveConnections() {
        activeConnections.incrementAndGet();
    }

    /** 活跃连接数 -1（由 LoggingFilter 在请求结束时调用） */
    public void decrementActiveConnections() {
        int val = activeConnections.decrementAndGet();
        if (val < 0) activeConnections.set(0);
    }
}