package com.mindvault.knowledge.service;

import java.util.List;
import java.util.Map;

public interface TagService {
    void batchTag(List<Long> ids, String tag);
    int batchAiTag(List<Long> ids);
    void updateTags(Long id, List<String> tags);
    List<Map<String, Object>> getAllTags();
    String mergeTags(String aiTags, String userTags);
}