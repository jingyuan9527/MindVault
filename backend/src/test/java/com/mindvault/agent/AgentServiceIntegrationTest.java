package com.mindvault.agent;

import com.mindvault.agent.tool.AddKnowledgeTool;
import com.mindvault.agent.tool.SearchKnowledgeTool;
import com.mindvault.ai.client.AiModelFactory;
import com.mindvault.common.service.MetricsService;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.knowledge.SearchEnhanceService;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.systemconfig.SystemConfigService;
import com.mindvault.tokenusage.TokenUsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class AgentServiceIntegrationTest {

    @Mock private ModelConfigService modelConfigService;
    @Mock private AiModelFactory aiModelFactory;
    @Mock private MetricsService metricsService;
    @Mock private SystemConfigService config;
    @Mock private ChatModel chatModel;
    @Mock private KnowledgeService knowledgeService;
    @Mock private SearchEnhanceService searchEnhanceService;
    @Mock private TokenUsageService tokenUsageService;

    private SearchKnowledgeTool searchTool;
    private AddKnowledgeTool addTool;

    @BeforeEach
    void setUp() {
        searchTool = new SearchKnowledgeTool(knowledgeService, searchEnhanceService, config);
        addTool = new AddKnowledgeTool(knowledgeService);
        lenient().when(config.getInt(anyString(), anyInt())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getDouble(anyString(), anyDouble())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getLong(anyString(), anyLong())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getString(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getPrompt(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
        lenient().when(metricsService.startLlmCall()).thenReturn(null);
    }

    private AgentService createService() {
        AgentService s = new AgentService(modelConfigService, aiModelFactory, searchTool, addTool,
                metricsService, config, tokenUsageService);
        s.init();
        return s;
    }

    private ChatResponse chatResponse(String content) {
        return new ChatResponse(List.of(
                new Generation(new AssistantMessage(content))
        ));
    }

    @Test
    void processMessage_successfulCall() {
        ModelConfig mc = new ModelConfig();
        when(modelConfigService.getPrimaryChatModel()).thenReturn(mc);
        when(aiModelFactory.buildChatModel(any(ModelConfig.class))).thenReturn(chatModel);
        when(aiModelFactory.buildChatModel(any(ModelConfig.class), any())).thenReturn(chatModel);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("Hello! How can I help you?"));

        AgentService svc = createService();

        assertEquals("Hello! How can I help you?", svc.processMessage("Hi"));
    }

    @Test
    void processMessage_allModelsFail_returnsError() {
        when(modelConfigService.getPrimaryChatModel()).thenThrow(new RuntimeException("no model"));

        AgentService svc = createService();

        String result = svc.processMessage("Hi");
        assertTrue(result.contains("抱歉"));
    }

    @Test
    void processMessage_stream_basic() {
        ModelConfig mc = new ModelConfig();
        when(modelConfigService.getPrimaryChatModel()).thenReturn(mc);
        when(aiModelFactory.buildChatModel(any(ModelConfig.class))).thenReturn(chatModel);
        when(aiModelFactory.buildChatModel(any(ModelConfig.class), any())).thenReturn(chatModel);
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(
                chatResponse("Hel"),
                chatResponse("lo")
        ));

        AgentService svc = createService();

        StringBuilder collected = new StringBuilder();
        svc.processMessageStream("Hi", new AgentService.StreamCallback() {
            @Override public void onToken(String token) { collected.append(token); }
            @Override public void onComplete() {}
            @Override public void onError(String error) { collected.append("ERR:").append(error); }
        });

        assertEquals("Hello", collected.toString());
    }

    @Test
    void processMessageStream_noModels_returnsError() {
        when(modelConfigService.getPrimaryChatModel()).thenThrow(new RuntimeException("未配置主模型"));

        AgentService svc = createService();

        StringBuilder sb = new StringBuilder();
        svc.processMessageStream("Hi", new AgentService.StreamCallback() {
            @Override public void onToken(String token) { sb.append(token); }
            @Override public void onComplete() {}
            @Override public void onError(String error) { sb.append("ERR:").append(error); }
        });

        assertTrue(sb.toString().contains("抱歉"));
    }
}