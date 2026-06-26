package com.mindvault.knowledge.service.strategy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.knowledge.config.ImportExportProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class CsvExportStrategy implements ExportFormatStrategy {

    private static final Logger log = LoggerFactory.getLogger(CsvExportStrategy.class);

    private final ImportExportProperties importExportProperties;
    private final ObjectMapper objectMapper;

    public CsvExportStrategy(ImportExportProperties importExportProperties) {
        this.importExportProperties = importExportProperties;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getFormat() {
        return "csv";
    }

    @Override
    public byte[] export(List<Knowledge> items) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(importExportProperties.getCsvHeader()).append("\n");
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (Knowledge k : items) {
                String tags = "";
                String merged = mergeTags(k.getTags(), k.getUserTags());
                if (merged != null && !merged.equals("[]")) {
                    try { tags = String.join("; ", objectMapper.readValue(merged, new TypeReference<List<String>>() {})); }
                    catch (Exception e) { log.warn("CSV 导出标签解析失败: id={}", k.getId(), e); }
                }
                sb.append(escapeCsv(displayTitle(k))).append(",");
                sb.append(escapeCsv(k.getContent())).append(",");
                sb.append(escapeCsv(k.getContentType())).append(",");
                sb.append(escapeCsv(k.getSummary())).append(",");
                sb.append(escapeCsv(tags)).append(",");
                sb.append(escapeCsv(k.getSourceUrl())).append(",");
                sb.append(k.getCreatedAt() != null ? k.getCreatedAt().format(dtf) : "").append("\n");
            }
            return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("CSV导出失败: {}", e.getMessage());
            return "标题,内容,类型,摘要,标签,来源,创建时间\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @Override
    public boolean isApplicable(List<Knowledge> items) {
        return true;
    }

    private String displayTitle(Knowledge k) {
        return k.getAiTitle() != null && !k.getAiTitle().isBlank() ? k.getAiTitle() : k.getTitle();
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) return "\"" + value.replace("\"", "\"\"") + "\"";
        return value;
    }

    private String mergeTags(String aiTags, String userTags) {
        try {
            Set<String> merged = new LinkedHashSet<>();
            if (aiTags != null && !aiTags.equals("[]")) merged.addAll(objectMapper.readValue(aiTags, new TypeReference<List<String>>() {}));
            if (userTags != null && !userTags.equals("[]")) merged.addAll(objectMapper.readValue(userTags, new TypeReference<List<String>>() {}));
            return objectMapper.writeValueAsString(new ArrayList<>(merged));
        } catch (Exception e) { return "[]"; }
    }
}