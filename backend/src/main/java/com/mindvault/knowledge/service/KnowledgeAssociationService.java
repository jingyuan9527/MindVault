package com.mindvault.knowledge.service;

import java.util.List;
import java.util.Map;

public interface KnowledgeAssociationService {

    List<Map<String, Object>> getRelatedKnowledge(Long knowledgeId, int limit);

    void scheduledAssociationDiscovery();
}