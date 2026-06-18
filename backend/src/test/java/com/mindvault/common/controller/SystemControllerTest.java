package com.mindvault.common.controller;

import com.mindvault.common.service.MetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SystemController.class)
class SystemControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private DataSource dataSource;
    @MockBean private MeterRegistry meterRegistry;
    @MockBean private MetricsService metricsService;

    @Test
    void health_shouldReturnUp() throws Exception {
        mockMvc.perform(get("/api/v1/system/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.app").value("mindvault"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.uptime").isNumber())
                .andExpect(jsonPath("$.checks").exists());
    }

    @Test
    void info_shouldReturnSystemInfo() throws Exception {
        mockMvc.perform(get("/api/v1/system/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.app").value("mindvault"))
                .andExpect(jsonPath("$.data.version").value("1.0.0"))
                .andExpect(jsonPath("$.data.javaVersion").isNotEmpty())
                .andExpect(jsonPath("$.data.availableProcessors").isNumber())
                .andExpect(jsonPath("$.data.uptime").isNumber());
    }

    @Test
    void metrics_shouldReturnMetrics() throws Exception {
        mockMvc.perform(get("/api/v1/system/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uptime").isNumber())
                .andExpect(jsonPath("$.data.activeConnections").isNumber());
    }
}