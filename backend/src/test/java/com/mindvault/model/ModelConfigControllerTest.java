package com.mindvault.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.model.entity.ModelConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ModelConfigController.class)
class ModelConfigControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ModelConfigService modelConfigService;

    private ModelConfig createConfig(Long id) {
        ModelConfig c = new ModelConfig();
        c.setId(id);
        c.setProvider("OPENAI");
        c.setModelName("gpt-4o");
        c.setModelType("CHAT");
        c.setIsPrimary(true);
        c.setIsEnabled(true);
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        return c;
    }

    @Test
    void addConfig_shouldReturnCreated() throws Exception {
        ModelConfig input = createConfig(null);
        when(modelConfigService.addConfig(any())).thenReturn(input);

        mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("OPENAI"));
    }

    @Test
    void listAll_shouldReturnConfigs() throws Exception {
        when(modelConfigService.listAll()).thenReturn(List.of(createConfig(1L)));

        mockMvc.perform(get("/api/v1/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].modelName").value("gpt-4o"));
    }

    @Test
    void setPrimary_shouldUpdate() throws Exception {
        when(modelConfigService.setPrimary(1L)).thenReturn(createConfig(1L));

        mockMvc.perform(patch("/api/v1/models/1/primary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPrimary").value(true));
    }

    @Test
    void updatePriority_shouldUpdate() throws Exception {
        when(modelConfigService.updatePriority(1L, 10)).thenReturn(createConfig(1L));

        mockMvc.perform(patch("/api/v1/models/1/priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("OPENAI"));
    }

    @Test
    void deleteConfig_shouldSucceed() throws Exception {
        doNothing().when(modelConfigService).deleteConfig(1L);

        mockMvc.perform(delete("/api/v1/models/1"))
                .andExpect(status().isOk());
    }

    @Test
    void fetchModels_shouldReturnList() throws Exception {
        when(modelConfigService.fetchAvailableModels("OPENAI", "sk-xxx", null))
                .thenReturn(List.of("gpt-4o", "gpt-3.5-turbo"));

        mockMvc.perform(post("/api/v1/models/fetch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\": \"OPENAI\", \"apiKey\": \"sk-xxx\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("gpt-4o"));
    }

    @Test
    void testConnection_shouldReturnTrue() throws Exception {
        when(modelConfigService.testConnection(1L)).thenReturn(true);

        mockMvc.perform(post("/api/v1/models/1/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void testConnection_shouldReturnErrorOnFailure() throws Exception {
        when(modelConfigService.testConnection(1L)).thenReturn(false);

        mockMvc.perform(post("/api/v1/models/1/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("连接测试失败，请检查 API Key 等配置"));
    }
}