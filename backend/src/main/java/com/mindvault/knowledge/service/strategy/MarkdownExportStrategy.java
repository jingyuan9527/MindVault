package com.mindvault.knowledge.service.strategy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.knowledge.config.ImportExportProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class MarkdownExportStrategy implements ExportFormatStrategy {

    private static final Logger log = LoggerFactory.getLogger(MarkdownExportStrategy.class);

    private final ImportExportProperties importExportProperties;
    private final ObjectMapper objectMapper;

    public MarkdownExportStrategy(ImportExportProperties importExportProperties) {
        this.importExportProperties = importExportProperties;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getFormat() {
        return "markdown";
    }

    @Override
    public byte[] export(List<Knowledge> items) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (Knowledge k : items) {
                String displayName = displayTitle(k);
                String safeName = displayName.replaceAll("[/\\\\:*?\"<>|]", "_").replaceAll("\\s+", "_");
                int maxFilenameLen = importExportProperties.getMaxFilenameLength();
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
                String untaggedFolder = importExportProperties.getMarkdownUntaggedFolder();
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
    public boolean isApplicable(List<Knowledge> items) {
        return items != null && !items.isEmpty();
    }

    private String displayTitle(Knowledge k) {
        return k.getAiTitle() != null && !k.getAiTitle().isBlank() ? k.getAiTitle() : k.getTitle();
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
}