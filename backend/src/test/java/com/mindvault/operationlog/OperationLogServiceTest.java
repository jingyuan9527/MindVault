package com.mindvault.operationlog;

import com.mindvault.common.dto.PageResult;
import com.mindvault.operationlog.entity.OperationLog;
import com.mindvault.operationlog.mapper.OperationLogMapper;
import com.mindvault.operationlog.service.OperationLogService;
import com.mindvault.operationlog.service.OperationLogServiceImpl;
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
        service = new OperationLogServiceImpl(mapper);
    }

    @Test
    void log_shouldInsertWithAllFields() {
        OperationLog entry = new OperationLog();
        entry.setModule("KNOWLEDGE");
        entry.setAction("新增知识");
        entry.setActionType("CREATE");
        entry.setEntityId("42");
        entry.setSummary("新增知识「测试」");
        entry.setOperator("admin");
        entry.setOperatorId(1L);
        entry.setIpAddress("127.0.0.1");
        entry.setResult("SUCCESS");
        entry.setDurationMs(150L);
        entry.setBeforeSnapshot("{\"old\":\"data\"}");
        entry.setAfterSnapshot("{\"new\":\"data\"}");

        service.log(entry);

        verify(mapper).insert(logCaptor.capture());
        OperationLog captured = logCaptor.getValue();
        assertEquals("KNOWLEDGE", captured.getModule());
        assertEquals("新增知识", captured.getAction());
        assertEquals("CREATE", captured.getActionType());
        assertEquals("42", captured.getEntityId());
        assertEquals("新增知识「测试」", captured.getSummary());
        assertEquals("admin", captured.getOperator());
        assertEquals(1L, captured.getOperatorId());
        assertEquals("127.0.0.1", captured.getIpAddress());
        assertEquals("SUCCESS", captured.getResult());
        assertEquals(150L, captured.getDurationMs());
        assertNotNull(captured.getCreatedAt());
    }

    @Test
    void log_shouldAllowNullFields() {
        OperationLog entry = new OperationLog();
        entry.setModule("SYSTEM");
        entry.setAction("BACKUP");
        entry.setEntityId((String) null);
        entry.setSummary("自动备份");

        service.log(entry);

        verify(mapper).insert(logCaptor.capture());
        assertNull(logCaptor.getValue().getEntityId());
        assertEquals("SUCCESS", logCaptor.getValue().getResult());
        assertEquals(0L, logCaptor.getValue().getDurationMs());
    }

    @Test
    void listPage_shouldDelegate() {
        OperationLog log1 = new OperationLog();
        log1.setModule("KNOWLEDGE");
        log1.setAction("ADD");
        when(mapper.listAll()).thenReturn(List.of(log1));
        when(mapper.countAll()).thenReturn(1L);

        PageResult<OperationLog> result = service.listPage(null, 0, 20);

        assertEquals(1, result.getRecords().size());
        assertEquals(1, result.getTotal());
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
        when(mapper.listAll()).thenReturn(List.of(new OperationLog(), new OperationLog()));

        List<OperationLog> logs = service.listAll();
        assertEquals(2, logs.size());
    }

    @Test
    void getDetail_shouldReturnFullLog() {
        OperationLog log = new OperationLog();
        log.setId(1L);
        log.setBeforeSnapshot("{\"old\":\"data\"}");
        log.setAfterSnapshot("{\"new\":\"data\"}");
        when(mapper.selectDetailById(1L)).thenReturn(log);

        OperationLog result = service.getDetail(1L);
        assertNotNull(result);
        assertEquals("{\"old\":\"data\"}", result.getBeforeSnapshot());
        assertEquals("{\"new\":\"data\"}", result.getAfterSnapshot());
    }
}