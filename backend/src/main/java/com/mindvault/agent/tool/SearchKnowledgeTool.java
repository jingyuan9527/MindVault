package com.mindvault.agent.tool;

import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.knowledge.entity.Knowledge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SearchKnowledgeTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(SearchKnowledgeTool.class);

    private final KnowledgeService knowledgeService;

    public SearchKnowledgeTool(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @Override
    public String getName() {
        return "search_knowledge";
    }

    @Override
    public String getDescription() {
        return "在个人知识库中搜索与查询相关的内容，返回匹配的知识条目列表。参数: query(搜索关键词), topN(最多返回数量，默认5)";
    }

    @Override
    @SuppressWarnings("unchecked")
    public String execute(Map<String, Object> args) {
        String query = (String) args.get("query");
        int topN = args.containsKey("topN") ? ((Number) args.get("topN")).intValue() : 5;
        log.info("Agent 调用 search_knowledge: query={}, topN={}", query, topN);

        try {
            List<Knowledge> knowledgeList = knowledgeService.searchByKeyword(query, topN);

            if (knowledgeList.isEmpty()) {
                return "未找到与「" + query + "」相关的知识。";
            }

            return knowledgeList.stream()
                    .map(k -> String.format("- [%d] %s\n  %s",
                            k.getId(), k.getTitle(),
                            k.getContent().length() > 200
                                    ? k.getContent().substring(0, 200) + "..."
                                    : k.getContent()))
                    .collect(Collectors.joining("\n\n"));
        } catch (Exception e) {
            log.warn("搜索失败，降级到空结果: {}", e.getMessage());
            return "搜索时遇到问题，请稍后重试。";
        }
    }
}