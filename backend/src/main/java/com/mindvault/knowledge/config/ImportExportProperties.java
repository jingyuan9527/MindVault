package com.mindvault.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mindvault.import-export")
public class ImportExportProperties {

    private String exportVersion = "0.4.0";
    private int maxFilenameLength = 80;
    private String markdownUntaggedFolder = "未分类";
    private String csvHeader = "标题,内容,类型,摘要,标签,来源,创建时间";
    private String importDefaultTitle = "未命名";

    public String getExportVersion() { return exportVersion; }
    public void setExportVersion(String exportVersion) { this.exportVersion = exportVersion; }
    public int getMaxFilenameLength() { return maxFilenameLength; }
    public void setMaxFilenameLength(int maxFilenameLength) { this.maxFilenameLength = maxFilenameLength; }
    public String getMarkdownUntaggedFolder() { return markdownUntaggedFolder; }
    public void setMarkdownUntaggedFolder(String markdownUntaggedFolder) { this.markdownUntaggedFolder = markdownUntaggedFolder; }
    public String getCsvHeader() { return csvHeader; }
    public void setCsvHeader(String csvHeader) { this.csvHeader = csvHeader; }
    public String getImportDefaultTitle() { return importDefaultTitle; }
    public void setImportDefaultTitle(String importDefaultTitle) { this.importDefaultTitle = importDefaultTitle; }
}