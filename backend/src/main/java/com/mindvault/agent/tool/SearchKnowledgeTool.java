package com.mindvault.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.knowledge.SearchEnhanceService;
import com.mindvault.systemconfig.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SearchKnowledgeTool implements Tool {

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

    @Override
    public String getName() {
        return "search_knowledge";
    }

    @Override
    public String getDescription() {
        return "在个人知识库中**语义搜索**与查询最相关的内容，返回匹配的知识条目（含标题、摘要和相似度）。适用于用户询问某个概念、话题时检索关联笔记辅助回答。参数: query(搜索关键词), limit(最多返回数量，默认3)";
    }

    @Override
    @SuppressWarnings("unchecked")
    public String execute(Map<String, Object> args) {
        String query = (String) args.get("query");
        int defaultLimit = config.getInt("tool.search-knowledge.default-limit", 3);
        int maxLimit = config.getInt("tool.search-knowledge.max-limit", 10);
        int limit = args.containsKey("limit") ? Math.min(((Number) args.get("limit")).intValue(), maxLimit) : defaultLimit;

        log.info("Agent 调用 search_knowledge: query={}, limit={}", query, limit);

        try {
            String method = config.getString("tool.search-knowledge.search-method", "rewrite");
            List<Map<String, Object>> results;
            results = switch (method) {
                case "hyde" -> searchEnhanceService.hydeSearch(query, limit);
                case "rewrite" -> searchEnhanceService.searchWithRewrite(query, limit);
                default -> knowledgeService.hybridSearch(query, limit);
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
                List<Map<String, Object>> results = knowledgeService.hybridSearch(query, limit);
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