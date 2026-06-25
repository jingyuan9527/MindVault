package com.mindvault.common.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SnapshotProvider {

    private static final Logger log = LoggerFactory.getLogger(SnapshotProvider.class);
    private final Map<Class<?>, BaseMapper<?>> mappers = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public <T> void register(Class<T> entityType, BaseMapper<T> mapper) {
        mappers.put(entityType, mapper);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public String getSnapshot(Class<?> entityType, String entityId) {
        if (entityType == null || entityType == Void.class || entityId == null) return null;
        BaseMapper mapper = mappers.get(entityType);
        if (mapper == null) return null;
        try {
            Object entity = mapper.selectById(entityId);
            if (entity == null) return null;
            return objectMapper.writeValueAsString(entity);
        } catch (Exception e) {
            log.warn("获取快照失败 [{}#{}]: {}", entityType.getSimpleName(), entityId, e.getMessage());
            return null;
        }
    }
}