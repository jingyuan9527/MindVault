package com.mindvault.operationlog;

import com.mindvault.operationlog.entity.OperationLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperationLogServiceTest {

    @Mock private OperationLogMapper mapper;

    private OperationLogService service;

    @Captor private ArgumentCaptor<OperationLog> logCaptor;

    @BeforeEach
    void setUp() {
        service = new OperationLogService(mapper);
    }

    @Test
    void log_shouldInsert() {
        service.log("KNOWLEDGE", "ADD", 1L, "添加知识「测试」");

        verify(mapper).insert(logCaptor.capture());
        OperationLog captured = logCaptor.getValue();
        assertEquals("KNOWLEDGE", captured.getModule());
        assertEquals("ADD", captured.getAction());
        assertEquals(1L, captured.getEntityId());
        assertEquals("添加知识「测试」", captured.getSummary());
        assertNotNull(captured.getCreatedAt());
    }

    @Test
    void log_shouldAllowNullEntityId() {
        service.log("SYSTEM", "BACKUP", null, "自动备份");

        verify(mapper).insert(logCaptor.capture());
        assertNull(logCaptor.getValue().getEntityId());
    }

    @Test
    void listByModule_shouldDelegate() {
        OperationLog log1 = new OperationLog();
        log1.setModule("KNOWLEDGE");
        log1.setAction("ADD");
        when(mapper.findByModuleOrderByCreatedAtDesc("KNOWLEDGE")).thenReturn(List.of(log1));

        List<OperationLog> logs = service.listByModule("KNOWLEDGE");

        assertEquals(1, logs.size());
        assertEquals("KNOWLEDGE", logs.get(0).getModule());
    }

    @Test
    void listAll_shouldDelegate() {
        when(mapper.selectList(null)).thenReturn(List.of(new OperationLog(), new OperationLog()));

        List<OperationLog> logs = service.listAll();

        assertEquals(2, logs.size());
    }
}