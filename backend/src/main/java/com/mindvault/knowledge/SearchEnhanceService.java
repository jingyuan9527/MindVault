package com.mindvault.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.ai.client.AiModelFactory;
import com.mindvault.ai.client.AiService;
import com.mindvault.ai.prompt.PromptRegistry;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.systemconfig.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 搜索增强服务
 *
 * 在知识库基础搜索之上提供三种 LLM 增强策略：
 * 1. HyDE（假设文档嵌入）— 先生成一个"理想文档"再向量检索，提升查询与文档的语义对齐
 * 2. 查询改写（Query Rewrite）— 用 LLM 优化用户原始查询后再搜索
 * 3. LLM 重排序（Rerank）— 对初筛结果用 LLM 逐条评分后重新排序
 *
 * 调用链路：
 * Controller → SearchEnhanceService.* → KnowledgeService.hybridSearch() → AI 调用(LlmFailoverService)
 *
 * 所有 LLM 调用参数（温度、Token 限制等）通过 SystemConfig 可配置。
 * 如果未配置主模型，所有增强策略静默回退到普通混合搜索。
 */
@Service
public class SearchEnhanceService {

    private static final Logger log = LoggerFactory.getLogger(SearchEnhanceService.class);

    private final KnowledgeService knowledgeService;
    private final ModelConfigService modelConfigService;
    private final AiService aiService;
    private final AiModelFactory aiModelFactory;
    private final SystemConfigService config;
    private final ObjectMapper objectMapper;

    public SearchEnhanceService(KnowledgeService knowledgeService,
                                ModelConfigService modelConfigService,
                                AiService aiService,
                                AiModelFactory aiModelFactory,
                                SystemConfigService config) {
        this.knowledgeService = knowledgeService;
        this.modelConfigService = modelConfigService;
        this.aiService = aiService;
        this.aiModelFactory = aiModelFactory;
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    /** HyDE 搜索：生成假设文档 → 向量检索 → 返回结果 */
    public List<Map<String, Object>> hydeSearch(String query, int topN) {
        String hypotheticalDoc = generateHypotheticalDocument(query);
        if (hypotheticalDoc == null) {
            return knowledgeService.hybridSearch(query, topN);
        }

        List<ModelConfig> embeddingModels = modelConfigService.getAvailableEmbeddingModels();
        if (embeddingModels.isEmpty()) {
            return knowledgeService.hybridSearch(query, topN);
        }

        String embedding = generateEmbeddingForText(hypotheticalDoc, embeddingModels.get(0));
        if (embedding == null) {
            return knowledgeService.hybridSearch(query, topN);
        }

        List<Map<String, Object>> results = knowledgeService.searchSimilar(embedding, topN);
        log.info("HyDE 搜索完成: query={}, 返回 {} 条", query, results.size());
        return results;
    }

    /** 查询改写搜索：改写查询 → 混合搜索 → LLM 重排序 */
    public List<Map<String, Object>> searchWithRewrite(String query, int topN) {
        String rewritten = rewriteQuery(query);
        if (rewritten == null || rewritten.isBlank()) {
            return knowledgeService.hybridSearch(query, topN);
        }
        log.info("查询重写: '{}' → '{}'", query, rewritten);
        List<Map<String, Object>> results = knowledgeService.hybridSearch(rewritten, topN);
        List<Map<String, Object>> reranked = rerankResults(rewritten, results, topN);
        return reranked;
    }

    /** 调用 LLM 改写查询语句（提取关键词、补充同义词） */
    private String rewriteQuery(String query) {
        try {
            modelConfigService.getPrimaryChatModel();
        } catch (Exception e) {
            return null;
        }

        String prompt = PromptRegistry.SEARCH_QUERY_REWRITE.resolve(config, query);
        double temperature = config.getDouble("threshold.search.rewrite-temperature", 0.2);
        int maxTokens = config.getInt("threshold.search.rewrite-max-tokens", 100);

        return aiService.call(prompt, temperature, maxTokens);
    }

    /** 调用 LLM 生成"假设文档"（HyDE 策略的关键步骤） */
    private String generateHypotheticalDocument(String query) {
        try {
            modelConfigService.getPrimaryChatModel();
        } catch (Exception e) {
            return null;
        }

        String prompt = PromptRegistry.SEARCH_HYDE.resolve(config, query);
        double temperature = config.getDouble("threshold.search.hyde-temperature", 0.3);
        int maxTokens = config.getInt("threshold.search.hyde-max-tokens", 500);

        return aiService.call(prompt, temperature, maxTokens);
    }

    /** 为指定文本生成嵌入向量 */
    private String generateEmbeddingForText(String text, ModelConfig embModel) {
        int embedTruncate = config.getInt("threshold.search.hyde-embedding-truncate", 8000);
        if (text.length() > embedTruncate) text = text.substring(0, embedTruncate);
        try {
            EmbeddingModel springAiModel = aiModelFactory.buildEmbeddingModel(embModel);
            float[] vector = springAiModel.embed(text);
            if (vector != null && vector.length > 0) {
                StringJoiner sj = new StringJoiner(",");
                        for (float v : vector) sj.add(String.valueOf(v));
                        return "[" + sj + "]";
            }
        } catch (Exception e) {
            log.warn("HyDE 向量生成失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * LLM 重排序：对初筛结果用 LLM 逐条评分（0-10），按分数降序取 topN
     * 如果 LLM 调用失败或返回异常，回退到取前 topN 条
     */
    public List<Map<String, Object>> rerankResults(String query, List<Map<String, Object>> results, int topN) {
        if (results == null || results.size() <= 1) return results;

        try {
            modelConfigService.getPrimaryChatModel();
        } catch (Exception e) {
            return results;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> item = results.get(i);
            String title = (String) item.getOrDefault("title", "");
            String content = (String) item.getOrDefault("content", "");
            String summary = (String) item.getOrDefault("summary", "");
            String display = summary != null && !summary.isBlank() ? summary : content;
            int truncateLen = config.getInt("threshold.search.rerank-truncate-length", 300);
            if (display.length() > truncateLen) display = display.substring(0, truncateLen);
            sb.append("--- 结果 ").append(i + 1).append(" ---\n");
            sb.append("标题: ").append(title).append("\n");
            sb.append("内容: ").append(display).append("\n\n");
        }

        String prompt = PromptRegistry.SEARCH_RERANK.resolve(config, query, sb.toString());

        try {
            double rerankTemp = config.getDouble("threshold.search.rerank-temperature", 0.1);
            int rerankMaxTokens = config.getInt("threshold.search.rerank-max-tokens", 200);
            String content = aiService.call(prompt, rerankTemp, rerankMaxTokens);

            if (content != null) {
                String cleaned = content.trim();
                int start = cleaned.indexOf('[');
                int end = cleaned.lastIndexOf(']');
                if (start >= 0 && end > start) {
                    cleaned = cleaned.substring(start, end + 1);
                    List<Integer> scores = objectMapper.readValue(cleaned,
                            new TypeReference<List<Integer>>() {});
                    if (scores.size() == results.size()) {
                        List<Map.Entry<Map<String, Object>, Integer>> scored = new ArrayList<>();
                        for (int i = 0; i < results.size(); i++) {
                            scored.add(Map.entry(results.get(i), scores.get(i)));
                        }
                        scored.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
                        List<Map<String, Object>> reranked = scored.stream()
                                .limit(topN)
                                .map(Map.Entry::getKey)
                                .toList();
                        log.info("LLM 重排序完成: {} 条 → {} 条", results.size(), reranked.size());
                        return reranked;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("LLM 重排序失败: {}", e.getMessage());
        }
        return results.stream().limit(topN).toList();
    }
}