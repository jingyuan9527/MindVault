package com.mindvault.operationlog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.common.dto.PageResult;
import com.mindvault.common.service.RequestHelper;
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
    @MockBean private RequestHelper requestHelper;

    private OperationLog createLog(Long id) {
        OperationLog log = new OperationLog();
        log.setId(id);
        log.setModule("KNOWLEDGE");
        log.setAction("新增知识");
        log.setActionType("CREATE");
        log.setSummary("新增知识「测试」");
        log.setOperator("admin");
        log.setOperatorId(1L);
        log.setResult("SUCCESS");
        log.setDurationMs(100L);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }

    @Test
    void list_shouldReturnPageResult() throws Exception {
        PageResult<OperationLog> page = new PageResult<>(List.of(createLog(1L)), 1L, 0, 20, 1);
        when(operationLogService.listPage(null, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/operation-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].summary").value("新增知识「测试」"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void listByModule_shouldReturnFiltered() throws Exception {
        PageResult<OperationLog> page = new PageResult<>(List.of(createLog(1L)), 1L, 0, 20, 1);
        when(operationLogService.listPage("KNOWLEDGE", 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/operation-logs?module=KNOWLEDGE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].module").value("KNOWLEDGE"));
    }

    @Test
    void detail_shouldReturnLog() throws Exception {
        OperationLog log = createLog(1L);
        log.setBeforeSnapshot("{\"title\":\"旧标题\"}");
        log.setAfterSnapshot("{\"title\":\"新标题\"}");
        when(operationLogService.getDetail(1L)).thenReturn(log);

        mockMvc.perform(get("/api/v1/operation-logs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.beforeSnapshot").value("{\"title\":\"旧标题\"}"))
                .andExpect(jsonPath("$.data.afterSnapshot").value("{\"title\":\"新标题\"}"));
    }

    @Test
    void detail_shouldReturn404() throws Exception {
        when(operationLogService.getDetail(999L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/operation-logs/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void list_shouldSupportPageParam() throws Exception {
        PageResult<OperationLog> page = new PageResult<>(List.of(), 0L, 1, 10, 0);
        when(operationLogService.listPage(null, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/v1/operation-logs?page=1&size=10"))
                .andExpect(status().isOk());
    }
}