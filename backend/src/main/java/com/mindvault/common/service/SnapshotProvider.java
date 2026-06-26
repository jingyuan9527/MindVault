package com.mindvault.common.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据快照提供者
 *
 * 通过 MyBatis Mapper 查询指定实体在某一时刻的完整状态，序列化为 JSON 字符串。
 * 配合 SnapshotRegistryConfig 在启动时注册实体 ↔ Mapper 映射，
 * 由 OperationLogAspect 在操作日志中自动录制操作前后的数据快照。
 *
 * 线程安全：使用 ConcurrentHashMap 存储 Mapper 映射。
 */
@Component
public class SnapshotProvider {

    private static final Logger log = LoggerFactory.getLogger(SnapshotProvider.class);
    private final Map<Class<?>, BaseMapper<?>> mappers = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    /** 注册实体类型对应的 Mapper（由 SnapshotRegistryConfig 在启动时调用） */
    public <T> void register(Class<T> entityType, BaseMapper<T> mapper) {
        mappers.put(entityType, mapper);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    /** 获取指定实体的数据快照（JSON 格式），查询失败返回 null */
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