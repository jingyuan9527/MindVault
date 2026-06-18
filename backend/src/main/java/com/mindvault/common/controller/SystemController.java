package com.mindvault.common.controller;

import com.mindvault.common.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    private final Instant startTime = Instant.now();

    @Value("${spring.application.name:mindvault}")
    private String appName;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        status.put("app", appName);
        status.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(status);
    }

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
}