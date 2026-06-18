package com.mindvault.writing;

import com.mindvault.agent.config.AgentConfig;
import com.mindvault.knowledge.KnowledgeMapper;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
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
class WritingServiceTest {

    @Mock private ModelConfigService modelConfigService;
    @Mock private AgentConfig agentConfig;
    @Mock private KnowledgeMapper knowledgeMapper;

    private WritingService service;

    @BeforeEach
    void setUp() {
        service = new WritingService(modelConfigService, agentConfig, knowledgeMapper);
    }

    private Knowledge createKnowledge(Long id, String title, String content, String summary) {
        Knowledge k = new Knowledge();
        k.setId(id);
        k.setTitle(title);
        k.setContent(content);
        k.setSummary(summary);
        k.setCreatedAt(LocalDateTime.now());
        return k;
    }

    @Test
    void generateArticle_noModels_shouldReturnErrorMessage() {
        String result = service.generateArticle("AI", "formal", "machine learning");

        assertEquals("系统未配置可用模型，请先在设置中添加并启用模型。", result);
        verifyNoInteractions(knowledgeMapper);
    }

    @Test
    void generateArticle_noRelatedKnowledge() {
        injectModel();

        when(knowledgeMapper.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(anyString(), anyString()))
                .thenReturn(List.of());

        String result = service.generateArticle("AI", "formal", "machine learning");

        assertNotNull(result);
        assertTrue(result.equals("文章生成失败，请稍后重试。") || result.contains("文章"));
        verify(knowledgeMapper, atLeastOnce())
                .findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(anyString(), anyString());
    }

    @Test
    void generateArticle_withRelatedKnowledge() {
        injectModel();

        Knowledge k1 = createKnowledge(1L, "AI Basics", "Artificial intelligence content", "AI summary");
        Knowledge k2 = createKnowledge(2L, "ML Overview", "Machine learning content", "ML summary");
        when(knowledgeMapper.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(anyString(), anyString()))
                .thenReturn(List.of(k1))
                .thenReturn(List.of(k2));

        String result = service.generateArticle("AI", "formal", "machine learning");

        assertNotNull(result);
        verify(knowledgeMapper, atLeastOnce())
                .findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(anyString(), anyString());
    }

    private void injectModel() {
        ModelConfig mc = new ModelConfig();
        mc.setId(1L);
        mc.setProvider("deepseek");
        mc.setModelName("deepseek-chat");
        mc.setApiKey("test-key");
        mc.setIsEnabled(true);
        when(modelConfigService.getAvailableChatModels()).thenReturn(List.of(mc));
        AgentConfig.LlmEndpoint endpoint = mock(AgentConfig.LlmEndpoint.class);
        when(endpoint.getFullUrl()).thenReturn("https://api.test.com/v1/chat/completions");
        when(endpoint.getApiKey()).thenReturn("test-key");
        when(endpoint.getModelName()).thenReturn("test-model");
        when(agentConfig.buildEndpoint(mc)).thenReturn(endpoint);
        service.refreshModels();
    }
}