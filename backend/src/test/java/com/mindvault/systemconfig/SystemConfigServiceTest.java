package com.mindvault.systemconfig;

import com.mindvault.systemconfig.entity.SystemConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemConfigServiceTest {

    @Mock private SystemConfigMapper mapper;

    private SystemConfigService service;

    private SystemConfig createConfig(String key, String value, String type) {
        SystemConfig c = new SystemConfig();
        c.setId(1L);
        c.setConfigKey(key);
        c.setConfigValue(value);
        c.setValueType(type);
        c.setDescription("test");
        c.setUpdatedAt(LocalDateTime.now());
        return c;
    }

    @BeforeEach
    void setUp() {
        when(mapper.selectList(null)).thenReturn(List.of(
                createConfig("test.key", "test-value", "string"),
                createConfig("test.num", "42", "int"),
                createConfig("test.bool", "true", "bool"),
                createConfig("test.double", "3.14", "double")
        ));
        service = new SystemConfigService(mapper);
        service.refreshCache();
    }

    @Test
    void getString_shouldReturnValue() {
        assertEquals("test-value", service.getString("test.key", "default"));
    }

    @Test
    void getString_fallback_shouldReturnDefault() {
        assertEquals("default", service.getString("missing.key", "default"));
    }

    @Test
    void getInt_shouldParseValue() {
        assertEquals(42, service.getInt("test.num", 0));
    }

    @Test
    void getInt_fallback_shouldReturnDefault() {
        assertEquals(99, service.getInt("missing.key", 99));
    }

    @Test
    void getInt_invalidFormat_shouldReturnDefault() {
        assertEquals(0, service.getInt("test.key", 0));
    }

    @Test
    void getLong_shouldParseValue() {
        assertEquals(42L, service.getLong("test.num", 0L));
    }

    @Test
    void getDouble_shouldParseValue() {
        assertEquals(3.14, service.getDouble("test.double", 0.0), 0.001);
    }

    @Test
    void getBool_shouldParseTrue() {
        assertTrue(service.getBool("test.bool", false));
    }

    @Test
    void getBool_fallback_shouldReturnDefault() {
        assertTrue(service.getBool("missing.key", true));
    }

    @Test
    void getPrompt_shouldReturnString() {
        assertEquals("test-value", service.getPrompt("test.key", "default prompt"));
    }

    @Test
    void set_shouldInsertNew() {
        when(mapper.findByKey("new.key")).thenReturn(null);

        service.set("new.key", "new-value", "new config", "string");

        verify(mapper).insert(any(SystemConfig.class));
    }

    @Test
    void set_shouldUpdateExisting() {
        SystemConfig existing = createConfig("test.key", "old-value", "string");
        when(mapper.findByKey("test.key")).thenReturn(existing);

        service.set("test.key", "updated-value", "updated desc", null);

        verify(mapper).updateById(any(SystemConfig.class));
    }

    @Test
    void delete_shouldRemoveByKey() {
        SystemConfig existing = createConfig("test.key", "val", "string");
        when(mapper.findByKey("test.key")).thenReturn(existing);

        service.delete("test.key");

        verify(mapper).findByKey("test.key");
    }

    @Test
    void delete_missingKey_shouldDoNothing() {
        when(mapper.findByKey("missing.key")).thenReturn(null);

        service.delete("missing.key");

        verify(mapper, never()).deleteById(anyLong());
    }

    @Test
    void listAll_shouldReturnFromMapper() {
        List<SystemConfig> all = service.listAll();
        assertEquals(4, all.size());
    }

    @Test
    void listByPrefix_shouldFilterByPrefix() {
        List<SystemConfig> result = service.listByPrefix("test.");
        assertEquals(4, result.size());
    }

    @Test
    void listByPrefix_noMatch_shouldReturnEmpty() {
        List<SystemConfig> result = service.listByPrefix("nonexistent.");
        assertTrue(result.isEmpty());
    }

    @Test
    void refreshCache_shouldReload() {
        service.refreshCache();
        assertEquals("test-value", service.getString("test.key", null));
    }
}