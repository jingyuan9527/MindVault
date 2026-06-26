package com.mindvault.knowledge.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.ai.client.AiService;
import com.mindvault.ai.prompt.PromptRegistry;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.knowledge.mapper.KnowledgeMapper;
import com.mindvault.operationlog.service.OperationLogService;
import com.mindvault.systemconfig.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TagServiceImpl implements TagService {

    private static final Logger log = LoggerFactory.getLogger(TagServiceImpl.class);

    private final KnowledgeMapper mapper;
    private final SystemConfigService config;
    private final AiService aiService;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    public TagServiceImpl(KnowledgeMapper mapper,
                          SystemConfigService config,
                          AiService aiService,
                          OperationLogService operationLogService) {
        this.mapper = mapper;
        this.config = config;
        this.aiService = aiService;
        this.operationLogService = operationLogService;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    @Override
    public void batchTag(List<Long> ids, String tag) {
        for (Long id : ids) {
            Knowledge k = mapper.selectById(id);
            if (k == null) continue;
            List<String> existingTags = new ArrayList<>();
            if (k.getUserTags() != null && !k.getUserTags().equals("[]")) {
                try { existingTags = objectMapper.readValue(k.getUserTags(), new TypeReference<List<String>>() {}); }
                catch (Exception e) { log.warn("反序列化用户标签失败: id={}", id, e); }
            }
            if (!existingTags.contains(tag)) {
                existingTags.add(tag);
                try { k.setUserTags(objectMapper.writeValueAsString(existingTags)); }
                catch (Exception e) { log.warn("序列化用户标签失败: id={}", id); continue; }
                k.setUpdatedAt(LocalDateTime.now());
                mapper.updateById(k);
                operationLogService.log("KNOWLEDGE", "TAG", id, "批量添加标签\u00AB" + tag + "\u00BB");
            }
        }
    }

    @Override
    public int batchAiTag(List<Long> ids) {
        int success = 0;
        int truncateLen = config.getInt("threshold.auto.truncate-length", 2000);
        double temperature = config.getDouble("threshold.auto.llm-temperature", 0.3);
        int maxTokens = config.getInt("threshold.auto.tags-max-tokens", 300);
        for (Long id : ids) {
            try {
                Knowledge k = mapper.selectById(id);
                if (k == null) continue;
                String content = k.getContent();
                if (content == null || content.isBlank()) continue;
                String prompt = PromptRegistry.AUTO_TAGS.resolve(config, displayTitle(k), AiService.truncate(content, truncateLen));
                String result = aiService.call(prompt, temperature, maxTokens);
                if (result == null) continue;
                String cleaned = result.trim();
                if (!cleaned.startsWith("[") || !cleaned.endsWith("]")) continue;
                List<String> aiTags = objectMapper.readValue(cleaned, new TypeReference<>() {});
                List<String> existingTags = new ArrayList<>();
                if (k.getUserTags() != null && !k.getUserTags().equals("[]")) {
                    existingTags = objectMapper.readValue(k.getUserTags(), new TypeReference<>() {});
                }
                Set<String> merged = new LinkedHashSet<>(existingTags);
                merged.addAll(aiTags);
                k.setUserTags(objectMapper.writeValueAsString(new ArrayList<>(merged)));
                k.setUpdatedAt(LocalDateTime.now());
                mapper.updateById(k);
                success++;
                operationLogService.log("KNOWLEDGE", "AI_TAG", id, "AI 批量添加标签: " + String.join(", ", aiTags));
            } catch (Exception e) {
                log.warn("AI 批量打标签失败: id={}", id, e);
            }
        }
        return success;
    }

    @Transactional
    @Override
    public void updateTags(Long id, List<String> tags) {
        Knowledge k = mapper.selectById(id);
        if (k == null) throw new IllegalArgumentException("知识不存在: " + id);
        try { k.setUserTags(objectMapper.writeValueAsString(tags)); }
        catch (Exception e) { throw new RuntimeException("序列化标签失败", e); }
        k.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(k);
        operationLogService.log("KNOWLEDGE", "TAG", id, "更新标签\u00AB" + String.join(", ", tags) + "\u00BB");
    }

    @Override
    public List<Map<String, Object>> getAllTags() { return mapper.aggregateTags(); }

    private String displayTitle(Knowledge k) {
        return k.getAiTitle() != null && !k.getAiTitle().isBlank() ? k.getAiTitle() : k.getTitle();
    }

    @Override
    public String mergeTags(String aiTags, String userTags) {
        try {
            Set<String> merged = new LinkedHashSet<>();
            if (aiTags != null && !aiTags.equals("[]")) merged.addAll(objectMapper.readValue(aiTags, new TypeReference<List<String>>() {}));
            if (userTags != null && !userTags.equals("[]")) merged.addAll(objectMapper.readValue(userTags, new TypeReference<List<String>>() {}));
            return objectMapper.writeValueAsString(new ArrayList<>(merged));
        } catch (Exception e) { return "[]"; }
    }
}