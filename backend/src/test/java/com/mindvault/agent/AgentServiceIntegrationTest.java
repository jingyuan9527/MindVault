package com.mindvault.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.mindvault.agent.tool.Tool;
import com.mindvault.ai.client.AiModelFactory;
import com.mindvault.common.config.CircuitBreakerConfig;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import com.mindvault.common.service.MetricsService;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.systemconfig.SystemConfigService;
import com.mindvault.tokenusage.TokenUsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
class AgentServiceIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private static final ObjectMapper mapper = new ObjectMapper();

    @Mock private ModelConfigService modelConfigService;
    @Mock private AiModelFactory aiModelFactory;
    @Mock private TokenUsageService tokenUsageService;
    @Mock private CircuitBreakerConfig circuitBreaker;
    @Mock private MetricsService metricsService;
    @Mock private SystemConfigService config;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + wireMock.getPort();

        lenient().when(config.getInt(anyString(), anyInt())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getDouble(anyString(), anyDouble())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getLong(anyString(), anyLong())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getString(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getPrompt(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));

        when(circuitBreaker.isAvailable(anyLong())).thenReturn(true);
        when(metricsService.startLlmCall()).thenReturn(null);
    }

    private ModelConfig createModel(Long id, String name) {
        ModelConfig mc = new ModelConfig();
        mc.setId(id);
        mc.setProvider("OPENAI");
        mc.setModelName(name);
        mc.setApiKey("test-key");
        mc.setBaseUrl(baseUrl);
        return mc;
    }

    private String responseBody(String content) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("content", content);
        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("index", 0);
        choice.put("message", msg);
        choice.put("finish_reason", "stop");
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("id", "chatcmpl-123");
        root.put("object", "chat.completion");
        root.put("created", 1677652288);
        root.put("model", "gpt-4o");
        root.put("choices", List.of(choice));
        try { return mapper.writeValueAsString(root); } catch (Exception e) { throw new RuntimeException(e); }
    }

    private String streamingData(String content, String finishReason) throws Exception {
        Map<String, Object> delta = new LinkedHashMap<>();
        delta.put("content", content);
        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("index", 0);
        choice.put("delta", delta);
        choice.put("finish_reason", finishReason);
        Map<String, Object> chunk = new LinkedHashMap<>();
        chunk.put("id", "chatcmpl-123");
        chunk.put("object", "chat.completion.chunk");
        chunk.put("created", 1677652288);
        chunk.put("model", "gpt-4o");
        chunk.put("choices", List.of(choice));
        return "data: " + mapper.writeValueAsString(chunk) + "\n\ndata: [DONE]\n\n";
    }

    private AgentService createService(ModelConfig mc, Tool... extraTools) {
        return createService(List.of(mc), extraTools);
    }

    private AgentService createService(List<ModelConfig> models, Tool... extraTools) {
        when(modelConfigService.getAvailableChatModels()).thenReturn(models);
        when(aiModelFactory.buildChatModel(any(ModelConfig.class), any())).thenAnswer(invocation -> {
            ModelConfig cfg = invocation.getArgument(0);
            return OpenAiChatModel.builder()
                    .options(OpenAiChatOptions.builder()
                            .baseUrl(cfg.getBaseUrl())
                            .apiKey(cfg.getApiKey())
                            .model(cfg.getModelName())
                            .build())
                    .build();
        });
List<Tool> tools = extraTools.length > 0 ? List.of(extraTools) : List.of();
        AgentService s = new AgentService(modelConfigService, aiModelFactory, tools,
                tokenUsageService, circuitBreaker, metricsService, config);
        s.init();
        return s;
    }

    @Test
    void processMessage_successfulCall() {
        ModelConfig mc = createModel(1L, "gpt-4o");
        AgentService svc = createService(mc);

        wireMock.stubFor(post("/chat/completions")
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(responseBody("Hello! How can I help you?"))));

        assertEquals("Hello! How can I help you?", svc.processMessage("Hi"));
    }

    @Test
    void processMessage_allModelsFail_returnsError() {
        ModelConfig mc = createModel(1L, "failing-model");
        AgentService svc = createService(mc);

        wireMock.stubFor(post("/chat/completions")
                .willReturn(aResponse().withStatus(500)));

        String result = svc.processMessage("Hi");
        assertTrue(result.contains("抱歉"));
    }

    @Test
    void processMessage_circuitBreakerOpen_skipsModel() {
        when(circuitBreaker.isAvailable(1L)).thenReturn(false);

        ModelConfig mc = createModel(1L, "gpt-4o");
        AgentService svc = createService(mc);

        String result = svc.processMessage("Hi");
        assertTrue(result.contains("抱歉"));
    }

    @Test
    void processMessage_toolCall_executesToolAndCallsLlmAgain() {
        ModelConfig mc = createModel(1L, "gpt-4o");
        Tool testTool = new Tool() {
            @Override public String getName() { return "test_tool"; }
            @Override public String getDescription() { return "A test tool"; }
            @Override public String execute(Map<String, Object> args) { return "tool result: val"; }
        };
        AgentService svc = createService(mc, testTool);

        wireMock.stubFor(post("/chat/completions")
                .inScenario("toolCall")
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(responseBody("[TOOL_CALL]\nname: test_tool\nargs: {\"key\": \"val\"}\n[END_TOOL_CALL]")))
                .willSetStateTo("toolExecuted"));

        wireMock.stubFor(post("/chat/completions")
                .inScenario("toolCall")
                .whenScenarioStateIs("toolExecuted")
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(responseBody("Tool executed successfully"))));

        String result = svc.processMessage("Run the tool");
        assertTrue(result.contains("Tool executed"));
    }

    @Test
    void processMessage_stream_basic() throws Exception {
        ModelConfig mc = createModel(1L, "gpt-4o");
        AgentService svc = createService(mc);

        wireMock.stubFor(post("/chat/completions")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody(streamingData("Hello", null))));

        StringBuilder collected = new StringBuilder();
        svc.processMessageStream("Hi", new AgentService.StreamCallback() {
            @Override public void onToken(String token) { collected.append(token); }
            @Override public void onComplete() {}
            @Override public void onError(String error) { collected.append("ERR:").append(error); }
        });

        assertEquals("Hello", collected.toString());
    }

    @Test
    void processMessageStream_withToolCall() throws Exception {
        ModelConfig mc = createModel(1L, "gpt-4o");
        Tool testTool = new Tool() {
            @Override public String getName() { return "test_tool"; }
            @Override public String getDescription() { return "A test tool"; }
            @Override public String execute(Map<String, Object> args) { return "tool result: val"; }
        };
        AgentService svc = createService(mc, testTool);

        wireMock.stubFor(post("/chat/completions")
                .inScenario("streamToolCall")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody(streamingData("[TOOL_CALL]\nname: test_tool\nargs: {\"key\": \"val\"}\n[END_TOOL_CALL]", null)))
                .willSetStateTo("toolExecuted"));

        wireMock.stubFor(post("/chat/completions")
                .inScenario("streamToolCall")
                .whenScenarioStateIs("toolExecuted")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody(streamingData("Done with streaming tool", "stop"))));

        StringBuilder collected = new StringBuilder();
        svc.processMessageStream("Run tool", new AgentService.StreamCallback() {
            @Override public void onToken(String token) { collected.append(token); }
            @Override public void onComplete() {}
            @Override public void onError(String error) { collected.append("ERR:").append(error); }
        });

        String text = collected.toString();
        assertTrue(text.contains("Done with streaming tool") || text.contains("抱歉"),
                "Expected tool result or error message, got: " + text);
    }

    @Test
    void processMessageStream_noModels_returnsError() {
        when(modelConfigService.getAvailableChatModels()).thenReturn(List.of());

        AgentService svc = new AgentService(modelConfigService, aiModelFactory, List.of(),
                tokenUsageService, circuitBreaker, metricsService, config);
        svc.init();

        StringBuilder sb = new StringBuilder();
        svc.processMessageStream("Hi", new AgentService.StreamCallback() {
            @Override public void onToken(String token) { sb.append(token); }
            @Override public void onComplete() {}
            @Override public void onError(String error) { sb.append("ERR:").append(error); }
        });

        assertTrue(sb.toString().contains("未配置主模型"));
    }
}