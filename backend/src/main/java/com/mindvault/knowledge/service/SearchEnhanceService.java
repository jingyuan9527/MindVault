package com.mindvault.knowledge.service;

import java.util.List;
import java.util.Map;

public interface SearchEnhanceService {

    List<Map<String, Object>> hydeSearch(String query, int topN);

    List<Map<String, Object>> searchWithRewrite(String query, int topN);

    /**
     * 查询改写搜索（支持 offset 分页）。
     * <p>语义：返回按相关度排序的结果中 [offset, offset+topN) 区间的条目。
     * 内部先取 topN+offset 的候选池，重排后 skip(offset).limit(topN)。</p>
     */
    List<Map<String, Object>> searchWithRewrite(String query, int topN, int offset);

    List<Map<String, Object>> rerankResults(String query, List<Map<String, Object>> results, int topN);
}