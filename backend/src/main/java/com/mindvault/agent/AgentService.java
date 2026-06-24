package com.mindvault.agent;

import com.mindvault.ai.client.AiModelFactory;
import com.mindvault.ai.prompt.PromptRegistry;
import com.mindvault.agent.tool.AddKnowledgeTool;
import com.mindvault.agent.tool.SearchKnowledgeTool;
import com.mindvault.common.service.MetricsService;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.systemconfig.SystemConfigService;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final ModelConfigService modelConfigService;
    private final AiModelFactory aiModelFactory;
    private final MetricsService metricsService;
    private final SystemConfigService config;

    private final SearchKnowledgeTool searchTool;
    private final AddKnowledgeTool addTool;

    private ToolCallback[] toolCallbacks;
    private String systemPrompt;

    public AgentService(ModelConfigService modelConfigService,
                        AiModelFactory aiModelFactory,
                        SearchKnowledgeTool searchTool,
                        AddKnowledgeTool addTool,
                        MetricsService metricsService,
                        SystemConfigService config) {
        this.modelConfigService = modelConfigService;
        this.aiModelFactory = aiModelFactory;
        this.metricsService = metricsService;
        this.searchTool = searchTool;
        this.addTool = addTool;
        this.config = config;
    }

    @PostConstruct
    public void init() {
        MethodToolCallbackProvider provider = MethodToolCallbackProvider.builder()
                .toolObjects(searchTool, addTool)
                .build();
        this.toolCallbacks = provider.getToolCallbacks();
        StringBuilder sb = new StringBuilder();
        for (ToolCallback tc : toolCallbacks) {
            sb.append("- ").append(tc.getToolDefinition().name())
                    .append(": ").append(tc.getToolDefinition().description()).append("\n");
        }
        systemPrompt = PromptRegistry.AGENT_SYSTEM.resolve(config, sb.toString().strip());
    }

    public String processMessage(String userMessage) {
        try {
            ModelConfig model = modelConfigService.getPrimaryChatModel();
            ChatModel chatModel = aiModelFactory.buildChatModel(model);

            double temperature = config.getDouble("threshold.agent.default-temperature", 0.7);
            Timer.Sample sample = metricsService.startLlmCall();

            Prompt prompt = new Prompt(
                    List.of(new SystemMessage(systemPrompt), new UserMessage(userMessage)),
                    ToolCallingChatOptions.builder().toolCallbacks(toolCallbacks).build()
            );
            String content = chatModel.call(prompt).getResult().getOutput().getText();

            metricsService.recordLlmCallSuccess(sample, model.getProvider(), model.getModelName());
            return content;
        } catch (Exception e) {
            log.error("Agent 处理消息失败: {}", e.getMessage(), e);
            return config.getString("default.agent.error-message", "抱歉，处理您的消息时遇到了问题，请稍后重试。");
        }
    }

    public void processMessageStream(String userMessage, StreamCallback callback) {
        try {
            ModelConfig model = modelConfigService.getPrimaryChatModel();
            double temperature = config.getDouble("threshold.agent.default-temperature", 0.7);
            ChatModel chatModel = aiModelFactory.buildChatModel(model, temperature);

            Prompt prompt = new Prompt(
                    List.of(new SystemMessage(systemPrompt), new UserMessage(userMessage)),
                    ToolCallingChatOptions.builder().toolCallbacks(toolCallbacks).build()
            );

            chatModel.stream(prompt).subscribe(
                    chunk -> {
                        String token = chunk.getResult().getOutput().getText();
                        if (token != null && !token.isEmpty()) {
                            callback.onToken(token);
                        }
                    },
                    error -> callback.onError(error.getMessage()),
                    () -> {
                        if (!callback.isDisposed()) {
                            callback.onComplete();
                        }
                    }
            );
        } catch (Exception e) {
            log.error("Agent 流式处理失败: {}", e.getMessage(), e);
            callback.onError(config.getString("default.agent.error-message", "抱歉，处理您的消息时遇到了问题，请稍后重试。"));
        }
    }

    public interface StreamCallback {
        void onToken(String token);
        void onComplete();
        void onError(String error);
        default boolean isDisposed() { return false; }
    }
}