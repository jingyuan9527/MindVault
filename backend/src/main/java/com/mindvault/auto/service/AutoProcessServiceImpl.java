package com.mindvault.auto.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.ai.client.AiModelFactory;
import com.mindvault.ai.client.AiService;
import com.mindvault.ai.prompt.PromptRegistry;
import com.mindvault.auto.entity.AutoProcessLog;
import com.mindvault.auto.mapper.AutoProcessLogMapper;
import com.mindvault.knowledge.service.KnowledgeService;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.model.service.ModelConfigService;
import com.mindvault.auto.config.AutoThresholdProperties;
import com.mindvault.systemconfig.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.StringJoiner;

@Service
public class AutoProcessServiceImpl implements AutoProcessService {

    private static final Logger log = LoggerFactory.getLogger(AutoProcessServiceImpl.class);

    private final ModelConfigService modelConfigService;
    private final AiService aiService;
    private final AiModelFactory aiModelFactory;
    private final KnowledgeService knowledgeService;
    private final AutoProcessLogMapper logMapper;
    private final AutoThresholdProperties autoThresholdProperties;
    private final SystemConfigService systemConfigService;
    private final ObjectMapper objectMapper;

    public AutoProcessServiceImpl(ModelConfigService modelConfigService,
                                  AiService aiService,
                                  AiModelFactory aiModelFactory,
                                  @Lazy KnowledgeService knowledgeService,
                                  AutoProcessLogMapper logMapper,
                                  AutoThresholdProperties autoThresholdProperties,
                                  SystemConfigService systemConfigService) {
        this.modelConfigService = modelConfigService;
        this.aiService = aiService;
        this.aiModelFactory = aiModelFactory;
        this.knowledgeService = knowledgeService;
        this.logMapper = logMapper;
        this.autoThresholdProperties = autoThresholdProperties;
        this.systemConfigService = systemConfigService;
        this.objectMapper = new ObjectMapper();
    }

    @Async
    @Override
    public void autoProcessAsync(Long knowledgeId, String userTitle, String content) {
        autoProcess(knowledgeId, userTitle, content);
    }

    @Override
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

    @Override
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

    private String generateAiTitle(String userTitle, String content) {
        try {
            int truncateLen = autoThresholdProperties.getTruncateLength();
            double temperature = autoThresholdProperties.getLlmTemperature();
            int maxTokens = autoThresholdProperties.getTitleMaxTokens();
            String prompt = PromptRegistry.AUTO_TITLE.resolve(systemConfigService, userTitle, AiService.truncate(content, truncateLen));
            return aiService.call(prompt, temperature, maxTokens, "AUTO_PROCESS");
        } catch (Exception e) {
            log.warn("生成 AI 标题失败: {}", e.getMessage());
        }
        return null;
    }

    private String generateSummary(String userTitle, String content) {
        try {
            int truncateLen = autoThresholdProperties.getTruncateLength();
            double temperature = autoThresholdProperties.getLlmTemperature();
            int maxTokens = autoThresholdProperties.getSummaryMaxTokens();
            String prompt = PromptRegistry.AUTO_SUMMARY.resolve(systemConfigService, userTitle, AiService.truncate(content, truncateLen));
            return aiService.call(prompt, temperature, maxTokens, "AUTO_PROCESS");
        } catch (Exception e) {
            log.warn("生成摘要失败: {}", e.getMessage());
        }
        return null;
    }

    private String generateTags(String userTitle, String content) {
        try {
            int truncateLen = autoThresholdProperties.getTruncateLength();
            double temperature = autoThresholdProperties.getLlmTemperature();
            int maxTokens = autoThresholdProperties.getTagsMaxTokens();
            String prompt = PromptRegistry.AUTO_TAGS.resolve(systemConfigService, userTitle, AiService.truncate(content, truncateLen));
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

    private void generateEmbedding(Long knowledgeId, String userTitle, String content) {
        List<ModelConfig> embeddingModels = modelConfigService.getAvailableEmbeddingModels();
        if (embeddingModels.isEmpty()) return;

        ModelConfig embModel = embeddingModels.get(0);
        int embedTruncate = autoThresholdProperties.getEmbeddingTruncateLength();
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
