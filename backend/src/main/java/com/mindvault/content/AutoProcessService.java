package com.mindvault.content;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.ai.client.AiModelFactory;
import com.mindvault.ai.client.AiService;
import com.mindvault.ai.prompt.PromptRegistry;
import com.mindvault.auto.AutoProcessLogMapper;
import com.mindvault.auto.entity.AutoProcessLog;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.systemconfig.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.StringJoiner;

/**
 * AI 自动处理服务（R1 阶段）
 *
 * 对新增的知识条目自动执行以下处理（按顺序）：
 * 1. AI 标题生成（generateAiTitle）— 基于内容生成精简中文标题
 * 2. AI 标签生成（generateTags）— 生成 3-5 个中文标签（JSON 数组格式）
 * 3. AI 摘要生成（generateSummary）— 生成 50-100 字中文摘要
 * 4. 嵌入向量生成（generateEmbedding）— 构建语义搜索向量
 *
 * 触发方式：
 * - 自动：知识创建时由 @Async autoProcessAsync 异步触发
 * - 手动：KnowledgeService.reprocessKnowledge() → generateAiTitleSync()
 *
 * 所有 LLM 调用参数（截断长度、温度、最大 Token 等）均通过 SystemConfig 可配置。
 * 每一步的结果会持久化到 knowledge 表对应字段，并记录 AutoProcessLog。
 *
 * 注意：如果没有配置主模型，自动处理会被静默跳过（不抛异常）。
 */
@Service
public class AutoProcessService {

    private static final Logger log = LoggerFactory.getLogger(AutoProcessService.class);

    private final ModelConfigService modelConfigService;
    private final AiService aiService;
    private final AiModelFactory aiModelFactory;
    private final KnowledgeService knowledgeService;
    private final AutoProcessLogMapper logMapper;
    private final SystemConfigService config;
    private final ObjectMapper objectMapper;

    public AutoProcessService(ModelConfigService modelConfigService,
                              AiService aiService,
                              AiModelFactory aiModelFactory,
                              @Lazy KnowledgeService knowledgeService,
                              AutoProcessLogMapper logMapper,
                              SystemConfigService config) {
        this.modelConfigService = modelConfigService;
        this.aiService = aiService;
        this.aiModelFactory = aiModelFactory;
        this.knowledgeService = knowledgeService;
        this.logMapper = logMapper;
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    /** 异步触发自动处理（由知识创建/更新事件调用） */
    @Async
    public void autoProcessAsync(Long knowledgeId, String userTitle, String content) {
        autoProcess(knowledgeId, userTitle, content);
    }

    /** 同步生成 AI 标题并更新到知识条目（用于手动 reprocess） */
    public String generateAiTitleSync(Long knowledgeId, String userTitle, String content) {
        try {
            modelConfigService.getPrimaryChatModel();
        } catch (Exception e) {
            log.warn("未配置主模型，跳过 AI 标题生成: knowledgeId={}", knowledgeId);
            return null;
        }
        String aiTitle = generateAiTitle(userTitle, content);
        if (aiTitle != null) {
            knowledgeService.updateAiFields(knowledgeId, aiTitle, null);
        }
        return aiTitle;
    }

    /**
     * R1 自动处理主流程（同步执行）
     *
     * 处理顺序：
     * 1. 检查是否有现成的 aiTitle（避免重复生成）
     * 2. 依次生成标题 → 标签 → 摘要（LLM 调用）
     * 3. 更新 knowledge 表对应字段并设置状态为 TITLE_TAG_DONE
     * 4. 生成嵌入向量（用于语义搜索）
     * 5. 记录 AutoProcessLog
     */
    public void autoProcess(Long knowledgeId, String userTitle, String content) {
        try {
            modelConfigService.getPrimaryChatModel();
        } catch (Exception e) {
            log.warn("未配置主模型，跳过自动处理: knowledgeId={}", knowledgeId);
            return;
        }

        LocalDateTime startedAt = LocalDateTime.now();
        var existing = knowledgeService.getById(knowledgeId);
        String aiTitle = existing.getAiTitle();
        if (aiTitle == null) {
            aiTitle = generateAiTitle(userTitle, content);
        }
        String tagsJson = generateTags(userTitle, content);
        String summary = generateSummary(userTitle, content);

        if (aiTitle != null || tagsJson != null || summary != null) {
            try {
                if (aiTitle != null) {
                    knowledgeService.updateAiFields(knowledgeId, aiTitle, tagsJson);
                } else if (tagsJson != null) {
                    knowledgeService.updateAiFields(knowledgeId, null, tagsJson);
                }
                if (summary != null) {
                    var k = knowledgeService.getById(knowledgeId);
                    k.setSummary(summary);
                    knowledgeService.updateKnowledge(knowledgeId, k);
                }
                knowledgeService.updateAutoProcessStatus(knowledgeId, "TITLE_TAG_DONE");
                log.info("R1 自动处理完成: knowledgeId={}, aiTitle={}", knowledgeId, aiTitle);
                saveLog(knowledgeId, "R1_TITLE_TAG", "SUCCESS", startedAt,
                        "aiTitle=" + aiTitle + ", tags=" + tagsJson + ", summary=" + (summary != null ? "ok" : "skip"),
                        0, 0, null);
            } catch (Exception e) {
                log.error("R1 自动处理保存失败: {}", e.getMessage());
                saveLog(knowledgeId, "R1_TITLE_TAG", "FAILED", startedAt, null, 0, 0, e.getMessage());
            }
        }

        generateEmbedding(knowledgeId, userTitle, content);
    }

    /** 调用 LLM 生成 AI 标题，参数从 SystemConfig 读取 */
    private String generateAiTitle(String userTitle, String content) {
        try {
            int truncateLen = config.getInt("threshold.auto.truncate-length", 2000);
            double temperature = config.getDouble("threshold.auto.llm-temperature", 0.3);
            int maxTokens = config.getInt("threshold.auto.title-max-tokens", 100);
            String prompt = PromptRegistry.AUTO_TITLE.resolve(config, userTitle, AiService.truncate(content, truncateLen));
            return aiService.call(prompt, temperature, maxTokens, "AUTO_PROCESS");
        } catch (Exception e) {
            log.warn("生成 AI 标题失败: {}", e.getMessage());
        }
        return null;
    }

    /** 调用 LLM 生成内容摘要 */
    private String generateSummary(String userTitle, String content) {
        try {
            int truncateLen = config.getInt("threshold.auto.truncate-length", 2000);
            double temperature = config.getDouble("threshold.auto.llm-temperature", 0.3);
            int maxTokens = config.getInt("threshold.auto.summary-max-tokens", 300);
            String prompt = PromptRegistry.AUTO_SUMMARY.resolve(config, userTitle, AiService.truncate(content, truncateLen));
            return aiService.call(prompt, temperature, maxTokens, "AUTO_PROCESS");
        } catch (Exception e) {
            log.warn("生成摘要失败: {}", e.getMessage());
        }
        return null;
    }

    /** 调用 LLM 生成标签（JSON 数组格式），验证合法性后返回 */
    private String generateTags(String userTitle, String content) {
        try {
            int truncateLen = config.getInt("threshold.auto.truncate-length", 2000);
            double temperature = config.getDouble("threshold.auto.llm-temperature", 0.3);
            int maxTokens = config.getInt("threshold.auto.tags-max-tokens", 300);
            String prompt = PromptRegistry.AUTO_TAGS.resolve(config, userTitle, AiService.truncate(content, truncateLen));
            String result = aiService.call(prompt, temperature, maxTokens, "AUTO_PROCESS");
            if (result != null) {
                String cleaned = result.trim();
                if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
                    return cleaned;
                }
            }
        } catch (Exception e) {
            log.warn("生成标签失败: {}", e.getMessage());
        }
        return null;
    }

    /** 生成嵌入向量并更新到 knowledge 表（使用第一个可用的嵌入模型） */
    private void generateEmbedding(Long knowledgeId, String userTitle, String content) {
        List<ModelConfig> embeddingModels = modelConfigService.getAvailableEmbeddingModels();
        if (embeddingModels.isEmpty()) return;

        ModelConfig embModel = embeddingModels.get(0);
        int embedTruncate = config.getInt("threshold.auto.embedding-truncate-length", 8000);
        String text = (userTitle + "\n" + content);
        if (text.length() > embedTruncate) text = text.substring(0, embedTruncate);

        try {
            EmbeddingModel springAiModel = aiModelFactory.buildEmbeddingModel(embModel);
            float[] vector = springAiModel.embed(text);
            if (vector != null && vector.length > 0) {
                StringJoiner sj = new StringJoiner(",");
                        for (float v : vector) sj.add(String.valueOf(v));
                        String vectorStr = "[" + sj + "]";
                knowledgeService.updateEmbedding(knowledgeId, vectorStr);
                log.info("嵌入向量生成完成: knowledgeId={}, dim={}", knowledgeId, vector.length);
            }
        } catch (Exception e) {
            log.warn("嵌入向量生成失败: knowledgeId={}, error={}", knowledgeId, e.getMessage());
        }
    }

    /** 记录自动处理日志到 auto_process_log 表 */
    private void saveLog(Long knowledgeId, String round, String status, LocalDateTime startedAt,
                         String resultSummary, int tokens, int durationMs, String errorMessage) {
        try {
            AutoProcessLog l = new AutoProcessLog();
            l.setKnowledgeId(knowledgeId);
            l.setRound(round);
            l.setStatus(status);
            l.setResultSummary(resultSummary);
            l.setLlmTokens(tokens);
            l.setLlmDurationMs(durationMs);
            l.setErrorMessage(errorMessage);
            l.setStartedAt(startedAt);
            l.setCompletedAt(LocalDateTime.now());
            l.setCreatedAt(LocalDateTime.now());
            logMapper.insert(l);
        } catch (Exception e) {
            log.warn("保存自动处理日志失败: {}", e.getMessage());
        }
    }
}