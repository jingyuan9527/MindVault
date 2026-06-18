package com.mindvault.common.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    public void recordApiCall(String method, String path, int status) {
        apiCallCounter.increment();
        meterRegistry.counter("mindvault.api.calls.detail",
                "method", method, "path", path, "status", String.valueOf(status)).increment();
    }

    public void recordApiError(String method, String path, int status) {
        apiErrorCounter.increment();
        meterRegistry.counter("mindvault.api.errors.detail",
                "method", method, "path", path, "status", String.valueOf(status)).increment();
    }

    public Timer.Sample startLlmCall() {
        llmCallCounter.increment();
        return Timer.start(meterRegistry);
    }

    public void recordLlmCallSuccess(Timer.Sample sample, String provider, String modelName) {
        sample.stop(llmCallTimer);
        meterRegistry.counter("mindvault.llm.calls.detail",
                "provider", provider, "model", modelName).increment();
    }

    public void recordLlmCallError(String provider, String modelName) {
        llmErrorCounter.increment();
        meterRegistry.counter("mindvault.llm.errors.detail",
                "provider", provider, "model", modelName).increment();
    }

    public void recordTokens(int inputTokens, int outputTokens) {
        tokenInputCounter.increment(inputTokens);
        tokenOutputCounter.increment(outputTokens);
    }

    public void recordCircuitBreakerOpen(Long modelId) {
        circuitBreakerOpenCounter.increment();
        meterRegistry.counter("mindvault.circuitbreaker.open.detail",
                "modelId", String.valueOf(modelId)).increment();
    }

    public void recordBackup() {
        backupCounter.increment();
    }

    public void recordExport(String format) {
        exportCounter.increment();
        meterRegistry.counter("mindvault.export.count.detail",
                "format", format).increment();
    }

    public void recordImport() {
        importCounter.increment();
    }

    public void incrementActiveConnections() {
        activeConnections.incrementAndGet();
    }

    public void decrementActiveConnections() {
        int val = activeConnections.decrementAndGet();
        if (val < 0) activeConnections.set(0);
    }
}