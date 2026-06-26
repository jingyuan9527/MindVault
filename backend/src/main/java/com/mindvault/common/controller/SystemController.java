package com.mindvault.common.controller;

import com.mindvault.common.dto.ApiResponse;
import com.mindvault.model.ModelConfigService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统监控与健康检查控制器
 *
 * 提供应用运行状态相关信息，供前端监控面板和后端 Docker 健康检查使用。
 *
 * 端点概览：
 * - GET /api/v1/system/health — 综合健康检查（DB + 磁盘 + LLM）
 * - GET /api/v1/system/info    — JVM 运行时信息（内存、线程、进程数）
 * - GET /api/v1/system/metrics — Prometheus 关键指标摘要
 *
 * 另提供 Actuator 端点（/api/v1/actuator/*）用于更细粒度的监控。
 */
@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    private final Instant startTime = Instant.now();

    @Value("${spring.application.name:mindvault}")
    private String appName;

    private final DataSource dataSource;
    private final MeterRegistry meterRegistry;
    private final ModelConfigService modelConfigService;

    public SystemController(DataSource dataSource, MeterRegistry meterRegistry, ModelConfigService modelConfigService) {
        this.dataSource = dataSource;
        this.meterRegistry = meterRegistry;
        this.modelConfigService = modelConfigService;
    }

    /** 综合健康检查：数据库连接 + 磁盘空间 + LLM 模型配置 */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        status.put("app", appName);
        status.put("timestamp", Instant.now().toString());
        status.put("uptime", Duration.between(startTime, Instant.now()).toSeconds());

        Map<String, Object> checks = new LinkedHashMap<>();
        checks.put("database", checkDatabase());
        checks.put("disk", checkDisk());
        checks.put("llm", checkLlm());
        status.put("checks", checks);

        int http200 = httpStatusCode();
        return ResponseEntity.status(http200).body(status);
    }

    /** JVM 运行时信息（应用名、版本、运行时长、内存、线程数） */
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> info() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("app", appName);
        info.put("version", "1.0.0");
        info.put("uptime", Duration.between(startTime, Instant.now()).toSeconds());
        info.put("javaVersion", runtime.getSpecVersion());
        info.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        info.put("totalMemory", Runtime.getRuntime().totalMemory() / 1024 / 1024 + "MB");
        info.put("freeMemory", Runtime.getRuntime().freeMemory() / 1024 / 1024 + "MB");
        info.put("maxMemory", Runtime.getRuntime().maxMemory() / 1024 / 1024 + "MB");
        info.put("heapUsed", memory.getHeapMemoryUsage().getUsed() / 1024 / 1024 + "MB");
        info.put("threadCount", ManagementFactory.getThreadMXBean().getThreadCount());

        return ApiResponse.success(info);
    }

    /** Prometheus 关键指标摘要（线程数、堆内存、活跃连接数、运行时长） */
    @GetMapping("/metrics")
    public ApiResponse<Map<String, Object>> metrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("jvm.threads.live", ManagementFactory.getThreadMXBean().getThreadCount());
        metrics.put("jvm.memory.used", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() / 1024 / 1024 + "MB");
        try {
            metrics.put("activeConnections", meterRegistry.get("mindvault.connections.active").gauge().value());
        } catch (Exception e) {
            metrics.put("activeConnections", 0);
        }
        metrics.put("uptime", Duration.between(startTime, Instant.now()).toSeconds());

        return ApiResponse.success(metrics);
    }

    /** 数据库健康检查：执行 SELECT 1 确认连接可用 */
    private Map<String, Object> checkDatabase() {
        Map<String, Object> db = new LinkedHashMap<>();
        db.put("name", "PostgreSQL");
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
            db.put("status", "UP");
        } catch (Exception e) {
            db.put("status", "DOWN");
            db.put("error", e.getMessage());
        }
        return db;
    }

    /** 磁盘健康检查：当前工作目录的剩余空间与总量 */
    private Map<String, Object> checkDisk() {
        Map<String, Object> disk = new LinkedHashMap<>();
        try {
            java.io.File root = new java.io.File(".");
            long free = root.getFreeSpace();
            long total = root.getTotalSpace();
            disk.put("status", "UP");
            disk.put("free", free / 1024 / 1024 + "MB");
            disk.put("total", total / 1024 / 1024 + "MB");
            disk.put("freePercent", String.format("%.1f%%", (double) free / total * 100));
        } catch (Exception e) {
            disk.put("status", "DOWN");
            disk.put("error", e.getMessage());
        }
        return disk;
    }

    /** LLM 配置检查：查询已配置的可用聊天模型，无模型时返回 NO_MODELS_CONFIGURED */
    private Map<String, Object> checkLlm() {
        Map<String, Object> llm = new LinkedHashMap<>();
        llm.put("name", "LLM");
        try {
            var models = modelConfigService.getAvailableChatModels();
            llm.put("status", models.isEmpty() ? "NO_MODELS_CONFIGURED" : "UP");
            llm.put("configuredCount", models.size());
        } catch (Exception e) {
            llm.put("status", "DOWN");
            llm.put("error", e.getMessage());
        }
        return llm;
    }

    private int httpStatusCode() {
        return org.springframework.http.HttpStatus.OK.value();
    }
}