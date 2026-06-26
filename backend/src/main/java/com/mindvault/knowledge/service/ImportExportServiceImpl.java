package com.mindvault.knowledge.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.auto.service.AutoProcessService;
import com.mindvault.common.service.MetricsService;
import com.mindvault.knowledge.dto.ImportPreview;
import com.mindvault.knowledge.dto.ImportPreview.ConflictItem;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.knowledge.mapper.KnowledgeMapper;
import com.mindvault.operationlog.service.OperationLogService;
import com.mindvault.systemconfig.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ImportExportServiceImpl implements ImportExportService {

    private static final Logger log = LoggerFactory.getLogger(ImportExportServiceImpl.class);

    private final KnowledgeMapper mapper;
    private final SystemConfigService config;
    private final OperationLogService operationLogService;
    private final MetricsService metricsService;
    private final AutoProcessService autoProcessService;
    private final ObjectMapper objectMapper;

    public ImportExportServiceImpl(KnowledgeMapper mapper,
                                   SystemConfigService config,
                                   OperationLogService operationLogService,
                                   MetricsService metricsService,
                                   AutoProcessService autoProcessService) {
        this.mapper = mapper;
        this.config = config;
        this.operationLogService = operationLogService;
        this.metricsService = metricsService;
        this.autoProcessService = autoProcessService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String batchExport(List<Long> ids) {
        try {
            List<Knowledge> selected = mapper.selectBatchIds(ids);
            List<Map<String, Object>> exportList = new ArrayList<>();
            for (Knowledge k : selected) {
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
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportData);
        } catch (Exception e) {
            log.error("批量导出失败: {}", e.getMessage());
            return "[]";
        }
    }

    @Override
    public String exportAllAsJson() {
        try {
            List<Knowledge> all = mapper.selectList(null);
            List<Map<String, Object>> exportList = new ArrayList<>();
            for (Knowledge k : all) {
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
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportData);
        } catch (Exception e) {
            log.error("导出失败: {}", e.getMessage());
            return "[]";
        }
    }

    @Override
    public byte[] exportAllAsMarkdown() {
        try {
            List<Knowledge> all = mapper.selectList(null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (Knowledge k : all) {
                String displayName = displayTitle(k);
                String safeName = displayName.replaceAll("[/\\\\:*?\"<>|]", "_").replaceAll("\\s+", "_");
                int maxFilenameLen = config.getInt("threshold.export.max-filename-length", 80);
                if (safeName.length() > maxFilenameLen) safeName = safeName.substring(0, maxFilenameLen);
                List<String> tagList = new ArrayList<>();
                String mergedTags = mergeTags(k.getTags(), k.getUserTags());
                if (mergedTags != null && !mergedTags.equals("[]")) {
                    try { tagList = objectMapper.readValue(mergedTags, new TypeReference<List<String>>() {}); }
                    catch (Exception e) { /* ignore */ }
                }
                String tagsSection = tagList.isEmpty() ? "" : "\n\n标签: " + String.join(", ", tagList);
                String summarySection = k.getSummary() != null && !k.getSummary().isBlank() ? "\n\n> " + k.getSummary().replaceAll("\n", "\n> ") : "";
                String sourceSection = k.getSourceUrl() != null && !k.getSourceUrl().isBlank() ? "\n\n来源: " + k.getSourceUrl() : "";
                String dateSection = "\n\n---\n创建于 " + (k.getCreatedAt() != null ? k.getCreatedAt().format(dtf) : "未知");
                String md = "---\ntitle: " + displayName + "\ntype: " + k.getContentType()
                        + "\ncreated: " + (k.getCreatedAt() != null ? k.getCreatedAt().format(dtf) : "")
                        + (k.getUpdatedAt() != null ? "\nupdated: " + k.getUpdatedAt().format(dtf) : "")
                        + (k.getSourceUrl() != null ? "\nsource: " + k.getSourceUrl() : "")
                        + (k.getSummary() != null ? "\nsummary: " + k.getSummary() : "")
                        + "\n---\n\n# " + displayName + summarySection + tagsSection + sourceSection + "\n\n" + k.getContent() + dateSection + "\n";
                String untaggedFolder = config.getString("default.export.markdown-untagged-folder", "未分类");
                String folder = tagList.isEmpty() ? untaggedFolder : sanitizeFolderName(tagList.get(0));
                zos.putNextEntry(new ZipEntry(folder + "/" + safeName + ".md"));
                zos.write(md.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zos.closeEntry();
            }
            zos.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Markdown导出失败: {}", e.getMessage());
            return new byte[0];
        }
    }

    @Override
    public String exportAllAsCsv() {
        try {
            List<Knowledge> all = mapper.selectList(null);
            StringBuilder sb = new StringBuilder();
            sb.append(config.getString("default.export.csv-header", "标题,内容,类型,摘要,标签,来源,创建时间")).append("\n");
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (Knowledge k : all) {
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
            return sb.toString();
        } catch (Exception e) {
            log.error("CSV导出失败: {}", e.getMessage());
            return "标题,内容,类型,摘要,标签,来源,创建时间\n";
        }
    }

    @Override
    public ImportPreview previewImport(String json) {
        try {
            Map<String, Object> importData = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            Object itemsObj = importData.get("items");
            if (itemsObj == null) return new ImportPreview(0, 0, 0, List.of());
            List<Map<String, Object>> items = extractItems(itemsObj);
            List<ConflictItem> conflicts = new ArrayList<>();
            int conflictCount = 0;
            for (int i = 0; i < items.size(); i++) {
                String title = (String) items.get(i).getOrDefault("title", config.getString("default.knowledge.import-default-title", "未命名"));
                List<Knowledge> existing = mapper.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(title, "");
                if (!existing.isEmpty()) { conflicts.add(new ConflictItem(i, title, existing.get(0).getTitle())); conflictCount++; }
            }
            return new ImportPreview(items.size(), items.size() - conflictCount, conflictCount, conflicts);
        } catch (Exception e) {
            log.error("导入预览失败: {}", e.getMessage());
            return new ImportPreview(0, 0, 0, List.of());
        }
    }

    @Transactional
    @Override
    public int importFromJson(String json) { return importFromJsonWithConflict(json, "skip"); }

    @Transactional
    @Override
    public int importFromJsonWithConflict(String json, String conflictMode) {
        try {
            Map<String, Object> importData = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            Object itemsObj = importData.get("items");
            if (itemsObj == null) return 0;
            List<Map<String, Object>> items = extractItems(itemsObj);
            int imported = 0;
            String defaultTitle = config.getString("default.knowledge.import-default-title", "未命名");
            for (Map<String, Object> item : items) {
                String title = (String) item.getOrDefault("title", defaultTitle);
                if ("skip".equals(conflictMode)) {
                    if (!mapper.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(title, "").isEmpty()) continue;
                }
                if ("overwrite".equals(conflictMode)) {
                    List<Knowledge> existing = mapper.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(title, "");
                    if (!existing.isEmpty()) {
                        Knowledge ek = existing.get(0);
                        ek.setContent((String) item.getOrDefault("content", ""));
                        ek.setContentType((String) item.getOrDefault("contentType", "TEXT"));
                        ek.setSourceUrl((String) item.getOrDefault("sourceUrl", null));
                        ek.setSummary((String) item.getOrDefault("summary", null));
                        ek.setTags((String) item.getOrDefault("tags", "[]"));
                        ek.setUserTags((String) item.getOrDefault("userTags", "[]"));
                        ek.setUpdatedAt(LocalDateTime.now());
                        mapper.updateById(ek);
                        imported++; continue;
                    }
                }
                Knowledge k = new Knowledge();
                k.setTitle(title);
                k.setContent((String) item.getOrDefault("content", ""));
                k.setContentType((String) item.getOrDefault("contentType", "TEXT"));
                k.setSourceUrl((String) item.getOrDefault("sourceUrl", null));
                k.setSummary((String) item.getOrDefault("summary", null));
                k.setTags((String) item.getOrDefault("tags", "[]"));
                k.setUserTags((String) item.getOrDefault("userTags", "[]"));
                k.setMetadata("{}");
                k.setAutoProcessStatus("PENDING");
                LocalDateTime now = LocalDateTime.now();
                k.setCreatedAt(now); k.setUpdatedAt(now);
                mapper.insert(k);
                autoProcessService.autoProcessAsync(k.getId(), k.getTitle(), k.getContent());
                imported++;
            }
            metricsService.recordImport();
            operationLogService.log("KNOWLEDGE", "IMPORT", null, "导入 " + imported + " 条知识 (冲突模式: " + conflictMode + ")");
            return imported;
        } catch (Exception e) {
            log.error("导入失败: {}", e.getMessage());
            throw new RuntimeException("导入失败: " + e.getMessage());
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) return "\"" + value.replace("\"", "\"\"") + "\"";
        return value;
    }

    private static String sanitizeFolderName(String name) {
        return name.replaceAll("[/\\\\:*?\"<>|]", "_").trim();
    }

    private String mergeTags(String aiTags, String userTags) {
        try {
            Set<String> merged = new LinkedHashSet<>();
            if (aiTags != null && !aiTags.equals("[]")) merged.addAll(objectMapper.readValue(aiTags, new TypeReference<List<String>>() {}));
            if (userTags != null && !userTags.equals("[]")) merged.addAll(objectMapper.readValue(userTags, new TypeReference<List<String>>() {}));
            return objectMapper.writeValueAsString(new ArrayList<>(merged));
        } catch (Exception e) { return "[]"; }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractItems(Object itemsObj) {
        if (itemsObj instanceof List<?> list) {
            List<Map<String, Object>> items = new ArrayList<>();
            for (Object o : list) { if (o instanceof Map<?, ?> m) items.add((Map<String, Object>) m); }
            return items;
        }
        return List.of();
    }

    private String displayTitle(Knowledge k) {
        return k.getAiTitle() != null && !k.getAiTitle().isBlank() ? k.getAiTitle() : k.getTitle();
    }
}