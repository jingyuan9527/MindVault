package com.mindvault.agent.service;

import com.mindvault.ai.client.AiModelFactory;
import com.mindvault.ai.prompt.PromptRegistry;
import com.mindvault.agent.tool.AddKnowledgeTool;
import com.mindvault.agent.tool.SearchKnowledgeTool;
import com.mindvault.common.service.MetricsService;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.model.service.ModelConfigService;
import com.mindvault.systemconfig.service.SystemConfigService;
import com.mindvault.tokenusage.service.TokenUsageService;
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

/**
 * AI Agent 核心服务。
 * <p>整合 LLM 调用与工具执行（知识搜索、知识添加），提供同步和流式两种消息处理模式。
 * 输入为用户消息文本，输出为 AI 回复文本（同步）或通过 StreamCallback 逐 token 回调（流式）。
 * 依赖 AiModelFactory 动态构建 ChatModel，使用 ToolCallingChatOptions 将 SearchKnowledgeTool / AddKnowledgeTool
 * 注册为 AI 可调用的工具函数。关键设计：系统提示词通过 PromptRegistry.AGENT_SYSTEM 动态渲染，
 * 包含当前可用工具的声明；调用完成后记录 token 用量到 TokenUsageService。</p>
 */
@Service
public class AgentServiceImpl implements AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentServiceImpl.class);

    private final ModelConfigService modelConfigService;
    private final AiModelFactory aiModelFactory;
    private final MetricsService metricsService;
    private final SystemConfigService config;

    private final SearchKnowledgeTool searchTool;
    private final AddKnowledgeTool addTool;
    private final TokenUsageService tokenUsageService;

    private ToolCallback[] toolCallbacks;
    private String systemPrompt;

    public AgentServiceImpl(ModelConfigService modelConfigService,
                            AiModelFactory aiModelFactory,
                            SearchKnowledgeTool searchTool,
                            AddKnowledgeTool addTool,
                            MetricsService metricsService,
                            SystemConfigService config,
                            TokenUsageService tokenUsageService) {
        this.modelConfigService = modelConfigService;
        this.aiModelFactory = aiModelFactory;
        this.searchTool = searchTool;
        this.addTool = addTool;
        this.metricsService = metricsService;
        this.config = config;
        this.tokenUsageService = tokenUsageService;
    }

    /**
     * 初始化 Agent：注册工具回调并渲染系统提示词。
     * 通过 MethodToolCallbackProvider 将 SearchKnowledgeTool 和 AddKnowledgeTool 注册为 AI 工具，
     * 并将工具声明嵌入系统提示词，使 LLM 知晓可用工具的名称和用途。
     */
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

    /**
     * 同步处理用户消息，返回 AI 完整回复。
     * 调用主模型 LLM，传入系统提示词和带工具声明的 ChatOptions，获取回复后记录 token 用量。
     * @param userMessage 用户消息文本
     * @return AI 回复文本，异常时返回错误提示
     */
    @Override
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
            var response = chatModel.call(prompt);
            String content = response.getResult().getOutput().getText();

            metricsService.recordLlmCallSuccess(sample, model.getProvider(), model.getModelName());

            var usage = response.getMetadata().getUsage();
            if (usage != null) {
                int promptTokens = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
                int completionTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
                tokenUsageService.recordUsage(model, promptTokens, completionTokens, "CHAT", null);
            }

            return content;
        } catch (Exception e) {
            log.error("Agent 处理消息失败: {}", e.getMessage(), e);
            return config.getString("default.agent.error-message", "抱歉，处理您的消息时遇到了问题，请稍后重试。");
        }
    }

    /**
     * 流式处理用户消息，通过回调逐 token 输出 AI 回复。
     * 使用 chatModel.stream() 订阅 LLM 流式响应，onToken 每次回调一个 token，
     * onComplete 表示完成，onError 表示异常。
     * @param userMessage 用户消息文本
     * @param callback 流式回调接口
     */
    @Override
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

    }