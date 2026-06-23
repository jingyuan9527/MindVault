package com.mindvault.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.agent.config.AgentConfig;
import com.mindvault.agent.config.AgentConfig.LlmEndpoint;
import com.mindvault.agent.tool.Tool;
import com.mindvault.common.config.CircuitBreakerConfig;
import com.mindvault.common.service.MetricsService;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.systemconfig.SystemConfigService;
import com.mindvault.tokenusage.TokenUsageService;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final ModelConfigService modelConfigService;
    private final AgentConfig agentConfig;
    private final List<Tool> tools;
    private final TokenUsageService tokenUsageService;
    private final CircuitBreakerConfig circuitBreaker;
    private final MetricsService metricsService;
    private final SystemConfigService config;
    private final ObjectMapper objectMapper;

    private volatile List<ModelEndpoint> modelEndpoints = List.of();
    private String systemPrompt;
    private double tokenEstimateRatio;

    public AgentService(ModelConfigService modelConfigService,
                        AgentConfig agentConfig,
                        List<Tool> tools,
                        TokenUsageService tokenUsageService,
                        CircuitBreakerConfig circuitBreaker,
                        MetricsService metricsService,
                        SystemConfigService config) {
        this.modelConfigService = modelConfigService;
        this.agentConfig = agentConfig;
        this.tools = tools;
        this.tokenUsageService = tokenUsageService;
        this.circuitBreaker = circuitBreaker;
        this.metricsService = metricsService;
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        refreshModels();
        systemPrompt = buildSystemPrompt();
        tokenEstimateRatio = config.getDouble("threshold.agent.token-estimate-ratio", 3.0);
    }

    public void refreshModels() {
        try {
            List<ModelConfig> models = modelConfigService.getAvailableChatModels();
            modelEndpoints = models.stream()
                    .map(mc -> new ModelEndpoint(mc.getId(), mc.getProvider(), agentConfig.buildEndpoint(mc)))
                    .toList();
            if (!modelEndpoints.isEmpty()) {
                ModelEndpoint primary = modelEndpoints.get(0);
                log.info("AgentService 初始化完成，主模型: {}/{}, 备用模型数: {}",
                        primary.endpoint.getModelName(), primary.endpoint.getBaseUrl(),
                        modelEndpoints.size() - 1);
            } else {
                log.warn("AgentService 初始化：无可用模型");
            }
        } catch (Exception e) {
            log.warn("AgentService 初始化失败: {}", e.getMessage());
        }
    }

    private String buildSystemPrompt() {
        StringBuilder toolList = new StringBuilder();
        for (Tool tool : tools) {
            toolList.append("- ").append(tool.getName()).append(": ").append(tool.getDescription()).append("\n");
        }
        String promptTmpl = config.getPrompt("prompt.agent.system-prompt",
                "你是 MindVault（知忆）AI 助手，一个个人知识库 Agent。\n\n你可以使用以下工具来帮助用户管理知识库：\n%s\n当你需要使用工具时，请按以下格式返回：\n[TOOL_CALL]\nname: 工具名称\nargs: {\"key\": \"value\"}\n[END_TOOL_CALL]\n\n请用中文回复用户。");
        return String.format(promptTmpl, toolList.toString());
    }

    public String processMessage(String userMessage) {
        if (modelEndpoints.isEmpty()) {
            return config.getString("default.agent.no-model-message", "系统未配置主模型，请先在设置中添加并设置主模型。");
        }

        try {
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMsg = new LinkedHashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);

            Map<String, String> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            String response = callLlmWithFailover(messages);
            String toolResult = executeToolCall(response);

            if (toolResult != null) {
                String cleanResponse = stripToolCallMarkers(response);
                Map<String, String> assistantMsg = new LinkedHashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", cleanResponse);
                messages.add(assistantMsg);

                Map<String, String> toolMsg = new LinkedHashMap<>();
                toolMsg.put("role", "user");
                toolMsg.put("content", "工具执行结果: " + toolResult);
                messages.add(toolMsg);

                return callLlmWithFailover(messages);
            }

            return response;
        } catch (Exception e) {
            log.error("Agent 处理消息失败: {}", e.getMessage(), e);
            return config.getString("default.agent.error-message", "抱歉，处理您的消息时遇到了问题，请稍后重试。");
        }
    }

    public void processMessageStream(String userMessage, StreamCallback callback) {
        if (modelEndpoints.isEmpty()) {
            callback.onError(config.getString("default.agent.no-model-message", "系统未配置主模型，请先在设置中添加并设置主模型。"));
            return;
        }

        try {
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userMessage));

            StringBuilder fullResponse = new StringBuilder();
            streamFromLlm(messages, callback, fullResponse);

            ToolCallInfo toolCall = extractToolCall(fullResponse.toString());
            if (toolCall != null) {
                callback.onToolCall(toolCall.name, toolCall.argsJson);

                String result = executeToolByName(toolCall.name, toolCall.argsJson);

                callback.onToolResult(result);

                String cleanResponse = stripToolCallMarkers(fullResponse.toString());
                messages.add(Map.of("role", "assistant", "content", cleanResponse));
                messages.add(Map.of("role", "user", "content", "工具执行结果: " + result));
                streamFromLlm(messages, callback, new StringBuilder());
            }

            callback.onComplete();
        } catch (Exception e) {
            log.error("Agent 流式处理失败: {}", e.getMessage(), e);
            callback.onError(config.getString("default.agent.error-message", "抱歉，处理您的消息时遇到了问题，请稍后重试。"));
        }
    }

    private void streamFromLlm(List<Map<String, String>> messages, StreamCallback callback, StringBuilder collector) {
        List<String> errors = new ArrayList<>();
        for (ModelEndpoint me : modelEndpoints) {
            if (!circuitBreaker.isAvailable(me.modelId)) {
                errors.add("模型" + me.modelId + " 熔断中");
                continue;
            }
            Timer.Sample sample = metricsService.startLlmCall();
            try {
                JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
                factory.setReadTimeout(config.getInt("threshold.agent.stream-read-timeout-ms", 120_000));

                RestClient client = RestClient.builder()
                        .baseUrl(me.endpoint.getFullUrl())
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .defaultHeader("Authorization", "Bearer " + me.endpoint.getApiKey())
                        .requestFactory(factory)
                        .build();

                Map<String, Object> requestBody = new LinkedHashMap<>();
                requestBody.put("model", me.endpoint.getModelName());
                requestBody.put("messages", messages);
                requestBody.put("temperature", config.getDouble("threshold.agent.default-temperature", 0.7));
                requestBody.put("stream", true);

                String jsonBody = objectMapper.writeValueAsString(requestBody);

                String promptText = objectMapper.writeValueAsString(messages);
                int promptTokens = (int) (promptText.length() / tokenEstimateRatio);

                boolean isOllama = me.endpoint.getFullUrl().contains("ollama") || me.endpoint.getFullUrl().contains("11434");

                client.post()
                        .body(jsonBody)
                        .exchange((request, response) -> {
                            InputStream body = response.getBody();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8));
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.isEmpty()) continue;
                                String content = parseStreamLine(line, isOllama);
                                if (content != null) {
                                    callback.onToken(content);
                                    collector.append(content);
                                }
                            }
                            return null;
                        });

                circuitBreaker.recordSuccess(me.modelId);
                metricsService.recordLlmCallSuccess(sample, me.provider, me.endpoint.getModelName());
                recordStreamUsage(me, promptTokens, collector.length());
                return;
            } catch (Exception e) {
                circuitBreaker.recordFailure(me.modelId);
                metricsService.recordLlmCallError(me.provider, me.endpoint.getModelName());
                log.warn("模型 id={} 流式调用失败: {}", me.modelId, e.getMessage());
                errors.add("模型" + me.modelId + ": " + e.getMessage());
            }
        }
        log.error("所有模型流式调用均失败: {}", String.join("; ", errors));
        throw new RuntimeException("所有模型流式调用均失败");
    }

    private void recordStreamUsage(ModelEndpoint me, int promptTokens, int completionChars) {
        try {
            int completionTokens = (int) (completionChars / tokenEstimateRatio);
            if (completionTokens == 0) return;
            metricsService.recordTokens(promptTokens, completionTokens);
            ModelConfig mc = new ModelConfig();
            mc.setId(me.modelId);
            mc.setProvider(me.provider);
            mc.setModelName(me.endpoint.getModelName());
            mc.setModelType("CHAT");
            tokenUsageService.recordUsage(mc, promptTokens, completionTokens, "CHAT_STREAM", null);
        } catch (Exception e) {
            log.warn("记录流式 Token 用量失败: {}", e.getMessage());
        }
    }

    private String parseStreamLine(String line, boolean isOllama) {
        if (isOllama) {
            try {
                Map<?, ?> data = objectMapper.readValue(line, Map.class);
                if (data.containsKey("message")) {
                    Map<?, ?> msg = (Map<?, ?>) data.get("message");
                    Object content = msg.get("content");
                    if (content instanceof String s && !s.isEmpty()) return s;
                }
                if (data.containsKey("done") && Boolean.TRUE.equals(data.get("done"))) return null;
            } catch (Exception e) {
                log.debug("Ollama 流式解析行失败: {}", e.getMessage());
            }
            return null;
        }

        if (!line.startsWith("data:")) return null;
        String data = line.substring(5).trim();
        if (data.equals("[DONE]")) return null;

        try {
            Map<?, ?> json = objectMapper.readValue(data, Map.class);
            List<?> choices = (List<?>) json.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                Object finishReason = choice.get("finish_reason");
                if (finishReason instanceof String s && !s.isBlank()) return null;
                Map<?, ?> delta = (Map<?, ?>) choice.get("delta");
                if (delta != null && delta.get("content") instanceof String s && !s.isEmpty()) return s;
            }
        } catch (Exception e) {
            log.debug("SSE 流式解析行失败: {}", e.getMessage());
        }
        return null;
    }

    private String callLlmWithFailover(List<Map<String, String>> messages) {
        return callLlmWithRetry(messages, 0);
    }

    private String callLlmWithRetry(List<Map<String, String>> messages, int retryCount) {
        List<String> errors = new ArrayList<>();
        for (ModelEndpoint me : modelEndpoints) {
            if (!circuitBreaker.isAvailable(me.modelId)) {
                errors.add("模型" + me.modelId + " 熔断中");
                continue;
            }

            Timer.Sample sample = metricsService.startLlmCall();
            try {
                RestClient client = RestClient.builder()
                        .baseUrl(me.endpoint.getFullUrl())
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .defaultHeader("Authorization", "Bearer " + me.endpoint.getApiKey())
                        .build();

                Map<String, Object> requestBody = new LinkedHashMap<>();
                requestBody.put("model", me.endpoint.getModelName());
                requestBody.put("messages", messages);
                requestBody.put("temperature", config.getDouble("threshold.agent.default-temperature", 0.7));

                String jsonBody = objectMapper.writeValueAsString(requestBody);
                String responseJson = client.post()
                        .body(jsonBody)
                        .retrieve()
                        .body(String.class);

                Map<?, ?> responseMap = objectMapper.readValue(responseJson, Map.class);
                String content = extractContent(responseMap);
                circuitBreaker.recordSuccess(me.modelId);
                metricsService.recordLlmCallSuccess(sample, me.provider, me.endpoint.getModelName());
                recordUsage(me, responseMap);
                if (content != null) return content;

                return responseJson;
            } catch (Exception e) {
                circuitBreaker.recordFailure(me.modelId);
                metricsService.recordLlmCallError(me.provider, me.endpoint.getModelName());
                log.warn("模型 id={} 调用失败: {}", me.modelId, e.getMessage());
                errors.add("模型" + me.modelId + ": " + e.getMessage());
            }
        }

        int maxRetries = config.getInt("threshold.agent.max-retries", 2);
        if (retryCount < maxRetries) {
            long retryDelayMs = config.getLong("threshold.agent.retry-delay-ms", 2000);
            log.warn("所有模型失败，{} 秒后重试 ({}/{})", (retryCount + 1) * retryDelayMs / 1000, retryCount + 1, maxRetries);
            try {
                Thread.sleep((retryCount + 1L) * retryDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("线程被中断", e);
            }
            return callLlmWithRetry(messages, retryCount + 1);
        }

        log.error("所有模型均调用失败 (已重试): {}", String.join("; ", errors));
        throw new RuntimeException("所有模型均调用失败");
    }

    @SuppressWarnings("unchecked")
    private void recordUsage(ModelEndpoint me, Map<?, ?> responseMap) {
        try {
            Map<String, Object> usage = (Map<String, Object>) responseMap.get("usage");
            if (usage != null) {
                int promptTokens = ((Number) usage.getOrDefault("prompt_tokens", 0)).intValue();
                int completionTokens = ((Number) usage.getOrDefault("completion_tokens", 0)).intValue();
                metricsService.recordTokens(promptTokens, completionTokens);
                ModelConfig mc = new ModelConfig();
                mc.setId(me.modelId);
                mc.setProvider(me.provider);
                mc.setModelName(me.endpoint.getModelName());
                mc.setModelType("CHAT");
                tokenUsageService.recordUsage(mc, promptTokens, completionTokens, "CHAT", null);
            }
        } catch (Exception e) {
            log.warn("记录 Token 用量失败: {}", e.getMessage());
        }
    }

    private String extractContent(Map<?, ?> responseMap) {
        if (responseMap.containsKey("choices")) {
            List<?> choices = (List<?>) responseMap.get("choices");
            if (!choices.isEmpty()) {
                Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                Map<?, ?> message = (Map<?, ?>) choice.get("message");
                if (message != null && message.get("content") instanceof String s) return s;
                if (choice.get("text") instanceof String s) return s;
            }
        }
        if (responseMap.containsKey("message")) {
            Map<?, ?> message = (Map<?, ?>) responseMap.get("message");
            if (message.get("content") instanceof String s) return s;
        }
        if (responseMap.containsKey("response")) {
            Object resp = responseMap.get("response");
            if (resp instanceof String s) return s;
        }
        return null;
    }

    private String executeToolCall(String response) {
        Pattern pattern = Pattern.compile(
                "\\[TOOL_CALL\\]\\s*name:\\s*(\\S+)\\s*args:\\s*(\\{.*?\\})\\s*\\[END_TOOL_CALL\\]",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);

        if (matcher.find()) {
            String toolName = matcher.group(1).trim();
            String argsJson = matcher.group(2).trim();

            for (Tool tool : tools) {
                if (tool.getName().equals(toolName)) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> args = objectMapper.readValue(argsJson, Map.class);
                        return tool.execute(args);
                    } catch (Exception e) {
                        log.error("执行工具 {} 失败: {}", toolName, e.getMessage());
                        return "工具执行失败: " + e.getMessage();
                    }
                }
            }
            return "未知工具: " + toolName;
        }

        return null;
    }

    public String stripToolCallMarkers(String text) {
        return text.replaceAll("\\[TOOL_CALL\\].*?\\[END_TOOL_CALL\\]", "").trim();
    }

    public ToolCallInfo extractToolCall(String response) {
        Pattern pattern = Pattern.compile(
                "\\[TOOL_CALL\\]\\s*name:\\s*(\\S+)\\s*args:\\s*(\\{.*?\\})\\s*\\[END_TOOL_CALL\\]",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return new ToolCallInfo(matcher.group(1).trim(), matcher.group(2).trim());
        }
        return null;
    }

    public String executeToolByName(String toolName, String argsJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> args = objectMapper.readValue(argsJson, Map.class);
            for (Tool tool : tools) {
                if (tool.getName().equals(toolName)) {
                    return tool.execute(args);
                }
            }
            return "未知工具: " + toolName;
        } catch (Exception e) {
            log.error("执行工具 {} 失败: {}", toolName, e.getMessage());
            return "工具执行失败: " + e.getMessage();
        }
    }

    public record ToolCallInfo(String name, String argsJson) {}

    public List<Tool> getTools() {
        return tools;
    }

    public interface StreamCallback {
        void onToken(String token);
        void onComplete();
        void onError(String error);
        default void onToolCall(String toolName, String argsJson) {}
        default void onToolResult(String resultJson) {}
    }

    private record ModelEndpoint(Long modelId, String provider, LlmEndpoint endpoint) {}
}