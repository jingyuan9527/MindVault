package com.mindvault.systemconfig.service;

import com.mindvault.systemconfig.entity.SystemConfig;
import com.mindvault.systemconfig.mapper.SystemConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final Logger log = LoggerFactory.getLogger(SystemConfigServiceImpl.class);

    private final SystemConfigMapper mapper;
    private volatile Map<String, String> cache;
    private long lastCacheRefresh = 0;

    public SystemConfigServiceImpl(SystemConfigMapper mapper) {
        this.mapper = mapper;
        this.cache = new ConcurrentHashMap<>();
    }

    @PostConstruct
    @Override
    public void init() {
        refreshCache();
    }

    @Override
    public String getString(String key, String defaultVal) {
        String val = getFromCache(key);
        return val != null ? val : defaultVal;
    }

    @Override
    public int getInt(String key, int defaultVal) {
        String val = getFromCache(key);
        if (val == null) return defaultVal;
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return defaultVal; }
    }

    @Override
    public long getLong(String key, long defaultVal) {
        String val = getFromCache(key);
        if (val == null) return defaultVal;
        try { return Long.parseLong(val); } catch (NumberFormatException e) { return defaultVal; }
    }

    @Override
    public double getDouble(String key, double defaultVal) {
        String val = getFromCache(key);
        if (val == null) return defaultVal;
        try { return Double.parseDouble(val); } catch (NumberFormatException e) { return defaultVal; }
    }

    @Override
    public boolean getBool(String key, boolean defaultVal) {
        String val = getFromCache(key);
        if (val == null) return defaultVal;
        return "true".equalsIgnoreCase(val) || "1".equals(val);
    }

    @Override
    public String getPrompt(String key, String defaultPrompt) {
        return getString(key, defaultPrompt);
    }

    @Override
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

    @Override
    public void delete(String key) {
        SystemConfig existing = mapper.findByKey(key);
        if (existing != null) mapper.deleteById(existing.getId());
        cache.remove(key);
    }

    @Override
    public List<SystemConfig> listAll() {
        return mapper.selectList(null);
    }

    @Override
    public List<SystemConfig> listByPrefix(String prefix) {
        return mapper.selectList(null).stream()
                .filter(c -> c.getConfigKey().startsWith(prefix))
                .toList();
    }

    @Override
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

    @Override
    public String deriveModule(String key) {
        return SystemConfigDefaults.deriveModule(key);
    }

    @Override
    public String deriveGroup(String key) {
        return SystemConfigDefaults.deriveGroup(key);
    }

    @Override
    public String getDefault(String key) {
        return SystemConfigDefaults.DEFAULTS.get(key);
    }

    @Override
    public SystemConfigDefaults.ValidationRule getValidation(String key) {
        return SystemConfigDefaults.VALIDATION_RULES.get(key);
    }

    @Override
    public List<SystemConfigDefaults.TaskMeta> getScheduledTasks() {
        return SystemConfigDefaults.SCHEDULED_TASKS;
    }

    @Override
    public Map<String, Object> getModules() {
        List<SystemConfig> all = listAll();
        Map<String, List<SystemConfig>> byModule = all.stream()
                .collect(Collectors.groupingBy(c -> deriveModule(c.getConfigKey())));

        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> modulesList = new ArrayList<>();

        for (var moduleEntry : SystemConfigDefaults.MODULE_LABELS.entrySet()) {
            String moduleId = moduleEntry.getKey();
            String moduleLabel = moduleEntry.getValue();
            List<SystemConfig> configsInModule = byModule.getOrDefault(moduleId, List.of());

            Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<>();
            for (SystemConfig cfg : configsInModule) {
                String groupId = deriveGroup(cfg.getConfigKey());
                groups.computeIfAbsent(groupId, k -> new ArrayList<>())
                        .add(buildItemMap(cfg));
            }

            Map<String, Object> moduleMap = new LinkedHashMap<>();
            moduleMap.put("id", moduleId);
            moduleMap.put("label", moduleLabel);
            List<Map<String, Object>> groupsList = new ArrayList<>();
            for (var gEntry : groups.entrySet()) {
                Map<String, Object> gMap = new LinkedHashMap<>();
                gMap.put("id", gEntry.getKey());
                gMap.put("label", groupLabel(gEntry.getKey()));
                gMap.put("items", gEntry.getValue());
                groupsList.add(gMap);
            }
            moduleMap.put("groups", groupsList);
            modulesList.add(moduleMap);
        }
        result.put("modules", modulesList);
        return result;
    }

    @Override
    public Map<String, Object> getModuleDetail(String moduleId) {
        List<SystemConfig> all = listAll();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", moduleId);
        result.put("label", SystemConfigDefaults.MODULE_LABELS.getOrDefault(moduleId, moduleId));

        Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<>();
        for (SystemConfig cfg : all) {
            if (moduleId.equals(deriveModule(cfg.getConfigKey()))) {
                String groupId = deriveGroup(cfg.getConfigKey());
                groups.computeIfAbsent(groupId, k -> new ArrayList<>())
                        .add(buildItemMap(cfg));
            }
        }
        List<Map<String, Object>> groupsList = new ArrayList<>();
        for (var gEntry : groups.entrySet()) {
            Map<String, Object> gMap = new LinkedHashMap<>();
            gMap.put("id", gEntry.getKey());
            gMap.put("label", groupLabel(gEntry.getKey()));
            gMap.put("items", gEntry.getValue());
            groupsList.add(gMap);
        }
        result.put("groups", groupsList);
        return result;
    }

    @Override
    public Map<String, Object> getItemDetail(String key) {
        SystemConfig cfg = mapper.findByKey(key);
        if (cfg == null) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("key", key);
            item.put("value", SystemConfigDefaults.DEFAULTS.get(key));
            item.put("defaultValue", SystemConfigDefaults.DEFAULTS.get(key));
            item.put("valueType", "string");
            item.put("description", "");
            return item;
        }
        return buildItemMap(cfg);
    }

    private Map<String, Object> buildItemMap(SystemConfig cfg) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", cfg.getConfigKey());
        item.put("value", cfg.getConfigValue());
        item.put("defaultValue", SystemConfigDefaults.DEFAULTS.get(cfg.getConfigKey()));
        item.put("valueType", cfg.getValueType());
        item.put("description", cfg.getDescription());
        item.put("module", deriveModule(cfg.getConfigKey()));
        item.put("group", deriveGroup(cfg.getConfigKey()));
        var validation = SystemConfigDefaults.VALIDATION_RULES.get(cfg.getConfigKey());
        if (validation != null) {
            Map<String, Object> vMap = new LinkedHashMap<>();
            vMap.put("valueType", validation.valueType());
            vMap.put("min", validation.min());
            vMap.put("max", validation.max());
            vMap.put("options", validation.options());
            item.put("validation", vMap);
        }
        if (cfg.getUpdatedAt() != null) {
            item.put("updatedAt", cfg.getUpdatedAt().toString());
        }
        return item;
    }

    private static String groupLabel(String groupId) {
        return switch (groupId) {
            case "prompt" -> "提示词";
            case "threshold" -> "阈值参数";
            case "default" -> "默认值";
            case "task" -> "定时任务";
            case "other" -> "其他";
            default -> groupId;
        };
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