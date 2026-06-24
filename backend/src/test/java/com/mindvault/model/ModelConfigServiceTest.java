package com.mindvault.model;

import com.mindvault.ai.client.AiModelFactory;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.operationlog.OperationLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelConfigServiceTest {

    @Mock
    private ModelConfigMapper mapper;

    @Mock
    private OperationLogService operationLogService;

    @Mock
    private AiModelFactory aiModelFactory;

    private ModelConfigService service;

    @Captor
    private ArgumentCaptor<ModelConfig> configCaptor;

    @BeforeEach
    void setUp() {
        service = new ModelConfigService(mapper, operationLogService, aiModelFactory);
    }

    private ModelConfig createConfig(Long id, String provider, String modelName, String modelType, Boolean isPrimary) {
        ModelConfig c = new ModelConfig();
        c.setId(id);
        c.setProvider(provider);
        c.setModelName(modelName);
        c.setModelType(modelType);
        c.setIsPrimary(isPrimary);
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        return c;
    }

    @Test
    void addConfig_shouldInsertAndLog() {
        ModelConfig input = createConfig(null, "OPENAI", "gpt-4", "CHAT", false);
        when(mapper.insert(any(ModelConfig.class))).thenReturn(1);

        ModelConfig result = service.addConfig(input);

        verify(mapper).insert(configCaptor.capture());
        ModelConfig captured = configCaptor.getValue();
        assertEquals("OPENAI", captured.getProvider());
        assertEquals("gpt-4", captured.getModelName());
        assertEquals("CHAT", captured.getModelType());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(operationLogService).log(eq("MODEL"), eq("ADD"), any(), contains("gpt-4"));
    }

    @Test
    void listAll_shouldDelegateToMapper() {
        ModelConfig c1 = createConfig(1L, "OPENAI", "gpt-4", "CHAT", true);
        ModelConfig c2 = createConfig(2L, "DEEPSEEK", "deepseek-chat", "CHAT", false);
        when(mapper.selectList(null)).thenReturn(List.of(c1, c2));

        List<ModelConfig> result = service.listAll();

        assertEquals(2, result.size());
        verify(mapper).selectList(null);
    }

    @Test
    void setPrimary_shouldUnsetOldAndSetNew() {
        ModelConfig oldPrimary = createConfig(1L, "OPENAI", "gpt-4", "CHAT", true);
        ModelConfig newPrimary = createConfig(2L, "DEEPSEEK", "deepseek-chat", "CHAT", false);
        when(mapper.selectById(2L)).thenReturn(newPrimary);
        when(mapper.selectList(null)).thenReturn(List.of(oldPrimary, newPrimary));

        ModelConfig result = service.setPrimary(2L);

        assertTrue(result.getIsPrimary());
        assertFalse(oldPrimary.getIsPrimary());
        assertTrue(newPrimary.getIsPrimary());
        verify(mapper, atLeastOnce()).updateById(any(ModelConfig.class));
        verify(operationLogService).log(eq("MODEL"), eq("SET_PRIMARY"), eq(2L), anyString());
    }

    @Test
    void setPrimary_shouldThrowWhenNotFound() {
        when(mapper.selectById(99L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.setPrimary(99L));
        verify(mapper, never()).updateById(any(ModelConfig.class));
    }

    @Test
    void getPrimaryChatModel_shouldReturnModel() {
        ModelConfig primary = createConfig(1L, "OPENAI", "gpt-4", "CHAT", true);
        when(mapper.findByModelTypeAndIsPrimaryTrue("CHAT")).thenReturn(Optional.of(primary));

        ModelConfig result = service.getPrimaryChatModel();

        assertEquals("gpt-4", result.getModelName());
        verify(mapper).findByModelTypeAndIsPrimaryTrue("CHAT");
    }

    @Test
    void getPrimaryChatModel_shouldThrowWhenNotFound() {
        when(mapper.findByModelTypeAndIsPrimaryTrue("CHAT")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.getPrimaryChatModel());
    }

    @Test
    void getAvailableChatModels_shouldDelegate() {
        ModelConfig c = createConfig(1L, "OPENAI", "gpt-4", "CHAT", true);
        when(mapper.findByModelTypeAndIsEnabledTrueOrderByPriorityDesc("CHAT")).thenReturn(List.of(c));

        List<ModelConfig> result = service.getAvailableChatModels();

        assertEquals(1, result.size());
        verify(mapper).findByModelTypeAndIsEnabledTrueOrderByPriorityDesc("CHAT");
    }

    @Test
    void getAvailableEmbeddingModels_shouldDelegate() {
        ModelConfig c = createConfig(1L, "OPENAI", "text-embedding-3-small", "EMBEDDING", true);
        when(mapper.findByModelTypeAndIsEnabledTrueOrderByPriorityDesc("EMBEDDING")).thenReturn(List.of(c));

        List<ModelConfig> result = service.getAvailableEmbeddingModels();

        assertEquals(1, result.size());
        verify(mapper).findByModelTypeAndIsEnabledTrueOrderByPriorityDesc("EMBEDDING");
    }

    @Test
    void updatePriority_shouldUpdateAndLog() {
        ModelConfig config = createConfig(1L, "OPENAI", "gpt-4", "CHAT", false);
        config.setPriority(5);
        when(mapper.selectById(1L)).thenReturn(config);

        ModelConfig result = service.updatePriority(1L, 10);

        assertEquals(10, result.getPriority());
        verify(mapper).updateById(configCaptor.capture());
        assertEquals(10, configCaptor.getValue().getPriority());
        verify(operationLogService).log(eq("MODEL"), eq("UPDATE_PRIORITY"), eq(1L), contains("10"));
    }

    @Test
    void deleteConfig_shouldDeleteAndLog() {
        ModelConfig config = createConfig(1L, "OPENAI", "gpt-4", "CHAT", false);
        when(mapper.selectById(1L)).thenReturn(config);

        service.deleteConfig(1L);

        verify(mapper).deleteById(any(Long.class));
        verify(operationLogService).log(eq("MODEL"), eq("DELETE"), eq(1L), contains("gpt-4"));
    }

    @Test
    void deleteConfig_shouldThrowWhenNotFound() {
        when(mapper.selectById(99L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.deleteConfig(99L));
        verify(mapper, never()).deleteById(any(Long.class));
    }

    @Test
    void testConnection_shouldReturnFalseWhenApiKeyEmpty() {
        ModelConfig config = createConfig(1L, "OPENAI", "gpt-4", "CHAT", false);
        config.setApiKey("");
        when(mapper.selectById(1L)).thenReturn(config);

        boolean result = service.testConnection(1L);

        assertFalse(result);
    }

    @Test
    void testConnection_shouldReturnFalseWhenProviderUnsupported() {
        ModelConfig config = createConfig(1L, "UNSUPPORTED", "some-model", "CHAT", false);
        config.setApiKey("sk-test");
        when(mapper.selectById(1L)).thenReturn(config);
        when(aiModelFactory.buildChatModel(config)).thenThrow(new RuntimeException("Unsupported provider"));

        boolean result = service.testConnection(1L);

        assertFalse(result);
    }

    @Test
    void testConnection_shouldReturnTrueWhenSucceeds() {
        ModelConfig config = createConfig(1L, "OPENAI", "gpt-4", "CHAT", false);
        config.setApiKey("sk-test");
        ChatModel chatModel = mock(ChatModel.class);
        when(mapper.selectById(1L)).thenReturn(config);
        when(aiModelFactory.buildChatModel(config)).thenReturn(chatModel);
        when(chatModel.call("Hi")).thenReturn("response");

        boolean result = service.testConnection(1L);

        assertTrue(result);
        verify(operationLogService).log(eq("MODEL"), eq("TEST"), eq(1L), contains("成功"));
    }

    @Test
    void setPrimary_shouldOnlyUnsetSameTypePrimary() {
        ModelConfig chatPrimary = createConfig(1L, "OPENAI", "gpt-4", "CHAT", true);
        ModelConfig embPrimary = createConfig(2L, "OPENAI", "text-embedding-3-small", "EMBEDDING", true);
        ModelConfig newChatPrimary = createConfig(3L, "DEEPSEEK", "deepseek-chat", "CHAT", false);
        when(mapper.selectById(3L)).thenReturn(newChatPrimary);
        when(mapper.selectList(null)).thenReturn(List.of(chatPrimary, embPrimary, newChatPrimary));

        service.setPrimary(3L);

        assertFalse(chatPrimary.getIsPrimary());
        assertTrue(embPrimary.getIsPrimary());
        assertTrue(newChatPrimary.getIsPrimary());
        verify(mapper, atLeast(2)).updateById(any(ModelConfig.class));
    }
}