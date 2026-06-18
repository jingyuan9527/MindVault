package com.mindvault.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.common.service.MetricsService;
import com.mindvault.content.AutoProcessService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.operationlog.OperationLogService;
import com.mindvault.review.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

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
 * 知识库服务
 *
 * 核心业务逻辑：
 * 1. 知识入库时调用嵌入模型生成向量
 * 2. 提供向量相似度搜索
 * 3. CRUD 操作
 *
 * v0.1 简化：嵌入模型调用直接通过主模型 API 实现
 * v0.3 支持独立的嵌入模型配置
 */
@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    private final KnowledgeMapper mapper;
    private final OperationLogService operationLogService;
    private final AutoProcessService autoProcessService;
    private final ReviewService reviewService;
    private final ModelConfigService modelConfigService;
    private final MetricsService metricsService;
    private final ObjectMapper objectMapper;

    public KnowledgeService(KnowledgeMapper mapper,
                            OperationLogService operationLogService,
                            AutoProcessService autoProcessService,
                            ReviewService reviewService,
                            ModelConfigService modelConfigService,
                            MetricsService metricsService) {
        this.mapper = mapper;
        this.operationLogService = operationLogService;
        this.autoProcessService = autoProcessService;
        this.reviewService = reviewService;
        this.modelConfigService = modelConfigService;
        this.metricsService = metricsService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 添加知识条目
     *
     * v0.1 简化版：先入库，嵌入生成由 Agent 的 @Tool 完成
     * v0.2 新增：入库后自动触发摘要+标签生成
     * v0.3 改为异步：入库 → 队列 → 后台生成嵌入
     */
    @Transactional
    public Knowledge addKnowledge(Knowledge knowledge) {
        LocalDateTime now = LocalDateTime.now();
        knowledge.setCreatedAt(now);
        knowledge.setUpdatedAt(now);
        mapper.insert(knowledge);
        log.info("添加知识: id={}, title={}, type={}", knowledge.getId(), knowledge.getTitle(), knowledge.getContentType());
        operationLogService.log("KNOWLEDGE", "ADD", knowledge.getId(),
                "添加知识「" + knowledge.getTitle() + "」");

        triggerAutoProcess(knowledge);
        try {
            reviewService.scheduleReview(knowledge.getId());
        } catch (Exception e) {
            log.warn("自动安排复习失败: {}", e.getMessage());
        }

        return knowledge;
    }

    /** 获取知识列表（分页） */
    public List<Knowledge> listAll(int page, int size) {
        Page<Knowledge> mpPage = mapper.selectPage(new Page<>(page + 1, size), null);
        return mpPage.getRecords();
    }

    /** 获取单条知识 */
    public Knowledge getById(Long id) {
        Knowledge k = mapper.selectById(id);
        if (k == null) throw new IllegalArgumentException("知识不存在: " + id);
        return k;
    }

    /**
     * 语义相似度搜索
     *
     * 两步查询：先查相似 ID 列表，再批量获取实体。
     * 避免原生查询返回 Object[] 时的类型转换问题。
     *
     * @param embedding 查询文本的向量嵌入（字符串格式）
     * @param topN      返回结果数
     * @return 知识条目列表，每个包含 similarity 分数
     */
    public List<Map<String, Object>> searchSimilar(String embedding, int topN) {
        List<Object[]> results = mapper.findSimilarIds(embedding, topN);

        Map<Long, Double> similarityMap = new LinkedHashMap<>();
        for (Object[] row : results) {
            Long id = ((Number) row[0]).longValue();
            Double similarity = ((Number) row[1]).doubleValue();
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
            item.put("content", k.getContent());
            item.put("similarity", entry.getValue());
            list.add(item);
        }

        log.info("语义搜索: topN={}, 返回 {} 条结果", topN, list.size());
        return list;
    }

    /**
     * 智能混合搜索（关键字 + 向量 + RRF 重排序）
     *
     * 三级策略：
     * 1. 有嵌入模型 → 混合检索 + RRF 重排序
     * 2. 纯向量检索
     * 3. 纯关键字检索（降级）
     */
    public List<Map<String, Object>> hybridSearch(String query, int limit) {
        boolean hasVector = hasEmbedding();

        if (hasVector) {
            return hybridSearchWithRerank(query, limit);
        }
        return keywordSearchWithRank(query, limit);
    }

    private boolean hasEmbedding() {
        try {
            return mapper.selectCount(null) > 0
                    && mapper.findFirstWithEmbedding() != null;
        } catch (Exception e) {
            return false;
        }
    }

    private List<Map<String, Object>> hybridSearchWithRerank(String query, int limit) {
        String embedding = generateEmbedding(query);
        if (embedding == null) {
            return keywordSearchWithRank(query, limit);
        }

        int fetchLimit = Math.max(limit * 3, 20);
        List<Object[]> keywordResults = mapper.keywordSearchWithRank(query, fetchLimit);
        List<Object[]> vectorResults = mapper.findSimilarIds(embedding, fetchLimit);

        double k = 60.0;
        Map<Long, Double> rrfScores = new LinkedHashMap<>();

        int rank = 1;
        for (Object[] row : keywordResults) {
            Long id = ((Number) row[0]).longValue();
            rrfScores.merge(id, 1.0 / (k + rank), Double::sum);
            rank++;
        }

        rank = 1;
        for (Object[] row : vectorResults) {
            Long id = ((Number) row[0]).longValue();
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
            item.put("content", kn.getContent());
            item.put("summary", kn.getSummary());
            item.put("tags", kn.getTags());
            item.put("contentType", kn.getContentType());
            item.put("sourceUrl", kn.getSourceUrl());
            item.put("similarity", entry.getValue());
            item.put("createdAt", kn.getCreatedAt() != null ? kn.getCreatedAt().toString() : null);
            list.add(item);
        }

        log.info("混合搜索: query={}, 返回 {} 条结果 (RRF)", query, list.size());
        return list;
    }

    public List<Map<String, Object>> keywordSearchWithRank(String query, int limit) {
        List<Knowledge> results = mapper.keywordSearch(query, limit);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Knowledge k : results) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", k.getId());
            item.put("title", k.getTitle());
            item.put("content", k.getContent());
            item.put("summary", k.getSummary());
            item.put("tags", k.getTags());
            item.put("contentType", k.getContentType());
            item.put("sourceUrl", k.getSourceUrl());
            item.put("createdAt", k.getCreatedAt());
            list.add(item);
        }
        return list;
    }

    private String generateEmbedding(String text) {
        List<ModelConfig> embeddingModels = modelConfigService.getAvailableEmbeddingModels();
        if (embeddingModels.isEmpty()) return null;

        ModelConfig embModel = embeddingModels.get(0);
        if (text.length() > 8000) text = text.substring(0, 8000);
        try {
            String embedUrl = buildEmbeddingUrl(embModel);
            if (embedUrl == null) return null;

            RestClient.Builder builder = RestClient.builder()
                    .baseUrl(embedUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader("Authorization", "Bearer " + embModel.getApiKey());

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", embModel.getModelName());
            if ("OLLAMA".equalsIgnoreCase(embModel.getProvider())) {
                requestBody.put("prompt", text);
            } else {
                requestBody.put("input", text);
            }

            String responseJson = builder.build().post()
                    .body(objectMapper.writeValueAsString(requestBody))
                    .retrieve()
                    .body(String.class);

            List<Double> vector = parseEmbeddingResponse(embModel.getProvider(), responseJson);
            if (vector != null && !vector.isEmpty()) {
                return "[" + vector.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
            }
        } catch (Exception e) {
            log.warn("生成查询向量失败: {}", e.getMessage());
        }
        return null;
    }

    private String buildEmbeddingUrl(ModelConfig config) {
        return switch (config.getProvider().toUpperCase()) {
            case "ALIYUN" -> "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings";
            case "DEEPSEEK" -> (config.getBaseUrl() != null ? config.getBaseUrl() : "https://api.deepseek.com/v1") + "/embeddings";
            case "OPENAI" -> (config.getBaseUrl() != null ? config.getBaseUrl() : "https://api.openai.com/v1") + "/embeddings";
            case "OLLAMA" -> (config.getBaseUrl() != null ? config.getBaseUrl() : "http://localhost:11434") + "/api/embeddings";
            default -> null;
        };
    }

    @SuppressWarnings("unchecked")
    private List<Double> parseEmbeddingResponse(String provider, String json) {
        try {
            Map<String, Object> root = objectMapper.readValue(json, Map.class);
            if ("OLLAMA".equalsIgnoreCase(provider)) {
                if (root.containsKey("embedding")) return (List<Double>) root.get("embedding");
            } else {
                List<Map<String, Object>> data = (List<Map<String, Object>>) root.get("data");
                if (data != null && !data.isEmpty()) {
                    return (List<Double>) data.get(0).get("embedding");
                }
            }
        } catch (Exception e) {
            log.warn("解析嵌入向量响应失败: {}", e.getMessage());
        }
        return null;
    }

    /** 关键字搜索（降级方案，返回实体列表） */
    public List<Knowledge> searchByKeyword(String query, int limit) {
        return mapper.keywordSearch(query, limit);
    }

    /** 关键字 + 标签联合搜索 */
    public List<Knowledge> searchByKeywordWithTag(String query, int limit, String tag) {
        return mapper.searchByTag("[\"" + tag + "\"]", limit);
    }

    /** 更新知识条目 */
    @Transactional
    public Knowledge updateKnowledge(Long id, Knowledge updated) {
        Knowledge existing = getById(id);
        existing.setTitle(updated.getTitle());
        existing.setContent(updated.getContent());
        existing.setContentType(updated.getContentType());
        existing.setSourceUrl(updated.getSourceUrl());
        existing.setSummary(updated.getSummary());
        existing.setTags(updated.getTags());
        existing.setMetadata(updated.getMetadata());
        existing.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(existing);
        log.info("更新知识: id={}, title={}", id, existing.getTitle());
        operationLogService.log("KNOWLEDGE", "UPDATE", id,
                "更新知识「" + existing.getTitle() + "」");
        return existing;
    }

    /** 更新嵌入向量 */
    @Transactional
    public void updateEmbedding(Long id, String embedding) {
        Knowledge k = mapper.selectById(id);
        if (k != null) {
            k.setEmbedding(embedding);
            mapper.updateById(k);
        }
    }

    /** 删除知识 */
    @Transactional
    public void deleteKnowledge(Long id) {
        Knowledge k = getById(id);
        mapper.deleteById(id);
        log.info("删除知识: id={}, title={}", id, k.getTitle());
        operationLogService.log("KNOWLEDGE", "DELETE", id,
                "删除知识「" + k.getTitle() + "」");
    }

    public List<Map<String, Object>> getAllTags() {
        return mapper.aggregateTags();
    }

    private void triggerAutoProcess(Knowledge knowledge) {
        if (knowledge.getContent() == null || knowledge.getContent().isBlank()) return;
        try {
            autoProcessService.autoProcess(knowledge.getId(), knowledge.getTitle(), knowledge.getContent());
        } catch (Exception e) {
            log.warn("自动处理失败: {}", e.getMessage());
        }
    }

    @Transactional
    public void batchDelete(List<Long> ids) {
        for (Long id : ids) {
            Knowledge k = mapper.selectById(id);
            if (k != null) {
                mapper.deleteById(id);
                log.info("批量删除知识: id={}, title={}", id, k.getTitle());
                operationLogService.log("KNOWLEDGE", "DELETE", id,
                        "批量删除知识「" + k.getTitle() + "」");
            }
        }
    }

    @Transactional
    public void batchTag(List<Long> ids, String tag) {
        for (Long id : ids) {
            Knowledge k = mapper.selectById(id);
            if (k == null) continue;
            List<String> existingTags = new ArrayList<>();
            if (k.getTags() != null && !k.getTags().equals("[]")) {
                try {
                    existingTags = objectMapper.readValue(k.getTags(), new TypeReference<List<String>>() {});
                } catch (Exception ignored) {}
            }
            if (!existingTags.contains(tag)) {
                existingTags.add(tag);
                try {
                    k.setTags(objectMapper.writeValueAsString(existingTags));
                } catch (Exception e) {
                    log.warn("序列化标签失败: id={}", id);
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

    public String batchExport(List<Long> ids) {
        try {
            List<Knowledge> selected = mapper.selectBatchIds(ids);
            List<Map<String, Object>> exportList = new ArrayList<>();
            for (Knowledge k : selected) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("title", k.getTitle());
                item.put("content", k.getContent());
                item.put("contentType", k.getContentType());
                item.put("sourceUrl", k.getSourceUrl());
                item.put("summary", k.getSummary());
                item.put("tags", k.getTags());
                item.put("createdAt", k.getCreatedAt() != null ? k.getCreatedAt().toString() : null);
                item.put("updatedAt", k.getUpdatedAt() != null ? k.getUpdatedAt().toString() : null);
                exportList.add(item);
            }
            Map<String, Object> exportData = new LinkedHashMap<>();
            exportData.put("version", "0.2.0");
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
    public void updateTags(Long id, List<String> tags) {
        Knowledge k = getById(id);
        try {
            k.setTags(objectMapper.writeValueAsString(tags));
        } catch (Exception e) {
            throw new RuntimeException("序列化标签失败", e);
        }
        k.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(k);
        log.info("更新标签: id={}, tags={}", id, tags);
        operationLogService.log("KNOWLEDGE", "TAG", id,
                "更新标签「" + String.join(", ", tags) + "」");
    }

    public String exportAllAsJson() {
        try {
            List<Knowledge> all = mapper.selectList(null);
            List<Map<String, Object>> exportList = new ArrayList<>();
            for (Knowledge k : all) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("title", k.getTitle());
                item.put("content", k.getContent());
                item.put("contentType", k.getContentType());
                item.put("sourceUrl", k.getSourceUrl());
                item.put("summary", k.getSummary());
                item.put("tags", k.getTags());
                item.put("createdAt", k.getCreatedAt() != null ? k.getCreatedAt().toString() : null);
                item.put("updatedAt", k.getUpdatedAt() != null ? k.getUpdatedAt().toString() : null);
                exportList.add(item);
            }
            Map<String, Object> exportData = new LinkedHashMap<>();
            exportData.put("version", "0.2.0");
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

    public byte[] exportAllAsMarkdown() {
        try {
            List<Knowledge> all = mapper.selectList(null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            for (Knowledge k : all) {
                String safeName = k.getTitle()
                        .replaceAll("[/\\\\:*?\"<>|]", "_")
                        .replaceAll("\\s+", "_");
                if (safeName.length() > 80) safeName = safeName.substring(0, 80);

                String tags = "";
                List<String> tagList = new ArrayList<>();
                if (k.getTags() != null && !k.getTags().equals("[]")) {
                    try {
                        tagList = objectMapper.readValue(k.getTags(), new TypeReference<List<String>>() {});
                    } catch (Exception e) { /* ignore */ }
                }

                String tagsSection = tagList.isEmpty() ? "" : "\n\n标签: " + String.join(", ", tagList);
                String summarySection = k.getSummary() != null && !k.getSummary().isBlank()
                        ? "\n\n> " + k.getSummary().replaceAll("\n", "\n> ") : "";
                String sourceSection = k.getSourceUrl() != null && !k.getSourceUrl().isBlank()
                        ? "\n\n来源: " + k.getSourceUrl() : "";
                String dateSection = "\n\n---\n创建于 " + (k.getCreatedAt() != null ? k.getCreatedAt().format(dtf) : "未知");

                String md = "---\n"
                        + "title: " + k.getTitle() + "\n"
                        + "type: " + k.getContentType() + "\n"
                        + "created: " + (k.getCreatedAt() != null ? k.getCreatedAt().format(dtf) : "") + "\n"
                        + (k.getUpdatedAt() != null ? "updated: " + k.getUpdatedAt().format(dtf) + "\n" : "")
                        + (k.getSourceUrl() != null ? "source: " + k.getSourceUrl() + "\n" : "")
                        + (k.getSummary() != null ? "summary: " + k.getSummary() + "\n" : "")
                        + "---\n\n"
                        + "# " + k.getTitle() + "\n"
                        + summarySection
                        + tagsSection
                        + sourceSection
                        + "\n\n" + k.getContent()
                        + dateSection + "\n";

                String folder = tagList.isEmpty() ? "未分类" : sanitizeFolderName(tagList.get(0));
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

    private static String sanitizeFolderName(String name) {
        return name.replaceAll("[/\\\\:*?\"<>|]", "_").trim();
    }

    public String exportAllAsCsv() {
        try {
            List<Knowledge> all = mapper.selectList(null);
            StringBuilder sb = new StringBuilder();
            sb.append("标题,内容,类型,摘要,标签,来源,创建时间\n");
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (Knowledge k : all) {
                String tags = "";
                if (k.getTags() != null && !k.getTags().equals("[]")) {
                    try {
                        List<String> tagList = objectMapper.readValue(k.getTags(), new TypeReference<List<String>>() {});
                        tags = String.join("; ", tagList);
                    } catch (Exception ignored) {}
                }
                sb.append(escapeCsv(k.getTitle())).append(",");
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

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

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
                String title = (String) items.get(i).getOrDefault("title", "未命名");
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
    public int importFromJson(String json) {
        return importFromJsonWithConflict(json, "skip");
    }

    @Transactional
    public int importFromJsonWithConflict(String json, String conflictMode) {
        try {
            Map<String, Object> importData = objectMapper.readValue(json,
                    new TypeReference<Map<String, Object>>() {});
            Object itemsObj = importData.get("items");
            if (itemsObj == null) return 0;

            List<Map<String, Object>> items = extractItems(itemsObj);

            int imported = 0;
            for (Map<String, Object> item : items) {
                String title = (String) item.getOrDefault("title", "未命名");

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
                k.setMetadata("{}");
                LocalDateTime now = LocalDateTime.now();
                k.setCreatedAt(now);
                k.setUpdatedAt(now);
                mapper.insert(k);
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