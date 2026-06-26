package com.mindvault.knowledge.service;

import com.mindvault.common.dto.PageResult;
import com.mindvault.knowledge.entity.Knowledge;

import java.util.List;
import java.util.Map;

public interface KnowledgeService {

    Knowledge addKnowledge(Knowledge knowledge);

    List<Map<String, Object>> searchSimilar(String embedding, int topN);

    PageResult<Knowledge> listAll(int page, int size, String keyword, List<String> tags, String sortBy, String sortOrder);

    List<Knowledge> listAllSimple(int page, int size);

    Knowledge getById(Long id);

    List<Map<String, Object>> hybridSearch(String query, int limit);

    List<Map<String, Object>> keywordSearchWithRank(String query, int limit);

    String displayTitle(Knowledge k);

    List<Knowledge> searchByKeyword(String query, int limit);

    List<Knowledge> searchByKeywordWithTag(String query, int limit, String tag);

    Knowledge updateKnowledge(Long id, Knowledge updated);

    Knowledge updateAiFields(Long id, String aiTitle, String aiTags);

    void updateEmbedding(Long id, String embedding);

    void deleteKnowledge(Long id);

    void updateAutoProcessStatus(Long id, String status);

    void reprocessKnowledge(Long id);

    void batchDelete(List<Long> ids);
}