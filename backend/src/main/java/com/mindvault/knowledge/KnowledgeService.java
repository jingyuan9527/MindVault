package com.mindvault.knowledge;

import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.operationlog.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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

    public KnowledgeService(KnowledgeRepository repository,
                            OperationLogService operationLogService) {
        this.repository = repository;
        this.operationLogService = operationLogService;
    }

    /**
     * 添加知识条目
     *
     * v0.1 简化版：先入库，嵌入生成由 Agent 的 @Tool 完成
     * v0.3 改为异步：入库 → 队列 → 后台生成嵌入
     */
    @Transactional
    public Knowledge addKnowledge(Knowledge knowledge) {
        // v0.1: 简单入库，嵌入由 AgentScope Tool 后续设置
        Knowledge saved = repository.save(knowledge);
        log.info("添加知识: id={}, title={}, type={}", saved.getId(), saved.getTitle(), saved.getContentType());
        operationLogService.log("KNOWLEDGE", "ADD", saved.getId(),
                "添加知识「" + knowledge.getTitle() + "」");
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
}