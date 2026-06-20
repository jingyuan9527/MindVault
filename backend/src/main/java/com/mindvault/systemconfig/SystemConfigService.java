package com.mindvault.systemconfig;

import com.mindvault.systemconfig.entity.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SystemConfigService {

    private static final Logger log = LoggerFactory.getLogger(SystemConfigService.class);

    private final SystemConfigMapper mapper;
    private volatile Map<String, String> cache;
    private long lastCacheRefresh = 0;

    public SystemConfigService(SystemConfigMapper mapper) {
        this.mapper = mapper;
        this.cache = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void init() {
        refreshCache();
    }

    public String getString(String key, String defaultVal) {
        String val = getFromCache(key);
        return val != null ? val : defaultVal;
    }

    public int getInt(String key, int defaultVal) {
        String val = getFromCache(key);
        if (val == null) return defaultVal;
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return defaultVal; }
    }

    public long getLong(String key, long defaultVal) {
        String val = getFromCache(key);
        if (val == null) return defaultVal;
        try { return Long.parseLong(val); } catch (NumberFormatException e) { return defaultVal; }
    }

    public double getDouble(String key, double defaultVal) {
        String val = getFromCache(key);
        if (val == null) return defaultVal;
        try { return Double.parseDouble(val); } catch (NumberFormatException e) { return defaultVal; }
    }

    public boolean getBool(String key, boolean defaultVal) {
        String val = getFromCache(key);
        if (val == null) return defaultVal;
        return "true".equalsIgnoreCase(val) || "1".equals(val);
    }

    public String getPrompt(String key, String defaultPrompt) {
        return getString(key, defaultPrompt);
    }

    public void set(String key, String value, String description, String valueType) {
        SystemConfig existing = mapper.findByKey(key);
        if (existing != null) {
            existing.setConfigValue(value);
            existing.setDescription(description);
            if (valueType != null) existing.setValueType(valueType);
            existing.setUpdatedAt(LocalDateTime.now());
            mapper.updateById(existing);
        } else {
            SystemConfig cfg = new SystemConfig();
            cfg.setConfigKey(key);
            cfg.setConfigValue(value);
            cfg.setDescription(description);
            cfg.setValueType(valueType != null ? valueType : "string");
            cfg.setUpdatedAt(LocalDateTime.now());
            mapper.insert(cfg);
        }
        cache.put(key, value);
    }

    public void delete(String key) {
        SystemConfig existing = mapper.findByKey(key);
        if (existing != null) mapper.deleteById(existing.getId());
        cache.remove(key);
    }

    public List<SystemConfig> listAll() {
        return mapper.selectList(null);
    }

    public List<SystemConfig> listByPrefix(String prefix) {
        return mapper.selectList(null).stream()
                .filter(c -> c.getConfigKey().startsWith(prefix))
                .toList();
    }

    public void refreshCache() {
        Map<String, String> newCache = new ConcurrentHashMap<>();
        List<SystemConfig> all = mapper.selectList(null);
        for (SystemConfig c : all) {
            newCache.put(c.getConfigKey(), c.getConfigValue());
        }
        cache = newCache;
        lastCacheRefresh = System.currentTimeMillis();
        log.info("系统配置缓存已刷新: {} 条", newCache.size());
    }

    private String getFromCache(String key) {
        String val = cache.get(key);
        if (val == null && System.currentTimeMillis() - lastCacheRefresh > 30000) {
            refreshCache();
            val = cache.get(key);
        }
        return val;
    }
}
