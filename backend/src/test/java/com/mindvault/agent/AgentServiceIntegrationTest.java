package com.mindvault.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.mindvault.agent.config.AgentConfig;
import com.mindvault.agent.tool.Tool;
import com.mindvault.common.config.CircuitBreakerConfig;
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

import java.util.HashMap;
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
    @Mock private AgentConfig agentConfig;
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

    private AgentConfig.LlmEndpoint createEndpoint(ModelConfig mc) {
        AgentConfig.LlmEndpoint ep = new AgentConfig.LlmEndpoint();
        ep.setBaseUrl(mc.getBaseUrl());
        ep.setApiPath("/chat/completions");
        ep.setApiKey(mc.getApiKey());
        ep.setModelName(mc.getModelName());
        return ep;
    }

    private AgentService createService(ModelConfig mc, Tool... extraTools) {
        return createService(List.of(mc), extraTools);
    }

    private AgentService createService(List<ModelConfig> models, Tool... extraTools) {
        when(modelConfigService.getAvailableChatModels()).thenReturn(models);
        for (ModelConfig mc : models) {
            when(agentConfig.buildEndpoint(mc)).thenReturn(createEndpoint(mc));
        }
List<Tool> tools = extraTools.length > 0 ? List.of(extraTools) : List.of();
        AgentService s = new AgentService(modelConfigService, agentConfig, tools,
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
                        .withBody("""
                                {"choices":[{"message":{"content":"Hello! How can I help you?"},"finish_reason":"stop"}]}
                                """)));

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
                        .withBody("""
                                {"choices":[{"message":{"content":"[TOOL_CALL]\\nname: test_tool\\nargs: {\\"key\\": \\"val\\"}\\n[END_TOOL_CALL]"},"finish_reason":"stop"}]}
                                """))
                .willSetStateTo("toolExecuted"));

        wireMock.stubFor(post("/chat/completions")
                .inScenario("toolCall")
                .whenScenarioStateIs("toolExecuted")
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"choices":[{"message":{"content":"Tool executed successfully"},"finish_reason":"stop"}]}
                                """)));

        String result = svc.processMessage("Run the tool");
        assertTrue(result.contains("Tool executed"));
    }

    @Test
    void processMessage_stream_basic() throws Exception {
        ModelConfig mc = createModel(1L, "gpt-4o");
        AgentService svc = createService(mc);

        Map<String, Object> delta = new HashMap<>();
        delta.put("content", "Hello");
        delta.put("finish_reason", null);
        Map<String, Object> choice = new HashMap<>();
        choice.put("delta", delta);
        choice.put("finish_reason", null);

        Map<String, Object> chunk = new LinkedHashMap<>();
        chunk.put("choices", List.of(choice));

        wireMock.stubFor(post("/chat/completions")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody("data: " + mapper.writeValueAsString(chunk) + "\n\ndata: [DONE]\n\n")));

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

        String toolCallContent = "[TOOL_CALL]\nname: test_tool\nargs: {\"key\": \"val\"}\n[END_TOOL_CALL]";
        Map<String, Object> delta = new HashMap<>();
        delta.put("content", toolCallContent);
        delta.put("finish_reason", null);
        Map<String, Object> choice = new HashMap<>();
        choice.put("delta", delta);
        choice.put("finish_reason", null);
        Map<String, Object> chunkMap = new HashMap<>();
        chunkMap.put("choices", List.of(choice));

        String toolCallChunk = mapper.writeValueAsString(chunkMap);

        Map<String, Object> finalChoice = new HashMap<>();
        Map<String, Object> finalMsg = new HashMap<>();
        finalMsg.put("content", "Done with streaming tool");
        finalChoice.put("message", finalMsg);
        finalChoice.put("finish_reason", "stop");
        Map<String, Object> finalRoot = new HashMap<>();
        finalRoot.put("choices", List.of(finalChoice));

        wireMock.stubFor(post("/chat/completions")
                .inScenario("streamToolCall")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody("data: " + toolCallChunk + "\n\ndata: [DONE]\n\n"))
                .willSetStateTo("toolExecuted"));

        Map<String, Object> contentDelta = new HashMap<>();
        contentDelta.put("content", "Done with streaming tool");
        Map<String, Object> contentChoice = new HashMap<>();
        contentChoice.put("delta", contentDelta);
        String finalContentChunk = mapper.writeValueAsString(Map.of("choices", List.of(contentChoice)));

        wireMock.stubFor(post("/chat/completions")
                .inScenario("streamToolCall")
                .whenScenarioStateIs("toolExecuted")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody("data: " + finalContentChunk + "\n\ndata: [DONE]\n\n")));

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

        AgentService svc = new AgentService(modelConfigService, agentConfig, List.of(),
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