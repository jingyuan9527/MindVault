package com.mindvault.knowledge.service;

import java.util.List;
import java.util.Map;

public interface SearchEnhanceService {

    List<Map<String, Object>> hydeSearch(String query, int topN);

    List<Map<String, Object>> searchWithRewrite(String query, int topN);

    List<Map<String, Object>> rerankResults(String query, List<Map<String, Object>> results, int topN);
}