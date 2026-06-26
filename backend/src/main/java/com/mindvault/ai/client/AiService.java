package com.mindvault.ai.client;

import com.mindvault.common.service.MetricsService;
import com.mindvault.model.service.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.tokenusage.service.TokenUsageService;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

/**
 * AI 调用服务（简单封装）
 *
 * 对外提供统一的 LLM 调用入口，自动获取主模型配置、构建 ChatModel、执行调用、
 * 记录指标和 Token 用量。
 *
 * 调用链路：
 * AoService.call(prompt) → ModelConfigService.getPrimaryChatModel()
 *                       → AiModelFactory.buildChatModel(model, temperature)
 *                       → ChatModel.call(prompt)
 *                       → TokenUsageService.recordUsage(...)
 *                       → MetricsService.recordLlmCall* (...)
 *
 * 注意：
 * - 此服务仅用于简单的一问一答场景
 * - 多轮对话 / 流式响应 / Agent 调用应使用 LlmFailoverService 或 AgentService
 * - 调用异常时返回 null（外部调用方需处理 null 判断）
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final ModelConfigService modelConfigService;
    private final AiModelFactory aiModelFactory;
    private final MetricsService metricsService;
    private final TokenUsageService tokenUsageService;

    public AiService(ModelConfigService modelConfigService,
                     AiModelFactory aiModelFactory,
                     MetricsService metricsService,
                     TokenUsageService tokenUsageService) {
        this.modelConfigService = modelConfigService;
        this.aiModelFactory = aiModelFactory;
        this.metricsService = metricsService;
        this.tokenUsageService = tokenUsageService;
    }

    /** 使用默认参数调用 AI（temperature=0.7, maxTokens=2000, source=UNKNOWN） */
    public String call(String prompt) {
        return call(prompt, 0.7, 2000, "UNKNOWN");
    }

    /** 使用指定温度和最长 Token 数调用 AI */
    public String call(String prompt, double temperature, int maxTokens) {
        return call(prompt, temperature, maxTokens, "UNKNOWN");
    }

    /**
     * 核心调用方法
     *
     * @param prompt      用户提示词
     * @param temperature 温度参数（控制创造性）
     * @param maxTokens   （预留）最大输出 Token 数
     * @param source      调用来源标识（用于 Token 用量记录）
     * @return AI 响应文本，失败时返回 null
     */
    public String call(String prompt, double temperature, int maxTokens, String source) {
        try {
            ModelConfig model = modelConfigService.getPrimaryChatModel();
            ChatModel chatModel = aiModelFactory.buildChatModel(model, temperature);

            Timer.Sample sample = metricsService.startLlmCall();
            ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
            metricsService.recordLlmCallSuccess(sample, model.getProvider(), model.getModelName());

            String content = response.getResult().getOutput().getText();

            var usage = response.getMetadata().getUsage();
            if (usage != null) {
                int promptTokens = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
                int completionTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
                tokenUsageService.recordUsage(model, promptTokens, completionTokens, source, null);
            }

            return content;
        } catch (Exception e) {
            log.warn("AI 调用失败: {}", e.getMessage());
            return null;
        }
    }

    /** 截断文本到指定最大长度 */
    public static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }
}