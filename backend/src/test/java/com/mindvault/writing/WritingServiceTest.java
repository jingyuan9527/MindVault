package com.mindvault.writing;

import com.mindvault.common.service.LlmFailoverService;
import com.mindvault.knowledge.KnowledgeMapper;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.systemconfig.SystemConfigService;
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
    @Mock private LlmFailoverService llmFailoverService;
    @Mock private KnowledgeMapper knowledgeMapper;
    @Mock private SystemConfigService config;

    private WritingService service;

    @BeforeEach
    void setUp() {
        service = new WritingService(modelConfigService, llmFailoverService, knowledgeMapper, config);
        lenient().when(config.getInt(anyString(), anyInt())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getLong(anyString(), anyLong())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getDouble(anyString(), anyDouble())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getString(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getBool(anyString(), anyBoolean())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getPrompt(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
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
        when(llmFailoverService.call(anyList(), any())).thenReturn("生成的测试文章内容。");
    }
}