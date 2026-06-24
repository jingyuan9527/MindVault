package com.mindvault.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.ai.client.AiModelFactory;
import com.mindvault.ai.prompt.PromptRegistry;
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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final ModelConfigService modelConfigService;
    private final AiModelFactory aiModelFactory;
    private final List<Tool> tools;
    private final TokenUsageService tokenUsageService;
    private final CircuitBreakerConfig circuitBreaker;
    private final MetricsService metricsService;
    private final SystemConfigService config;
    private final ObjectMapper objectMapper;

    private String systemPrompt;
    private double tokenEstimateRatio;

    public AgentService(ModelConfigService modelConfigService,
                        AiModelFactory aiModelFactory,
                        List<Tool> tools,
                        TokenUsageService tokenUsageService,
                        CircuitBreakerConfig circuitBreaker,
                        MetricsService metricsService,
                        SystemConfigService config) {
        this.modelConfigService = modelConfigService;
        this.aiModelFactory = aiModelFactory;
        this.tools = tools;
        this.tokenUsageService = tokenUsageService;
        this.circuitBreaker = circuitBreaker;
        this.metricsService = metricsService;
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        String toolDesc = tools.stream()
                .map(t -> "- " + t.getName() + ": " + t.getDescription())
                .collect(Collectors.joining("\n"));
        systemPrompt = PromptRegistry.AGENT_SYSTEM.resolve(config, toolDesc);
        tokenEstimateRatio = config.getDouble("threshold.agent.token-estimate-ratio", 3.0);
    }

    public String processMessage(String userMessage) {
        List<ModelConfig> models = modelConfigService.getAvailableChatModels();
        if (models.isEmpty()) {
            return config.getString("default.agent.no-model-message", "系统未配置主模型，请先在设置中添加并设置主模型。");
        }

        try {
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemPrompt));
            messages.add(new UserMessage(userMessage));

            String response = callLlmWithFailover(models, messages);
            String toolResult = executeToolCall(response);

            if (toolResult != null) {
                String cleanResponse = stripToolCallMarkers(response);
                messages.add(new AssistantMessage(cleanResponse));
                messages.add(new UserMessage("工具执行结果: " + toolResult));

                return callLlmWithFailover(models, messages);
            }

            return response;
        } catch (Exception e) {
            log.error("Agent 处理消息失败: {}", e.getMessage(), e);
            return config.getString("default.agent.error-message", "抱歉，处理您的消息时遇到了问题，请稍后重试。");
        }
    }

    public void processMessageStream(String userMessage, StreamCallback callback) {
        List<ModelConfig> models = modelConfigService.getAvailableChatModels();
        if (models.isEmpty()) {
            callback.onError(config.getString("default.agent.no-model-message", "系统未配置主模型，请先在设置中添加并设置主模型。"));
            return;
        }

        try {
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemPrompt));
            messages.add(new UserMessage(userMessage));

            StringBuilder fullResponse = new StringBuilder();
            streamFromLlm(models, messages, callback, fullResponse);

            ToolCallInfo toolCall = extractToolCall(fullResponse.toString());
            if (toolCall != null) {
                callback.onToolCall(toolCall.name, toolCall.argsJson);

                String result = executeToolByName(toolCall.name, toolCall.argsJson);

                callback.onToolResult(result);

                String cleanResponse = stripToolCallMarkers(fullResponse.toString());
                messages.add(new AssistantMessage(cleanResponse));
                messages.add(new UserMessage("工具执行结果: " + result));
                streamFromLlm(models, messages, callback, new StringBuilder());
            }

            callback.onComplete();
        } catch (Exception e) {
            log.error("Agent 流式处理失败: {}", e.getMessage(), e);
            callback.onError(config.getString("default.agent.error-message", "抱歉，处理您的消息时遇到了问题，请稍后重试。"));
        }
    }

    private void streamFromLlm(List<ModelConfig> models, List<Message> messages, StreamCallback callback, StringBuilder collector) {
        List<String> errors = new ArrayList<>();
        double temperature = config.getDouble("threshold.agent.default-temperature", 0.7);

        for (ModelConfig mc : models) {
            if (!circuitBreaker.isAvailable(mc.getId())) {
                errors.add("模型" + mc.getId() + " 熔断中");
                continue;
            }
            Timer.Sample sample = metricsService.startLlmCall();
            try {
                ChatModel chatModel = aiModelFactory.buildChatModel(mc, temperature);
                Prompt prompt = new Prompt(messages);

                chatModel.stream(prompt).toStream().forEach(chunk -> {
                    String token = chunk.getResult().getOutput().getText();
                    if (token != null && !token.isEmpty()) {
                        callback.onToken(token);
                        collector.append(token);
                    }
                });

                circuitBreaker.recordSuccess(mc.getId());
                metricsService.recordLlmCallSuccess(sample, mc.getProvider(), mc.getModelName());
                recordStreamUsage(mc, messages, collector.length());
                return;
            } catch (Exception e) {
                circuitBreaker.recordFailure(mc.getId());
                metricsService.recordLlmCallError(mc.getProvider(), mc.getModelName());
                log.warn("模型 id={} 流式调用失败: {}", mc.getId(), e.getMessage());
                errors.add("模型" + mc.getId() + ": " + e.getMessage());
            }
        }
        log.error("所有模型流式调用均失败: {}", String.join("; ", errors));
        throw new RuntimeException("所有模型流式调用均失败");
    }

    private void recordStreamUsage(ModelConfig mc, List<Message> messages, int completionChars) {
        try {
            int promptTokens = estimateTokens(messages);
            int completionTokens = (int) (completionChars / tokenEstimateRatio);
            if (completionTokens == 0) return;
            metricsService.recordTokens(promptTokens, completionTokens);
            tokenUsageService.recordUsage(mc, promptTokens, completionTokens, "CHAT_STREAM", null);
        } catch (Exception e) {
            log.warn("记录流式 Token 用量失败: {}", e.getMessage());
        }
    }

    private int estimateTokens(List<Message> messages) {
        int total = 0;
        for (Message msg : messages) {
            String text = msg.getText();
            if (text != null) {
                total += text.length();
            }
        }
        return (int) (total / tokenEstimateRatio);
    }

    private String callLlmWithFailover(List<ModelConfig> models, List<Message> messages) {
        return callLlmWithRetry(models, messages, 0);
    }

    private String callLlmWithRetry(List<ModelConfig> models, List<Message> messages, int retryCount) {
        List<String> errors = new ArrayList<>();
        double temperature = config.getDouble("threshold.agent.default-temperature", 0.7);

        for (ModelConfig mc : models) {
            if (!circuitBreaker.isAvailable(mc.getId())) {
                errors.add("模型" + mc.getId() + " 熔断中");
                continue;
            }

            Timer.Sample sample = metricsService.startLlmCall();
            try {
                ChatModel chatModel = aiModelFactory.buildChatModel(mc, temperature);
                Prompt prompt = new Prompt(messages);
                ChatResponse response = chatModel.call(prompt);

                String content = response.getResult().getOutput().getText();
                circuitBreaker.recordSuccess(mc.getId());
                metricsService.recordLlmCallSuccess(sample, mc.getProvider(), mc.getModelName());
                recordUsage(mc, response);
                if (content != null) return content;

                return "";
            } catch (Exception e) {
                circuitBreaker.recordFailure(mc.getId());
                metricsService.recordLlmCallError(mc.getProvider(), mc.getModelName());
                log.warn("模型 id={} 调用失败: {}", mc.getId(), e.getMessage());
                errors.add("模型" + mc.getId() + ": " + e.getMessage());
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
            return callLlmWithRetry(models, messages, retryCount + 1);
        }

        log.error("所有模型均调用失败 (已重试): {}", String.join("; ", errors));
        throw new RuntimeException("所有模型均调用失败");
    }

    private void recordUsage(ModelConfig mc, ChatResponse response) {
        try {
            var usage = response.getMetadata().getUsage();
            if (usage != null) {
                int promptTokens = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
                int completionTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
                if (promptTokens > 0 || completionTokens > 0) {
                    metricsService.recordTokens(promptTokens, completionTokens);
                    tokenUsageService.recordUsage(mc, promptTokens, completionTokens, "CHAT", null);
                }
            }
        } catch (Exception e) {
            log.warn("记录 Token 用量失败: {}", e.getMessage());
        }
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
}