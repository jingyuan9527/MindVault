package com.mindvault.knowledge.service;

import com.mindvault.knowledge.dto.ImportPreview;
import java.util.List;
import java.util.Map;

public interface ImportExportService {
    String batchExport(List<Long> ids);
    String exportAllAsJson();
    byte[] exportAllAsMarkdown();
    String exportAllAsCsv();
    ImportPreview previewImport(String json);
    int importFromJson(String json);
    int importFromJsonWithConflict(String json, String conflictMode);
}