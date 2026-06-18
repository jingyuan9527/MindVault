package com.mindvault.operationlog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.operationlog.entity.OperationLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OperationLogController.class)
class OperationLogControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private OperationLogService operationLogService;

    private OperationLog createLog(Long id) {
        OperationLog log = new OperationLog();
        log.setId(id);
        log.setModule("KNOWLEDGE");
        log.setAction("ADD");
        log.setSummary("添加知识「测试」");
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }

    @Test
    void listAll_shouldReturnLogs() throws Exception {
        when(operationLogService.listAll()).thenReturn(List.of(createLog(1L)));

        mockMvc.perform(get("/api/v1/operation-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].summary").value("添加知识「测试」"));
    }

    @Test
    void listByModule_shouldReturnFiltered() throws Exception {
        when(operationLogService.listByModule("KNOWLEDGE")).thenReturn(List.of(createLog(1L)));

        mockMvc.perform(get("/api/v1/operation-logs?module=KNOWLEDGE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].module").value("KNOWLEDGE"));
    }

    @Test
    void listByModule_shouldReturnEmptyForUnknownModule() throws Exception {
        when(operationLogService.listByModule("UNKNOWN")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/operation-logs?module=UNKNOWN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}