package com.mindvault.knowledge.service.strategy;

import java.util.List;
import java.util.Map;

public interface SearchStrategy {
    boolean isApplicable();
    List<Map<String, Object>> search(String query, int limit);
    String name();
}