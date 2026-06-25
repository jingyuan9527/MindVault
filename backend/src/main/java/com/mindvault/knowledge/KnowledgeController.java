package com.mindvault.knowledge;

import com.mindvault.common.annotation.OperationLog;
import com.mindvault.common.dto.ApiResponse;
import com.mindvault.common.dto.PageResult;
import com.mindvault.content.ContentParserService;
import com.mindvault.knowledge.entity.Knowledge;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mindvault.auto.AutoProcessLogMapper;
import com.mindvault.auto.entity.AutoProcessLog;
import com.mindvault.knowledge.dto.ImportPreview;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "知识库", description = "知识的增删改查、搜索、导入导出、标签管理")
@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final ContentParserService contentParserService;
    private final KnowledgeAssociationService associationService;
    private final SearchEnhanceService searchEnhanceService;
    private final AutoProcessLogMapper autoProcessLogMapper;

    public KnowledgeController(KnowledgeService knowledgeService,
                                ContentParserService contentParserService,
                                KnowledgeAssociationService associationService,
                                SearchEnhanceService searchEnhanceService,
                                AutoProcessLogMapper autoProcessLogMapper) {
        this.knowledgeService = knowledgeService;
        this.contentParserService = contentParserService;
        this.associationService = associationService;
        this.searchEnhanceService = searchEnhanceService;
        this.autoProcessLogMapper = autoProcessLogMapper;
    }

    @OperationLog(module = "knowledge", action = "create", description = "新增知识")
    @Operation(summary = "新增知识", description = "创建一条新的知识笔记")
    @PostMapping
    public ApiResponse<Knowledge> addKnowledge(@Valid @RequestBody Knowledge knowledge) {
        return ApiResponse.success(knowledgeService.addKnowledge(knowledge));
    }

    @Operation(summary = "知识列表", description = "分页获取知识列表，支持关键字搜索、标签筛选、排序")
    @GetMapping
    public ApiResponse<PageResult<Knowledge>> listAll(
            @Parameter(description = "页码，从 0 开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "标签筛选（可重复）") @RequestParam(required = false) List<String> tags,
            @Parameter(description = "排序字段: createdAt/updatedAt/title/relevance") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向: asc/desc") @RequestParam(defaultValue = "desc") String sortOrder) {
        return ApiResponse.success(knowledgeService.listAll(page, size, keyword, tags, sortBy, sortOrder));
    }

    @Operation(summary = "知识详情", description = "根据 ID 获取单条知识")
    @GetMapping("/{id}")
    public ApiResponse<Knowledge> getById(@Parameter(description = "知识 ID") @PathVariable Long id) {
        return ApiResponse.success(knowledgeService.getById(id));
    }

    @OperationLog(module = "knowledge", action = "update", description = "更新知识")
    @Operation(summary = "更新知识", description = "更新指定 ID 的知识内容")
    @PutMapping("/{id}")
    public ApiResponse<Knowledge> updateKnowledge(@Parameter(description = "知识 ID") @PathVariable Long id,
                                                   @Valid @RequestBody Knowledge knowledge) {
        return ApiResponse.success(knowledgeService.updateKnowledge(id, knowledge));
    }

    @Operation(summary = "搜索知识", description = "混合搜索（向量+关键字），支持语义重排序和标签过滤")
    @GetMapping("/search")
    public ApiResponse<?> search(
            @Parameter(description = "搜索关键词") @RequestParam String q,
            @Parameter(description = "返回条数") @RequestParam(defaultValue = "5") int topN,
            @Parameter(description = "标签过滤（可选）") @RequestParam(required = false) String tag) {
        if (tag != null && !tag.isBlank()) {
            return ApiResponse.success(knowledgeService.searchByKeywordWithTag(q, topN, tag));
        }
        List<Map<String, Object>> results = knowledgeService.hybridSearch(q, topN * 2);
        List<Map<String, Object>> reranked = searchEnhanceService.rerankResults(q, results, topN);
        return ApiResponse.success(reranked);
    }

    @Operation(summary = "HyDE 搜索", description = "基于假设文档嵌入的增强语义搜索")
    @GetMapping("/search/hyde")
    public ApiResponse<?> searchHyde(
            @Parameter(description = "搜索关键词") @RequestParam String q,
            @Parameter(description = "返回条数") @RequestParam(defaultValue = "5") int topN) {
        List<Map<String, Object>> results = searchEnhanceService.hydeSearch(q, topN * 2);
        List<Map<String, Object>> reranked = searchEnhanceService.rerankResults(q, results, topN);
        return ApiResponse.success(reranked);
    }

    @Operation(summary = "查询改写搜索", description = "LLM 改写查询后进行语义搜索")
    @GetMapping("/search/rewrite")
    public ApiResponse<?> searchWithRewrite(
            @Parameter(description = "搜索关键词") @RequestParam String q,
            @Parameter(description = "返回条数") @RequestParam(defaultValue = "5") int topN) {
        List<Map<String, Object>> results = searchEnhanceService.searchWithRewrite(q, topN);
        return ApiResponse.success(results);
    }

    @Operation(summary = "相关知识", description = "基于向量相似度获取相关知识推荐")
    @GetMapping("/{id}/related")
    public ApiResponse<List<Map<String, Object>>> getRelated(
            @Parameter(description = "知识 ID") @PathVariable Long id,
            @Parameter(description = "返回条数") @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.success(associationService.getRelatedKnowledge(id, limit));
    }

    @OperationLog(module = "knowledge", action = "ai_tag", description = "AI 批量打标签")
    @Operation(summary = "AI 批量打标签", description = "选中多条知识，AI 根据内容生成标签并追加到 user_tags")
    @PostMapping("/batch/ai-tag")
    public ApiResponse<Map<String, Object>> batchAiTag(@RequestBody List<Long> ids) {
        int count = knowledgeService.batchAiTag(ids);
        Map<String, Object> result = new HashMap<>();
        result.put("success", count);
        result.put("total", ids.size());
        return ApiResponse.success(result);
    }

    @OperationLog(module = "knowledge", action = "delete", description = "删除知识")
    @Operation(summary = "删除知识", description = "删除指定 ID 的知识及其关联的复习计划")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteKnowledge(@Parameter(description = "知识 ID") @PathVariable Long id) {
        knowledgeService.deleteKnowledge(id);
        return ApiResponse.success(null);
    }

    @OperationLog(module = "knowledge", action = "reprocess", description = "重新 AI 处理")
    @Operation(summary = "重新 AI 处理", description = "重置 AI 字段并重新执行自动处理流水线")
    @PostMapping("/{id}/reprocess")
    public ApiResponse<Void> reprocessKnowledge(@Parameter(description = "知识 ID") @PathVariable Long id) {
        knowledgeService.reprocessKnowledge(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "处理日志", description = "获取知识的 AI 自动处理日志")
    @GetMapping("/{id}/process-logs")
    public ApiResponse<List<AutoProcessLog>> getProcessLogs(@Parameter(description = "知识 ID") @PathVariable Long id) {
        return ApiResponse.success(autoProcessLogMapper.findByKnowledgeId(id));
    }

    @Operation(summary = "解析 URL", description = "从网页 URL 提取内容并创建知识")
    @PostMapping("/parse-url")
    public ApiResponse<Knowledge> parseUrl(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        if (url == null || url.isBlank()) {
            return ApiResponse.error(400, "URL不能为空");
        }
        ContentParserService.ParseResult result = contentParserService.parseUrl(url);
        if (result.content() == null) {
            return ApiResponse.error(400, "URL解析失败，请检查地址是否正确");
        }
        Knowledge knowledge = new Knowledge();
        knowledge.setTitle(result.title());
        knowledge.setContent(result.content());
        knowledge.setContentType(result.contentType());
        knowledge.setSourceUrl(url);
        return ApiResponse.success(knowledgeService.addKnowledge(knowledge));
    }

    @Operation(summary = "解析 PDF", description = "上传 PDF 文件，提取内容并创建知识")
    @PostMapping("/parse-pdf")
    public ApiResponse<Knowledge> parsePdf(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.error(400, "文件不能为空");
        }
        try {
            ContentParserService.ParseResult result = contentParserService.parsePdf(
                    file.getBytes(), file.getOriginalFilename());
            if (result.content() == null) {
                return ApiResponse.error(400, "PDF解析失败，文件可能已损坏");
            }
            Knowledge knowledge = new Knowledge();
            knowledge.setTitle(result.title());
            knowledge.setContent(result.content());
            knowledge.setContentType(result.contentType());
            return ApiResponse.success(knowledgeService.addKnowledge(knowledge));
        } catch (Exception e) {
            return ApiResponse.error(400, "PDF解析失败: " + e.getMessage());
        }
    }

    @Operation(summary = "导出 JSON", description = "导出全部知识为 JSON 格式文件")
    @GetMapping("/export/json")
    public ResponseEntity<String> exportJson() {
        String json = knowledgeService.exportAllAsJson();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mindvault-export.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    @Operation(summary = "导出 Markdown", description = "导出全部知识为 Markdown 文件（ZIP 压缩包）")
    @GetMapping("/export/markdown")
    public ResponseEntity<byte[]> exportMarkdown() {
        byte[] zip = knowledgeService.exportAllAsMarkdown();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mindvault-export.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zip);
    }

    @Operation(summary = "导出 CSV", description = "导出全部知识为 CSV 格式文件")
    @GetMapping("/export/csv")
    public ResponseEntity<String> exportCsv() {
        String csv = knowledgeService.exportAllAsCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mindvault-export.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv);
    }

    @Operation(summary = "预览导入", description = "预览 JSON 导入的内容，显示冲突信息")
    @PostMapping("/import/preview")
    public ApiResponse<ImportPreview> previewImport(@RequestBody String jsonBody) {
        ImportPreview preview = knowledgeService.previewImport(jsonBody);
        return ApiResponse.success(preview);
    }

    @Operation(summary = "导入 JSON", description = "导入 JSON 格式的知识数据，支持冲突策略（skip/overwrite）")
    @PostMapping("/import")
    public ApiResponse<Map<String, Object>> importJson(
            @RequestBody String jsonBody,
            @Parameter(description = "冲突策略：skip 跳过 / overwrite 覆盖") @RequestParam(defaultValue = "skip") String conflict) {
        int count = knowledgeService.importFromJsonWithConflict(jsonBody, conflict);
        Map<String, Object> result = new HashMap<>();
        result.put("imported", count);
        result.put("conflictMode", conflict);
        return ApiResponse.success(result);
    }

    @Operation(summary = "批量删除", description = "根据 ID 列表批量删除知识")
    @PostMapping("/batch/delete")
    public ApiResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        knowledgeService.batchDelete(ids);
        return ApiResponse.success(null);
    }

    @Operation(summary = "批量打标签", description = "为多条知识批量添加/替换标签")
    @PostMapping("/batch/tag")
    public ApiResponse<Void> batchTag(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Number> rawIds = (List<Number>) body.get("ids");
        List<Long> ids = rawIds.stream().map(Number::longValue).toList();
        String tag = (String) body.get("tag");
        knowledgeService.batchTag(ids, tag);
        return ApiResponse.success(null);
    }

    @Operation(summary = "批量导出", description = "按 ID 列表批量导出知识为 JSON 文件")
    @PostMapping("/batch/export")
    public ResponseEntity<String> batchExport(@RequestBody List<Long> ids) {
        String json = knowledgeService.batchExport(ids);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mindvault-export.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    @Operation(summary = "标签列表", description = "获取所有标签及其使用次数")
    @GetMapping("/tags")
    public ApiResponse<List<Map<String, Object>>> getAllTags() {
        return ApiResponse.success(knowledgeService.getAllTags());
    }
}