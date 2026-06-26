package com.mindvault.systemconfig.service;

import com.mindvault.systemconfig.entity.SystemConfig;

import java.util.List;
import java.util.Map;

public interface SystemConfigService {

    void init();

    String getString(String key, String defaultVal);

    int getInt(String key, int defaultVal);

    long getLong(String key, long defaultVal);

    double getDouble(String key, double defaultVal);

    boolean getBool(String key, boolean defaultVal);

    String getPrompt(String key, String defaultPrompt);

    void set(String key, String value, String description, String valueType);

    void delete(String key);

    List<SystemConfig> listAll();

    List<SystemConfig> listByPrefix(String prefix);

    void refreshCache();

    String deriveModule(String key);

    String deriveGroup(String key);

    String getDefault(String key);

    SystemConfigDefaults.ValidationRule getValidation(String key);

    List<SystemConfigDefaults.TaskMeta> getScheduledTasks();

    Map<String, Object> getModules();

    Map<String, Object> getModuleDetail(String moduleId);

    Map<String, Object> getItemDetail(String key);
}