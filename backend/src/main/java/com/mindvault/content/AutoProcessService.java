package com.mindvault.content;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.ai.client.AiModelFactory;
import com.mindvault.ai.prompt.PromptRegistry;
import com.mindvault.auto.AutoProcessLogMapper;
import com.mindvault.auto.entity.AutoProcessLog;
import com.mindvault.common.service.LlmFailoverService;
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
import java.util.stream.Collectors;

@Service
public class AutoProcessService {

    private static final Logger log = LoggerFactory.getLogger(AutoProcessService.class);

    private final ModelConfigService modelConfigService;
    private final LlmFailoverService llmFailoverService;
    private final AiModelFactory aiModelFactory;
    private final KnowledgeService knowledgeService;
    private final AutoProcessLogMapper logMapper;
    private final SystemConfigService config;
    private final ObjectMapper objectMapper;

    public AutoProcessService(ModelConfigService modelConfigService,
                              LlmFailoverService llmFailoverService,
                              AiModelFactory aiModelFactory,
                              @Lazy KnowledgeService knowledgeService,
                              AutoProcessLogMapper logMapper,
                              SystemConfigService config) {
        this.modelConfigService = modelConfigService;
        this.llmFailoverService = llmFailoverService;
        this.aiModelFactory = aiModelFactory;
        this.knowledgeService = knowledgeService;
        this.logMapper = logMapper;
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    @Async
    public void autoProcessAsync(Long knowledgeId, String userTitle, String content) {
        autoProcess(knowledgeId, userTitle, content);
    }

    public void autoProcess(Long knowledgeId, String userTitle, String content) {
        List<ModelConfig> models = modelConfigService.getAvailableChatModels();
        if (models.isEmpty()) {
            log.warn("未配置主模型，跳过自动处理: knowledgeId={}", knowledgeId);
            return;
        }

        LocalDateTime startedAt = LocalDateTime.now();
        String aiTitle = generateAiTitle(userTitle, content, models);
        String tagsJson = generateTags(userTitle, content, models);
        String summary = generateSummary(userTitle, content, models);

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

    private String generateAiTitle(String userTitle, String content, List<ModelConfig> models) {
        try {
            int truncateLen = config.getInt("threshold.auto.truncate-length", 2000);
            double temperature = config.getDouble("threshold.auto.llm-temperature", 0.3);
            int maxTokens = config.getInt("threshold.auto.title-max-tokens", 100);
            String prompt = PromptRegistry.AUTO_TITLE.resolve(config, userTitle, LlmFailoverService.truncate(content, truncateLen));
            return llmFailoverService.call(models, new LlmFailoverService.LlmCallOptions(prompt, temperature, maxTokens, false, null));
        } catch (Exception e) {
            log.warn("生成 AI 标题失败: {}", e.getMessage());
        }
        return null;
    }

    private String generateSummary(String userTitle, String content, List<ModelConfig> models) {
        try {
            int truncateLen = config.getInt("threshold.auto.truncate-length", 2000);
            double temperature = config.getDouble("threshold.auto.llm-temperature", 0.3);
            int maxTokens = config.getInt("threshold.auto.summary-max-tokens", 300);
            String prompt = PromptRegistry.AUTO_SUMMARY.resolve(config, userTitle, LlmFailoverService.truncate(content, truncateLen));
            return llmFailoverService.call(models, new LlmFailoverService.LlmCallOptions(prompt, temperature, maxTokens, false, null));
        } catch (Exception e) {
            log.warn("生成摘要失败: {}", e.getMessage());
        }
        return null;
    }

    private String generateTags(String userTitle, String content, List<ModelConfig> models) {
        try {
            int truncateLen = config.getInt("threshold.auto.truncate-length", 2000);
            double temperature = config.getDouble("threshold.auto.llm-temperature", 0.3);
            int maxTokens = config.getInt("threshold.auto.tags-max-tokens", 300);
            String prompt = PromptRegistry.AUTO_TAGS.resolve(config, userTitle, LlmFailoverService.truncate(content, truncateLen));
            String result = llmFailoverService.call(models, new LlmFailoverService.LlmCallOptions(prompt, temperature, maxTokens, false, null));
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