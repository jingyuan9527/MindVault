package com.mindvault.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.ai.client.AiModelFactory;
import com.mindvault.ai.client.AiService;
import com.mindvault.ai.prompt.PromptRegistry;
import com.mindvault.common.service.MetricsService;
import com.mindvault.content.AutoProcessService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.mindvault.common.dto.PageResult;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.operationlog.OperationLogService;
import com.mindvault.relation.KnowledgeRelationMapper;
import com.mindvault.relation.entity.KnowledgeRelation;
import com.mindvault.review.ReviewService;
import com.mindvault.systemconfig.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.mindvault.knowledge.dto.ImportPreview;
import com.mindvault.knowledge.dto.ImportPreview.ConflictItem;

/**
 * 知识库核心服务
 *
 * 知识条目的增删改查、搜索、导入导出、标签管理、AI 自动处理编排。
 * 这是系统中最复杂的服务，依赖多个模块协同工作。
 *
 * 核心方法概览：
 * - CRUD: addKnowledge / updateKnowledge / deleteKnowledge / getById
 * - 搜索: hybridSearch / keywordSearchWithRank / searchSimilar / searchByKeyword
 * - 批量操作: batchDelete / batchTag / batchAiTag / batchExport
 * - 导入导出: exportAllAsJson / exportAllAsMarkdown / exportAllAsCsv / importFromJsonWithConflict
 * - 标签: updateTags / getAllTags / mergeTags
 * - AI: updateAiFields / updateEmbedding / reprocessKnowledge
 *
 * 事务边界：
 * - 每个写操作都有 @Transactional 保证原子性
 * - addKnowledge 包含：插入 + AI 标题生成(同步) + 异步自动处理 + 复习计划安排
 * - deleteKnowledge 包含：删除 + 级联删除关联记录
 */
@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    private final KnowledgeMapper mapper;
    private final OperationLogService operationLogService;
    private final AutoProcessService autoProcessService;
    private final ReviewService reviewService;
    private final ModelConfigService modelConfigService;
    private final AiModelFactory aiModelFactory;
    private final AiService aiService;
    private final MetricsService metricsService;
    private final KnowledgeRelationMapper relationMapper;
    private final SystemConfigService config;
    private final ObjectMapper objectMapper;

    public KnowledgeService(KnowledgeMapper mapper,
                            OperationLogService operationLogService,
                            AutoProcessService autoProcessService,
                            ReviewService reviewService,
                            ModelConfigService modelConfigService,
                            AiModelFactory aiModelFactory,
                            AiService aiService,
                            MetricsService metricsService,
                            KnowledgeRelationMapper relationMapper,
                            SystemConfigService config) {
        this.mapper = mapper;
        this.operationLogService = operationLogService;
        this.autoProcessService = autoProcessService;
        this.reviewService = reviewService;
        this.modelConfigService = modelConfigService;
        this.aiModelFactory = aiModelFactory;
        this.aiService = aiService;
        this.metricsService = metricsService;
        this.relationMapper = relationMapper;
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    /** 新增知识：插入 DB → 同步生成 AI 标题 → 异步 R1 自动处理 → 安排复习计划 */
    @Transactional
    public Knowledge addKnowledge(Knowledge knowledge) {
        LocalDateTime now = LocalDateTime.now();
        knowledge.setCreatedAt(now);
        knowledge.setUpdatedAt(now);
        knowledge.setAutoProcessStatus(config.getString("default.knowledge.auto-process-status", "PENDING"));
        mapper.insert(knowledge);
        log.info("添加知识: id={}, userTitle={}, type={}", knowledge.getId(), knowledge.getTitle(), knowledge.getContentType());
        operationLogService.log("KNOWLEDGE", "ADD", knowledge.getId(),
                "添加知识「" + knowledge.getTitle() + "」");

        if (knowledge.getContent() != null && !knowledge.getContent().isBlank()) {
            String aiTitle = autoProcessService.generateAiTitleSync(knowledge.getId(), knowledge.getTitle(), knowledge.getContent());
            if (aiTitle != null) {
                knowledge.setAiTitle(aiTitle);
            }
            triggerAutoProcess(knowledge);
        }
        try {
            reviewService.scheduleReview(knowledge.getId());
        } catch (Exception e) {
            log.warn("自动安排复习失败: {}", e.getMessage());
        }

        return knowledge;
    }

    /** 分页列表查询：支持关键词搜索、多标签筛选、自定义排序（createdAt/updatedAt/title） */
    public PageResult<Knowledge> listAll(int page, int size, String keyword, List<String> tags,
                                          String sortBy, String sortOrder) {
        QueryWrapper<Knowledge> qw = new QueryWrapper<>();

        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like("title", keyword)
                    .or().like("ai_title", keyword)
                    .or().like("content", keyword));
        }

        if (tags != null && !tags.isEmpty()) {
            for (String tag : tags) {
                String tagJson = "[\"" + tag + "\"]";
                qw.and(w -> w.apply("tags::jsonb @> {0}::jsonb", tagJson)
                        .or().apply("user_tags::jsonb @> {0}::jsonb", tagJson));
            }
        }

        long total = mapper.selectCount(qw);

        String column = switch (sortBy != null ? sortBy : "createdAt") {
            case "updatedAt" -> "updated_at";
            case "title" -> "title";
            default -> "created_at";
        };
        qw.orderBy(true, "asc".equalsIgnoreCase(sortOrder), column);
        int offset = page * size;
        qw.last("LIMIT " + size + " OFFSET " + offset);
        List<Knowledge> records = mapper.selectList(qw);
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PageResult<>(records, total, page, size, totalPages);
    }

    /** 简化分页列表（不包含关键字/标签筛选），用于内部调用 */
    public List<Knowledge> listAllSimple(int page, int size) {
        int offset = page * size;
        QueryWrapper<Knowledge> qw = new QueryWrapper<>();
        qw.orderBy(true, false, "created_at");
        qw.last("LIMIT " + size + " OFFSET " + offset);
        return mapper.selectList(qw);
    }

    /** 按 ID 获取单条知识，不存在时抛出 IllegalArgumentException */
    public Knowledge getById(Long id) {
        Knowledge k = mapper.selectById(id);
        if (k == null) throw new IllegalArgumentException("知识不存在: " + id);
        return k;
    }

    /** 语义搜索：基于向量相似度查询，返回 id+title+aiTitle+content+similarity */
    public List<Map<String, Object>> searchSimilar(String embedding, int topN) {
        List<Map<String, Object>> results = mapper.findSimilarIds(embedding, topN);
        Map<Long, Double> similarityMap = new LinkedHashMap<>();
        for (Map<String, Object> row : results) {
            Long id = ((Number) row.get("id")).longValue();
            Double similarity = ((Number) row.get("similarity")).doubleValue();
            similarityMap.put(id, similarity);
        }
        List<Knowledge> knowledgeList = mapper.selectBatchIds(similarityMap.keySet());
        Map<Long, Knowledge> knowledgeMap = knowledgeList.stream()
                .collect(Collectors.toMap(Knowledge::getId, k -> k));

        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : similarityMap.entrySet()) {
            Knowledge k = knowledgeMap.get(entry.getKey());
            if (k == null) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", k.getId());
            item.put("title", k.getTitle());
            item.put("aiTitle", k.getAiTitle());
            item.put("content", k.getContent());
            item.put("similarity", entry.getValue());
            list.add(item);
        }
        log.info("语义搜索: topN={}, 返回 {} 条结果", topN, list.size());
        return list;
    }

    /**
     * 混合搜索（核心搜索入口）
     *
     * 策略：
     * 1. 如果数据库中有嵌入向量 → 混合搜索（关键词 + 向量，RRF 融合排序）
     * 2. 否则 → 纯关键词搜索（带排名）
     */
    public List<Map<String, Object>> hybridSearch(String query, int limit) {
        boolean hasVector = hasEmbedding();
        if (hasVector) {
            return hybridSearchWithRerank(query, limit);
        }
        return keywordSearchWithRank(query, limit);
    }

    /** 检查数据库中是否已有嵌入向量（避免查询空 embedding 时报错） */
    private boolean hasEmbedding() {
        try {
            return mapper.selectCount(null) > 0
                    && mapper.findFirstWithEmbedding() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 混合搜索核心实现（RRF 融合）
     * 1. 关键词搜索（带 ILIKE 排名）
     * 2. 向量语义搜索（余弦距离）
     * 3. RRF（Reciprocal Rank Fusion）融合两组结果
     * 参数 k（rrf-k）控制融合权重
     */
    private List<Map<String, Object>> hybridSearchWithRerank(String query, int limit) {
        String embedding = generateEmbedding(query);
        if (embedding == null) {
            return keywordSearchWithRank(query, limit);
        }
        int multiplier = config.getInt("threshold.search.fetch-limit-multiplier", 3);
        int minFetch = config.getInt("threshold.search.min-fetch-limit", 20);
        int fetchLimit = Math.max(limit * multiplier, minFetch);
        List<Map<String, Object>> keywordResults = mapper.keywordSearchWithRank(query, fetchLimit);
        List<Map<String, Object>> vectorResults = mapper.findSimilarIds(embedding, fetchLimit);
        double k = config.getDouble("threshold.search.rrf-k", 60.0);
        Map<Long, Double> rrfScores = new LinkedHashMap<>();
        int rank = 1;
        for (Map<String, Object> row : keywordResults) {
            Long id = ((Number) row.get("id")).longValue();
            rrfScores.merge(id, 1.0 / (k + rank), Double::sum);
            rank++;
        }
        rank = 1;
        for (Map<String, Object> row : vectorResults) {
            Long id = ((Number) row.get("id")).longValue();
            rrfScores.merge(id, 1.0 / (k + rank), Double::sum);
            rank++;
        }
        List<Map.Entry<Long, Double>> sorted = rrfScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .toList();
        List<Long> ids = sorted.stream().map(Map.Entry::getKey).toList();
        Map<Long, Knowledge> knowledgeMap = mapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Knowledge::getId, entry -> entry));
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : sorted) {
            Knowledge kn = knowledgeMap.get(entry.getKey());
            if (kn == null) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", kn.getId());
            item.put("title", kn.getTitle());
            item.put("aiTitle", kn.getAiTitle());
            item.put("content", kn.getContent());
            item.put("summary", kn.getSummary());
            item.put("tags", kn.getTags());
            item.put("userTags", kn.getUserTags());
            item.put("contentType", kn.getContentType());
            item.put("sourceUrl", kn.getSourceUrl());
            item.put("similarity", entry.getValue());
            item.put("createdAt", kn.getCreatedAt() != null ? kn.getCreatedAt().toString() : null);
            list.add(item);
        }
        log.info("混合搜索: query={}, 返回 {} 条结果 (RRF)", query, list.size());
        return list;
    }

    /** 纯关键词搜索（按创建时间降序返回完整信息） */
    public List<Map<String, Object>> keywordSearchWithRank(String query, int limit) {
        List<Knowledge> results = mapper.keywordSearch(query, limit);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Knowledge k : results) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", k.getId());
            item.put("title", k.getTitle());
            item.put("aiTitle", k.getAiTitle());
            item.put("content", k.getContent());
            item.put("summary", k.getSummary());
            item.put("tags", k.getTags());
            item.put("userTags", k.getUserTags());
            item.put("contentType", k.getContentType());
            item.put("sourceUrl", k.getSourceUrl());
            item.put("createdAt", k.getCreatedAt());
            list.add(item);
        }
        return list;
    }

    /** 获取显示标题：优先 AI 标题，回退到用户标题 */
    public String displayTitle(Knowledge k) {
        return k.getAiTitle() != null && !k.getAiTitle().isBlank() ? k.getAiTitle() : k.getTitle();
    }

    /** 为搜索查询生成嵌入向量（使用第一个可用的嵌入模型） */
    private String generateEmbedding(String text) {
        List<ModelConfig> embeddingModels = modelConfigService.getAvailableEmbeddingModels();
        if (embeddingModels.isEmpty()) return null;
        ModelConfig embModel = embeddingModels.get(0);
        if (text.length() > 8000) text = text.substring(0, 8000);
        try {
            EmbeddingModel model = aiModelFactory.buildEmbeddingModel(embModel);
            float[] vector = model.embed(text);
            if (vector != null && vector.length > 0) {
                StringJoiner sj = new StringJoiner(",");
                        for (float v : vector) sj.add(String.valueOf(v));
                        return "[" + sj + "]";
            }
        } catch (Exception e) {
            log.warn("生成查询向量失败: {}", e.getMessage());
        }
        return null;
    }

    /** 关键词搜索（供 Agent 工具调用） */
    public List<Knowledge> searchByKeyword(String query, int limit) {
        return mapper.keywordSearch(query, limit);
    }

    /** 关键词 + 标签组合搜索 */
    public List<Knowledge> searchByKeywordWithTag(String query, int limit, String tag) {
        return mapper.searchByTag("[\"" + tag + "\"]", limit);
    }

    @Transactional
    /** 更新知识（仅更新用户字段，不影响 AI 字段） */
    public Knowledge updateKnowledge(Long id, Knowledge updated) {
        Knowledge existing = getById(id);
        existing.setTitle(updated.getTitle());
        existing.setContent(updated.getContent());
        existing.setContentType(updated.getContentType());
        existing.setSourceUrl(updated.getSourceUrl());
        existing.setSummary(updated.getSummary());
        existing.setUserTags(updated.getUserTags());
        existing.setMetadata(updated.getMetadata());
        existing.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(existing);
        log.info("更新知识: id={}, userTitle={}", id, existing.getTitle());
        operationLogService.log("KNOWLEDGE", "UPDATE", id,
                "更新知识「" + existing.getTitle() + "」");
        return existing;
    }

    @Transactional
    /** 更新 AI 自动生成的标题和标签（由 AutoProcessService 调用） */
    public Knowledge updateAiFields(Long id, String aiTitle, String aiTags) {
        Knowledge k = getById(id);
        if (aiTitle != null) k.setAiTitle(aiTitle);
        if (aiTags != null) k.setTags(aiTags);
        k.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(k);
        log.info("更新 AI 字段: id={}, aiTitle={}", id, k.getAiTitle());
        return k;
    }

    @Transactional
    /** 更新嵌入向量（由 AutoProcessService 生成后调用） */
    public void updateEmbedding(Long id, String embedding) {
        Knowledge k = mapper.selectById(id);
        if (k != null) {
            k.setEmbedding(embedding);
            mapper.updateById(k);
        }
    }

    @Transactional
    /** 删除知识（级联删除关联记录） */
    public void deleteKnowledge(Long id) {
        Knowledge k = getById(id);
        mapper.deleteById(id);
        relationMapper.deleteByKnowledgeId(id);
        log.info("删除知识: id={}, title={}", id, k.getTitle());
        operationLogService.log("KNOWLEDGE", "DELETE", id,
                "删除知识「" + k.getTitle() + "」");
    }

    /** 触发异步 R1 自动处理 */
    private void triggerAutoProcess(Knowledge knowledge) {
        if (knowledge.getContent() == null || knowledge.getContent().isBlank()) return;
        autoProcessService.autoProcessAsync(knowledge.getId(), knowledge.getTitle(), knowledge.getContent());
    }

    @Transactional
    /** 更新知识的自动处理状态（由调度器/自动处理服务调用） */
    public void updateAutoProcessStatus(Long id, String status) {
        Knowledge k = mapper.selectById(id);
        if (k != null) {
            k.setAutoProcessStatus(status);
            k.setUpdatedAt(LocalDateTime.now());
            mapper.updateById(k);
        }
    }

    @Transactional
    /** 重置 AI 字段并重新执行自动处理流水线 */
    public void reprocessKnowledge(Long id) {
        Knowledge k = getById(id);
        k.setAutoProcessStatus(config.getString("default.knowledge.auto-process-status", "PENDING"));
        k.setAiTitle(null);
        k.setTags(config.getString("default.knowledge.tags-empty-json", "[]"));
        k.setSummary(null);
        mapper.updateById(k);
        autoProcessService.autoProcessAsync(k.getId(), k.getTitle(), k.getContent());
        log.info("重新处理知识: id={}", id);
        operationLogService.log("KNOWLEDGE", "REPROCESS", id,
                "重新处理知识「" + k.getTitle() + "」");
    }

    @Transactional
    /** 批量删除（逐条删除并记录操作日志） */
    public void batchDelete(List<Long> ids) {
        for (Long id : ids) {
            Knowledge k = mapper.selectById(id);
            if (k != null) {
                mapper.deleteById(id);
                relationMapper.deleteByKnowledgeId(id);
                log.info("批量删除知识: id={}, title={}", id, k.getTitle());
                operationLogService.log("KNOWLEDGE", "DELETE", id,
                        "批量删除知识「" + k.getTitle() + "」");
            }
        }
    }

    @Transactional
    /** 批量添加用户标签（去重追加） */
    public void batchTag(List<Long> ids, String tag) {
        for (Long id : ids) {
            Knowledge k = mapper.selectById(id);
            if (k == null) continue;
            List<String> existingTags = new ArrayList<>();
            if (k.getUserTags() != null && !k.getUserTags().equals("[]")) {
                try {
                    existingTags = objectMapper.readValue(k.getUserTags(), new TypeReference<List<String>>() {});
                } catch (Exception e) {
                    log.warn("反序列化用户标签失败: id={}, userTags={}", id, k.getUserTags(), e);
                }
            }
            if (!existingTags.contains(tag)) {
                existingTags.add(tag);
                try {
                    k.setUserTags(objectMapper.writeValueAsString(existingTags));
                } catch (Exception e) {
                    log.warn("序列化用户标签失败: id={}", id);
                    continue;
                }
                k.setUpdatedAt(LocalDateTime.now());
                mapper.updateById(k);
                log.info("批量打标签: id={}, tag={}", id, tag);
                operationLogService.log("KNOWLEDGE", "TAG", id,
                        "批量添加标签「" + tag + "」");
            }
        }
    }

    /** AI 批量打标签：LLM 分析内容生成标签，合并到 user_tags 中 */
    public int batchAiTag(List<Long> ids) {
        int success = 0;
        int truncateLen = config.getInt("threshold.auto.truncate-length", 2000);
        double temperature = config.getDouble("threshold.auto.llm-temperature", 0.3);
        int maxTokens = config.getInt("threshold.auto.tags-max-tokens", 300);
        for (Long id : ids) {
            try {
                Knowledge k = mapper.selectById(id);
                if (k == null) continue;
                String displayTitle = displayTitle(k);
                String content = k.getContent();
                if (content == null || content.isBlank()) continue;
                String prompt = PromptRegistry.AUTO_TAGS.resolve(config, displayTitle, AiService.truncate(content, truncateLen));
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
                log.info("AI 批量打标签: id={}, tags={}", id, cleaned);
                operationLogService.log("KNOWLEDGE", "AI_TAG", id,
                        "AI 批量添加标签: " + String.join(", ", aiTags));
            } catch (Exception e) {
                log.warn("AI 批量打标签失败: id={}, error={}", id, e.getMessage());
            }
        }
        return success;
    }

    /** 批量导出为 JSON（按 ID 列表） */
    public String batchExport(List<Long> ids) {
        try {
            List<Knowledge> selected = mapper.selectBatchIds(ids);
            List<Map<String, Object>> exportList = new ArrayList<>();
            for (Knowledge k : selected) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("title", k.getTitle());
                item.put("aiTitle", k.getAiTitle());
                item.put("content", k.getContent());
                item.put("contentType", k.getContentType());
                item.put("sourceUrl", k.getSourceUrl());
                item.put("summary", k.getSummary());
                item.put("tags", k.getTags());
                item.put("userTags", k.getUserTags());
                item.put("createdAt", k.getCreatedAt() != null ? k.getCreatedAt().toString() : null);
                item.put("updatedAt", k.getUpdatedAt() != null ? k.getUpdatedAt().toString() : null);
                exportList.add(item);
            }
            Map<String, Object> exportData = new LinkedHashMap<>();
            exportData.put("version", config.getString("default.export.version", "0.4.0"));
            exportData.put("exportedAt", LocalDateTime.now().toString());
            exportData.put("count", exportList.size());
            exportData.put("items", exportList);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportData);
        } catch (Exception e) {
            log.error("批量导出失败: {}", e.getMessage());
            return "[]";
        }
    }

    @Transactional
    /** 更新用户标签（替换式） */
    public void updateTags(Long id, List<String> tags) {
        Knowledge k = getById(id);
        try {
            k.setUserTags(objectMapper.writeValueAsString(tags));
        } catch (Exception e) {
            throw new RuntimeException("序列化标签失败", e);
        }
        k.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(k);
        log.info("更新用户标签: id={}, tags={}", id, tags);
        operationLogService.log("KNOWLEDGE", "TAG", id,
                "更新标签「" + String.join(", ", tags) + "」");
    }

    /** 全量导出为 JSON（含版本号、导出时间） */
    public String exportAllAsJson() {
        try {
            List<Knowledge> all = mapper.selectList(null);
            List<Map<String, Object>> exportList = new ArrayList<>();
            for (Knowledge k : all) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("title", k.getTitle());
                item.put("aiTitle", k.getAiTitle());
                item.put("content", k.getContent());
                item.put("contentType", k.getContentType());
                item.put("sourceUrl", k.getSourceUrl());
                item.put("summary", k.getSummary());
                item.put("tags", k.getTags());
                item.put("userTags", k.getUserTags());
                item.put("createdAt", k.getCreatedAt() != null ? k.getCreatedAt().toString() : null);
                item.put("updatedAt", k.getUpdatedAt() != null ? k.getUpdatedAt().toString() : null);
                exportList.add(item);
            }
            Map<String, Object> exportData = new LinkedHashMap<>();
            exportData.put("version", config.getString("default.export.version", "0.4.0"));
            exportData.put("exportedAt", LocalDateTime.now().toString());
            exportData.put("count", exportList.size());
            exportData.put("items", exportList);
            metricsService.recordExport("json");
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportData);
        } catch (Exception e) {
            log.error("导出失败: {}", e.getMessage());
            return "[]";
        }
    }

    /** 全量导出为 Markdown（ZIP 压缩包），按标签分类到不同目录 */
    public byte[] exportAllAsMarkdown() {
        try {
            List<Knowledge> all = mapper.selectList(null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            for (Knowledge k : all) {
                String displayName = displayTitle(k);
                String safeName = displayName
                        .replaceAll("[/\\\\:*?\"<>|]", "_")
                        .replaceAll("\\s+", "_");
                int maxFilenameLen = config.getInt("threshold.export.max-filename-length", 80);
                if (safeName.length() > maxFilenameLen) safeName = safeName.substring(0, maxFilenameLen);

                String tags = "";
                List<String> tagList = new ArrayList<>();
                String mergedTags = mergeTags(k.getTags(), k.getUserTags());
                if (mergedTags != null && !mergedTags.equals("[]")) {
                    try {
                        tagList = objectMapper.readValue(mergedTags, new TypeReference<List<String>>() {});
                    } catch (Exception e) { /* ignore */ }
                }

                String tagsSection = tagList.isEmpty() ? "" : "\n\n标签: " + String.join(", ", tagList);
                String summarySection = k.getSummary() != null && !k.getSummary().isBlank()
                        ? "\n\n> " + k.getSummary().replaceAll("\n", "\n> ") : "";
                String sourceSection = k.getSourceUrl() != null && !k.getSourceUrl().isBlank()
                        ? "\n\n来源: " + k.getSourceUrl() : "";
                String dateSection = "\n\n---\n创建于 " + (k.getCreatedAt() != null ? k.getCreatedAt().format(dtf) : "未知");

                String md = "---\n"
                        + "title: " + displayName + "\n"
                        + "type: " + k.getContentType() + "\n"
                        + "created: " + (k.getCreatedAt() != null ? k.getCreatedAt().format(dtf) : "") + "\n"
                        + (k.getUpdatedAt() != null ? "updated: " + k.getUpdatedAt().format(dtf) + "\n" : "")
                        + (k.getSourceUrl() != null ? "source: " + k.getSourceUrl() + "\n" : "")
                        + (k.getSummary() != null ? "summary: " + k.getSummary() + "\n" : "")
                        + "---\n\n"
                        + "# " + displayName + "\n"
                        + summarySection
                        + tagsSection
                        + sourceSection
                        + "\n\n" + k.getContent()
                        + dateSection + "\n";

                String untaggedFolder = config.getString("default.export.markdown-untagged-folder", "未分类");
                String folder = tagList.isEmpty() ? untaggedFolder : sanitizeFolderName(tagList.get(0));
                String entryName = folder + "/" + safeName + ".md";
                zos.putNextEntry(new ZipEntry(entryName));
                zos.write(md.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            zos.finish();
            log.info("Markdown导出完成: 共 {} 条", all.size());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Markdown导出失败: {}", e.getMessage());
            return new byte[0];
        }
    }

    /** 清理文件夹名称中的非法字符 */
    private static String sanitizeFolderName(String name) {
        return name.replaceAll("[/\\\\:*?\"<>|]", "_").trim();
    }

    /** 合并 AI 标签和用户标签（去重），返回 JSON 数组格式 */
    private String mergeTags(String aiTags, String userTags) {
        try {
            Set<String> merged = new LinkedHashSet<>();
            if (aiTags != null && !aiTags.equals("[]")) {
                merged.addAll(objectMapper.readValue(aiTags, new TypeReference<List<String>>() {}));
            }
            if (userTags != null && !userTags.equals("[]")) {
                merged.addAll(objectMapper.readValue(userTags, new TypeReference<List<String>>() {}));
            }
            return objectMapper.writeValueAsString(new ArrayList<>(merged));
        } catch (Exception e) {
            return "[]";
        }
    }

    /** 全量导出为 CSV 格式 */
    public String exportAllAsCsv() {
        try {
            List<Knowledge> all = mapper.selectList(null);
            StringBuilder sb = new StringBuilder();
            sb.append(config.getString("default.export.csv-header", "标题,内容,类型,摘要,标签,来源,创建时间")).append("\n");
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (Knowledge k : all) {
                String tags = "";
                String merged = mergeTags(k.getTags(), k.getUserTags());
                if (merged != null && !merged.equals("[]")) {
                    try {
                        List<String> tagList = objectMapper.readValue(merged, new TypeReference<List<String>>() {});
                        tags = String.join("; ", tagList);
                    } catch (Exception e) {
                        log.warn("CSV 导出标签解析失败: id={}", k.getId(), e);
                    }
                }
                sb.append(escapeCsv(displayTitle(k))).append(",");
                sb.append(escapeCsv(k.getContent())).append(",");
                sb.append(escapeCsv(k.getContentType())).append(",");
                sb.append(escapeCsv(k.getSummary())).append(",");
                sb.append(escapeCsv(tags)).append(",");
                sb.append(escapeCsv(k.getSourceUrl())).append(",");
                sb.append(k.getCreatedAt() != null ? k.getCreatedAt().format(dtf) : "").append("\n");
            }
            log.info("CSV导出完成: 共 {} 条", all.size());
            return sb.toString();
        } catch (Exception e) {
            log.error("CSV导出失败: {}", e.getMessage());
            return "标题,内容,类型,摘要,标签,来源,创建时间\n";
        }
    }

    /** CSV 字段转义（处理逗号、双引号、换行） */
    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /** 获取所有标签聚合统计（返回标签名 + 使用次数） */
    public List<Map<String, Object>> getAllTags() {
        return mapper.aggregateTags();
    }

    /** 预览导入数据：解析 JSON 并检测冲突（按标题匹配已有知识） */
    public ImportPreview previewImport(String json) {
        try {
            Map<String, Object> importData = objectMapper.readValue(json,
                    new TypeReference<Map<String, Object>>() {});
            Object itemsObj = importData.get("items");
            if (itemsObj == null) return new ImportPreview(0, 0, 0, List.of());

            List<Map<String, Object>> items = extractItems(itemsObj);
            List<ConflictItem> conflicts = new ArrayList<>();
            int conflictCount = 0;

            for (int i = 0; i < items.size(); i++) {
                String defaultTitle = config.getString("default.knowledge.import-default-title", "未命名");
                String title = (String) items.get(i).getOrDefault("title", defaultTitle);
                List<Knowledge> existing = mapper.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(title, "");
                if (!existing.isEmpty()) {
                    conflicts.add(new ConflictItem(i, title, existing.get(0).getTitle()));
                    conflictCount++;
                }
            }

            return new ImportPreview(items.size(), items.size() - conflictCount, conflictCount, conflicts);
        } catch (Exception e) {
            log.error("导入预览失败: {}", e.getMessage());
            return new ImportPreview(0, 0, 0, List.of());
        }
    }

    /** 从导入数据中提取条目列表（类型安全检查） */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractItems(Object itemsObj) {
        if (itemsObj instanceof List<?> list) {
            List<Map<String, Object>> items = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof Map<?, ?> m) {
                    items.add((Map<String, Object>) m);
                }
            }
            return items;
        }
        return List.of();
    }

    @Transactional
    /** 导入 JSON（默认冲突策略：skip 跳过） */
    public int importFromJson(String json) {
        return importFromJsonWithConflict(json, "skip");
    }

    @Transactional
    /**
     * 导入 JSON（可指定冲突策略）
     *
     * @param conflictMode skip=跳过冲突, overwrite=覆盖已有
     */
    public int importFromJsonWithConflict(String json, String conflictMode) {
        try {
            Map<String, Object> importData = objectMapper.readValue(json,
                    new TypeReference<Map<String, Object>>() {});
            Object itemsObj = importData.get("items");
            if (itemsObj == null) return 0;

            List<Map<String, Object>> items = extractItems(itemsObj);

            int imported = 0;
            String defaultTitle = config.getString("default.knowledge.import-default-title", "未命名");
            for (Map<String, Object> item : items) {
                String title = (String) item.getOrDefault("title", defaultTitle);

                if ("skip".equals(conflictMode)) {
                    List<Knowledge> existing = mapper.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(title, "");
                    if (!existing.isEmpty()) {
                        log.debug("跳过冲突知识: {}", title);
                        continue;
                    }
                }

                if ("overwrite".equals(conflictMode)) {
                    List<Knowledge> existing = mapper.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(title, "");
                    if (!existing.isEmpty()) {
                        Knowledge existingK = existing.get(0);
                        existingK.setContent((String) item.getOrDefault("content", ""));
                        existingK.setContentType((String) item.getOrDefault("contentType", "TEXT"));
                        existingK.setSourceUrl((String) item.getOrDefault("sourceUrl", null));
                        existingK.setSummary((String) item.getOrDefault("summary", null));
                        existingK.setTags((String) item.getOrDefault("tags", "[]"));
                        existingK.setUserTags((String) item.getOrDefault("userTags", "[]"));
                        existingK.setUpdatedAt(LocalDateTime.now());
                        mapper.updateById(existingK);
                        imported++;
                        continue;
                    }
                }

                Knowledge k = new Knowledge();
                k.setTitle(title);
                k.setContent((String) item.getOrDefault("content", ""));
                k.setContentType((String) item.getOrDefault("contentType", "TEXT"));
                k.setSourceUrl((String) item.getOrDefault("sourceUrl", null));
                k.setSummary((String) item.getOrDefault("summary", null));
                k.setTags((String) item.getOrDefault("tags", "[]"));
                k.setUserTags((String) item.getOrDefault("userTags", "[]"));
                k.setMetadata("{}");
                k.setAutoProcessStatus("PENDING");
                LocalDateTime now = LocalDateTime.now();
                k.setCreatedAt(now);
                k.setUpdatedAt(now);
                mapper.insert(k);
                autoProcessService.autoProcessAsync(k.getId(), k.getTitle(), k.getContent());
                imported++;
            }

            log.info("导入完成: 共 {} 条 (冲突模式: {})", imported, conflictMode);
            metricsService.recordImport();
            operationLogService.log("KNOWLEDGE", "IMPORT", null,
                    "导入 " + imported + " 条知识 (冲突模式: " + conflictMode + ")");
            return imported;
        } catch (Exception e) {
            log.error("导入失败: {}", e.getMessage());
            throw new RuntimeException("导入失败: " + e.getMessage());
        }
    }
}
