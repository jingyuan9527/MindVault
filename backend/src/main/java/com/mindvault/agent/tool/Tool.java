package com.mindvault.agent.tool;

import java.util.Map;

public interface Tool {
    String getName();
    String getDescription();
    String execute(Map<String, Object> args);
}