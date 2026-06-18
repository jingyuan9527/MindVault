package com.mindvault.common.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SystemController.class)
class SystemControllerTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void health_shouldReturnUp() throws Exception {
        mockMvc.perform(get("/api/v1/system/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.app").value("mindvault"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
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
}