package com.mindvault.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.content.AutoProcessService;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.operationlog.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    private final KnowledgeRepository repository;
    private final OperationLogService operationLogService;
    private final AutoProcessService autoProcessService;
    private final ObjectMapper objectMapper;

    public KnowledgeService(KnowledgeRepository repository,
                            OperationLogService operationLogService,
                            AutoProcessService autoProcessService) {
        this.repository = repository;
        this.operationLogService = operationLogService;
        this.autoProcessService = autoProcessService;
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
        Knowledge saved = repository.save(knowledge);
        log.info("添加知识: id={}, title={}, type={}", saved.getId(), saved.getTitle(), saved.getContentType());
        operationLogService.log("KNOWLEDGE", "ADD", saved.getId(),
                "添加知识「" + knowledge.getTitle() + "」");

        triggerAutoProcess(saved);

        return saved;
    }

    /** 获取知识列表（分页） */
    public List<Knowledge> listAll(int page, int size) {
        return repository.findAll(PageRequest.of(page, size)).getContent();
    }

    /** 获取单条知识 */
    public Knowledge getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("知识不存在: " + id));
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
        List<Object[]> results = repository.findSimilarIds(embedding, topN);

        Map<Long, Double> similarityMap = new LinkedHashMap<>();
        for (Object[] row : results) {
            Long id = ((Number) row[0]).longValue();
            Double similarity = ((Number) row[1]).doubleValue();
            similarityMap.put(id, similarity);
        }

        List<Knowledge> knowledgeList = repository.findAllById(similarityMap.keySet());
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

    /** 关键字搜索（降级方案） */
    public List<Knowledge> searchByKeyword(String query, int limit) {
        return repository.keywordSearch(query, limit);
    }

    /** 关键字 + 标签联合搜索 */
    public List<Knowledge> searchByKeywordWithTag(String query, int limit, String tag) {
        return repository.searchByTag("[\"" + tag + "\"]", limit);
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
        Knowledge saved = repository.save(existing);
        log.info("更新知识: id={}, title={}", id, saved.getTitle());
        operationLogService.log("KNOWLEDGE", "UPDATE", id,
                "更新知识「" + saved.getTitle() + "」");
        return saved;
    }

    /** 更新嵌入向量 */
    @Transactional
    public void updateEmbedding(Long id, String embedding) {
        repository.findById(id).ifPresent(k -> {
            k.setEmbedding(embedding);
            repository.save(k);
        });
    }

    /** 删除知识 */
    @Transactional
    public void deleteKnowledge(Long id) {
        Knowledge k = getById(id);
        repository.deleteById(id);
        log.info("删除知识: id={}, title={}", id, k.getTitle());
        operationLogService.log("KNOWLEDGE", "DELETE", id,
                "删除知识「" + k.getTitle() + "」");
    }

    private void triggerAutoProcess(Knowledge knowledge) {
        if (knowledge.getContent() == null || knowledge.getContent().isBlank()) return;
        try {
            autoProcessService.autoProcess(knowledge.getId(), knowledge.getTitle(), knowledge.getContent());
        } catch (Exception e) {
            log.warn("自动处理失败: {}", e.getMessage());
        }
    }

    public String exportAllAsJson() {
        try {
            List<Knowledge> all = repository.findAll();
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
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportData);
        } catch (Exception e) {
            log.error("导出失败: {}", e.getMessage());
            return "[]";
        }
    }

    public byte[] exportAllAsMarkdown() {
        try {
            List<Knowledge> all = repository.findAll();
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

    @Transactional
    public int importFromJson(String json) {
        try {
            Map<String, Object> importData = objectMapper.readValue(json,
                    new TypeReference<Map<String, Object>>() {});
            Object itemsObj = importData.get("items");
            if (itemsObj == null) return 0;

            List<Map<String, Object>> items;
            if (itemsObj instanceof List<?> list) {
                items = new ArrayList<>();
                for (Object o : list) {
                    if (o instanceof Map<?, ?> m) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> casted = (Map<String, Object>) m;
                        items.add(casted);
                    }
                }
            } else {
                return 0;
            }

            int imported = 0;
            for (Map<String, Object> item : items) {
                Knowledge k = new Knowledge();
                k.setTitle((String) item.getOrDefault("title", "未命名"));
                k.setContent((String) item.getOrDefault("content", ""));
                k.setContentType((String) item.getOrDefault("contentType", "TEXT"));
                k.setSourceUrl((String) item.getOrDefault("sourceUrl", null));
                k.setSummary((String) item.getOrDefault("summary", null));
                k.setTags((String) item.getOrDefault("tags", "[]"));
                k.setMetadata("{}");
                repository.save(k);
                imported++;
            }

            log.info("导入完成: 共 {} 条", imported);
            operationLogService.log("KNOWLEDGE", "IMPORT", null,
                    "导入 " + imported + " 条知识");
            return imported;
        } catch (Exception e) {
            log.error("导入失败: {}", e.getMessage());
            throw new RuntimeException("导入失败: " + e.getMessage());
        }
    }
}