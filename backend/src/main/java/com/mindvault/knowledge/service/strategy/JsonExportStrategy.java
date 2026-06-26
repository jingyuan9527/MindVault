package com.mindvault.knowledge.service.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.common.service.MetricsService;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.systemconfig.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class JsonExportStrategy implements ExportFormatStrategy {

    private static final Logger log = LoggerFactory.getLogger(JsonExportStrategy.class);

    private final SystemConfigService config;
    private final MetricsService metricsService;
    private final ObjectMapper objectMapper;

    public JsonExportStrategy(SystemConfigService config, MetricsService metricsService) {
        this.config = config;
        this.metricsService = metricsService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getFormat() {
        return "json";
    }

    @Override
    public byte[] export(List<Knowledge> items) {
        try {
            List<Map<String, Object>> exportList = new ArrayList<>();
            for (Knowledge k : items) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("title", k.getTitle()); item.put("aiTitle", k.getAiTitle());
                item.put("content", k.getContent()); item.put("contentType", k.getContentType());
                item.put("sourceUrl", k.getSourceUrl()); item.put("summary", k.getSummary());
                item.put("tags", k.getTags()); item.put("userTags", k.getUserTags());
                item.put("createdAt", k.getCreatedAt() != null ? k.getCreatedAt().toString() : null);
                item.put("updatedAt", k.getUpdatedAt() != null ? k.getUpdatedAt().toString() : null);
                exportList.add(item);
            }
            Map<String, Object> exportData = new LinkedHashMap<>();
            exportData.put("version", config.getString("default.export.version", "0.4.0"));
            exportData.put("exportedAt", LocalDateTime.now().toString());
            exportData.put("count", exportList.size());
            exportData.put("items", exportList);
            metricsService.recordExport("json");
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportData).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("JSON导出失败: {}", e.getMessage());
            return "[]".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @Override
    public boolean isApplicable(List<Knowledge> items) {
        return true;
    }
}