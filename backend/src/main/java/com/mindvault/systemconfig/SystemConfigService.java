package com.mindvault.systemconfig;

import com.mindvault.systemconfig.entity.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 系统配置服务。
 * <p>
 * 核心职责: 管理系统配置的读取（带缓存）、写入、删除和结构化查询。
 * 提供类型安全的 getString/getInt/getLong/getDouble/getBool 方法，支持默认值兜底。
 * </p>
 * <p>
 * 关键设计:
 * <ul>
 *   <li>缓存机制: 使用 volatile ConcurrentHashMap 缓存全部配置，启动时全量加载</li>
 *   <li>缓存自动刷新: 缓存 miss 且距离上次刷新超过 30 秒时自动从 DB 重新加载</li>
 *   <li>结构化 API: getModules() / getModuleDetail() / getItemDetail() 支持按模块分组的结构化配置（前端配置页面使用）</li>
 *   <li>默认值委托: 所有类型安全的 getter 方法均委托 SystemConfigDefaults 作为最终 fallback</li>
 * </ul>
 * </p>
 * <p>依赖: SystemConfigMapper, SystemConfigDefaults</p>
 */
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

    /**
     * 初始化: 服务启动时从 DB 加载全量配置到缓存。
     */
    @PostConstruct
    public void init() {
        refreshCache();
    }

    /**
     * 获取字符串类型配置值。
     * @param key        配置键
     * @param defaultVal 默认值（缓存 miss 时返回）
     * @return 配置值或默认值
     */
    public String getString(String key, String defaultVal) {
        String val = getFromCache(key);
        return val != null ? val : defaultVal;
    }

    /**
     * 获取整数类型配置值。
     * @param key        配置键
     * @param defaultVal 默认值
     * @return 配置值，解析失败时返回默认值
     */
    public int getInt(String key, int defaultVal) {
        String val = getFromCache(key);
        if (val == null) return defaultVal;
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return defaultVal; }
    }

    /**
     * 获取长整数类型配置值。
     * @param key        配置键
     * @param defaultVal 默认值
     * @return 配置值，解析失败时返回默认值
     */
    public long getLong(String key, long defaultVal) {
        String val = getFromCache(key);
        if (val == null) return defaultVal;
        try { return Long.parseLong(val); } catch (NumberFormatException e) { return defaultVal; }
    }

    /**
     * 获取浮点数类型配置值。
     * @param key        配置键
     * @param defaultVal 默认值
     * @return 配置值，解析失败时返回默认值
     */
    public double getDouble(String key, double defaultVal) {
        String val = getFromCache(key);
        if (val == null) return defaultVal;
        try { return Double.parseDouble(val); } catch (NumberFormatException e) { return defaultVal; }
    }

    /**
     * 获取布尔类型配置值。
     * @param key        配置键
     * @param defaultVal 默认值
     * @return true 当值为 "true" 或 "1" 时，否则返回默认值
     */
    public boolean getBool(String key, boolean defaultVal) {
        String val = getFromCache(key);
        if (val == null) return defaultVal;
        return "true".equalsIgnoreCase(val) || "1".equals(val);
    }

    /**
     * 获取提示词模板。
     * @param key           配置键
     * @param defaultPrompt 默认提示词模板
     * @return 提示词模板字符串
     */
    public String getPrompt(String key, String defaultPrompt) {
        return getString(key, defaultPrompt);
    }

    /**
     * 创建或更新配置项。
     * 如果 key 已存在则更新值/描述/类型，否则新增一条配置。
     * 写入后同步更新缓存。
     * @param key         配置键
     * @param value       配置值
     * @param description 描述
     * @param valueType   值类型（string/int/bool/cron/prompt），为 null 时保持原值或默认 "string"
     */
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

    /**
     * 删除配置项。同时从缓存中移除。
     * @param key 配置键
     */
    public void delete(String key) {
        SystemConfig existing = mapper.findByKey(key);
        if (existing != null) mapper.deleteById(existing.getId());
        cache.remove(key);
    }

    /**
     * 获取所有配置项。
     * @return 全量配置列表（直接从 DB 读取，不走缓存）
     */
    public List<SystemConfig> listAll() {
        return mapper.selectList(null);
    }

    /**
     * 按配置键前缀筛选配置项。
     * @param prefix 键前缀
     * @return 匹配的配置列表
     */
    public List<SystemConfig> listByPrefix(String prefix) {
        return mapper.selectList(null).stream()
                .filter(c -> c.getConfigKey().startsWith(prefix))
                .toList();
    }

    /**
     * 从 DB 重新加载全量配置到缓存。
     * 通常在配置项经过外部修改后手动调用。
     */
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

    // --- New: module metadata & structured API ---

    /**
     * 推导配置项所属模块，委托给 SystemConfigDefaults.deriveModule。
     * @param key 配置键
     * @return 模块 ID
     */
    public String deriveModule(String key) {
        return SystemConfigDefaults.deriveModule(key);
    }

    /**
     * 推导配置项所属分组，委托给 SystemConfigDefaults.deriveGroup。
     * @param key 配置键
     * @return 分组 ID
     */
    public String deriveGroup(String key) {
        return SystemConfigDefaults.deriveGroup(key);
    }

    /**
     * 获取配置项的默认值。
     * @param key 配置键
     * @return 默认值，无默认值则返回 null
     */
    public String getDefault(String key) {
        return SystemConfigDefaults.DEFAULTS.get(key);
    }

    /**
     * 获取配置项的校验规则。
     * @param key 配置键
     * @return 校验规则，无校验规则则返回 null
     */
    public SystemConfigDefaults.ValidationRule getValidation(String key) {
        return SystemConfigDefaults.VALIDATION_RULES.get(key);
    }

    /**
     * 获取所有定时任务的元数据列表。
     * @return 定时任务元数据列表
     */
    public List<SystemConfigDefaults.TaskMeta> getScheduledTasks() {
        return SystemConfigDefaults.SCHEDULED_TASKS;
    }

    /**
     * 获取按模块分组的结构化配置数据（供前端配置页面使用）。
     * 每个模块包含多个分组（prompt/threshold/default/task），每个分组包含配置项列表。
     * @return 结构化配置数据
     */
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

    /**
     * 获取指定模块的所有配置项详情。
     * @param moduleId 模块 ID
     * @return 该模块的结构化数据（含分组和配置项列表）
     */
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

    /**
     * 获取配置项详情（含值、默认值、校验规则、模块/分组归属）。
     * 如果 DB 中不存在该 key，则以默认值构造一个虚拟配置项。
     * @param key 配置键
     * @return 配置项详情 Map
     */
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

    /**
     * 将 SystemConfig 实体转换为前端可用的结构化 Map。
     * 包含值、默认值、校验规则、模块/分组归属和更新时间。
     * @param cfg 配置实体
     * @return 配置项 Map
     */
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

    /**
     * 将分组 ID 转换为中文显示标签。
     */
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

    /**
     * 从缓存中读取配置值。
     * 如果缓存 miss 且距离上次刷新超过 30 秒，则自动触发一次缓存刷新。
     * @param key 配置键
     * @return 缓存中的配置值，不存在则返回 null
     */
    private String getFromCache(String key) {
        String val = cache.get(key);
        if (val == null && System.currentTimeMillis() - lastCacheRefresh > 30000) {
            refreshCache();
            val = cache.get(key);
        }
        return val;
    }
}
