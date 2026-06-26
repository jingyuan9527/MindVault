package com.mindvault.knowledge.service.strategy;

import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.knowledge.mapper.KnowledgeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class KeywordSearchStrategy implements SearchStrategy {

    private static final Logger log = LoggerFactory.getLogger(KeywordSearchStrategy.class);

    private final KnowledgeMapper mapper;

    public KeywordSearchStrategy(KnowledgeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean isApplicable() {
        return true;
    }

    @Override
    public List<Map<String, Object>> search(String query, int limit) {
        return keywordSearchWithRank(query, limit);
    }

    @Override
    public String name() {
        return "keyword";
    }

    private List<Map<String, Object>> keywordSearchWithRank(String query, int limit) {
        List<Knowledge> results = mapper.keywordSearch(query, limit);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Knowledge k : results) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", k.getId()); item.put("title", k.getTitle()); item.put("aiTitle", k.getAiTitle());
            item.put("content", k.getContent()); item.put("summary", k.getSummary());
            item.put("tags", k.getTags()); item.put("userTags", k.getUserTags());
            item.put("contentType", k.getContentType()); item.put("sourceUrl", k.getSourceUrl());
            item.put("createdAt", k.getCreatedAt());
            list.add(item);
        }
        return list;
    }
}