package com.mindvault.systemconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.systemconfig.entity.SystemConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SystemConfigController.class)
class SystemConfigControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private SystemConfigService systemConfigService;

    private SystemConfig createConfig(String key, String value, String type) {
        SystemConfig c = new SystemConfig();
        c.setId(1L);
        c.setConfigKey(key);
        c.setConfigValue(value);
        c.setValueType(type);
        c.setDescription("test config");
        c.setUpdatedAt(LocalDateTime.now());
        return c;
    }

    @Test
    void listAll_shouldReturnConfigs() throws Exception {
        when(systemConfigService.listAll()).thenReturn(List.of(createConfig("test.key", "val", "string")));

        mockMvc.perform(get("/api/v1/system-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].configKey").value("test.key"))
                .andExpect(jsonPath("$.data[0].configValue").value("val"));
    }

    @Test
    void getByKey_shouldReturnValue() throws Exception {
        when(systemConfigService.getString("test.key", null)).thenReturn("test-value");

        mockMvc.perform(get("/api/v1/system-config/test.key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("test-value"));
    }

    @Test
    void getByKey_nonExistent_shouldReturnNull() throws Exception {
        when(systemConfigService.getString("missing.key", null)).thenReturn(null);

        mockMvc.perform(get("/api/v1/system-config/missing.key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void setConfig_shouldUpsert() throws Exception {
        SystemConfig input = createConfig("my.key", "my-value", "string");

        mockMvc.perform(put("/api/v1/system-config/my.key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk());

        verify(systemConfigService).set(eq("my.key"), eq("my-value"), eq("test config"), eq("string"));
    }

    @Test
    void deleteConfig_shouldSucceed() throws Exception {
        doNothing().when(systemConfigService).delete("delete.key");

        mockMvc.perform(delete("/api/v1/system-config/delete.key"))
                .andExpect(status().isOk());

        verify(systemConfigService).delete("delete.key");
    }

    @Test
    void refreshCache_shouldSucceed() throws Exception {
        doNothing().when(systemConfigService).refreshCache();

        mockMvc.perform(post("/api/v1/system-config/refresh"))
                .andExpect(status().isOk());

        verify(systemConfigService).refreshCache();
    }
}