package com.mindvault.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.knowledge.service.KnowledgeService;
import com.mindvault.knowledge.service.SearchEnhanceService;
import com.mindvault.systemconfig.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 工具：语义搜索知识库。
 * <p>注册为 Spring AI Tool（名称 search_knowledge），供 LLM 在对话中检索相关知识。
 * 支持三种搜索策略：rewrite（查询改写+向量搜索）、hyde（HyDE 假设文档嵌入）、hybridSearch（混合搜索），
 * 通过系统配置 tool.search-knowledge.search-method 切换。
 * 输入为搜索关键词和数量限制，输出为匹配条目的 JSON 数组（含 ID、标题、摘要、相似度分数）。
 * 语义搜索失败时自动降级到 hybridSearch。</p>
 */
@Component
public class SearchKnowledgeTool {

    private static final Logger log = LoggerFactory.getLogger(SearchKnowledgeTool.class);

    private final KnowledgeService knowledgeService;
    private final SearchEnhanceService searchEnhanceService;
    private final SystemConfigService config;
    private final ObjectMapper objectMapper;

    public SearchKnowledgeTool(KnowledgeService knowledgeService,
                               SearchEnhanceService searchEnhanceService,
                               SystemConfigService config) {
        this.knowledgeService = knowledgeService;
        this.searchEnhanceService = searchEnhanceService;
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    @Tool(name = "search_knowledge", description = "在个人知识库中语义搜索与查询最相关的内容，返回匹配的知识条目（含标题、摘要和相似度）。适用于用户询问某个概念、话题时检索关联笔记辅助回答。")
    public String searchKnowledge(
            @ToolParam(description = "搜索关键词") String query,
            @ToolParam(description = "最多返回数量", required = false) Integer limit) {
        int defaultLimit = config.getInt("tool.search-knowledge.default-limit", 3);
        int maxLimit = config.getInt("tool.search-knowledge.max-limit", 10);
        int n = limit != null ? Math.min(limit, maxLimit) : defaultLimit;

        log.info("Agent 调用 search_knowledge: query={}, limit={}", query, n);

        try {
            String method = config.getString("tool.search-knowledge.search-method", "rewrite");
            List<Map<String, Object>> results;
            results = switch (method) {
                case "hyde" -> searchEnhanceService.hydeSearch(query, n);
                case "rewrite" -> searchEnhanceService.searchWithRewrite(query, n);
                default -> knowledgeService.hybridSearch(query, n);
            };

            if (results.isEmpty()) {
                return "[]";
            }

            List<Map<String, Object>> formatted = results.stream().map(k -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", k.get("id"));
                item.put("title", k.getOrDefault("title", ""));
                String summary = (String) k.getOrDefault("summary", "");
                String content = (String) k.getOrDefault("content", "");
                String display = summary != null && !summary.isBlank() ? summary : content;
                if (display != null && display.length() > 300) display = display.substring(0, 300);
                item.put("summary", display);
                Object sim = k.get("similarity");
                item.put("score", sim != null ? ((Number) sim).doubleValue() : 0.0);
                return item;
            }).toList();

            return objectMapper.writeValueAsString(formatted);
        } catch (Exception e) {
            log.warn("语义搜索失败，降级到混合搜索: {}", e.getMessage());
            try {
                List<Map<String, Object>> results = knowledgeService.hybridSearch(query, n);
                if (results.isEmpty()) return "[]";
                List<Map<String, Object>> formatted = results.stream().map(k -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", k.get("id"));
                    item.put("title", k.getOrDefault("title", ""));
                    String content = (String) k.getOrDefault("content", "");
                    item.put("summary", content != null && content.length() > 300 ? content.substring(0, 300) : content);
                    Object sim = k.get("similarity");
                    item.put("score", sim != null ? ((Number) sim).doubleValue() : 0.0);
                    return item;
                }).toList();
                return objectMapper.writeValueAsString(formatted);
            } catch (Exception e2) {
                log.warn("降级搜索也失败: {}", e2.getMessage());
                return "[]";
            }
        }
    }
}