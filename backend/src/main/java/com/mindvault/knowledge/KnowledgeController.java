package com.mindvault.knowledge;

import com.mindvault.common.dto.ApiResponse;
import com.mindvault.content.ContentParserService;
import com.mindvault.knowledge.entity.Knowledge;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 知识库 REST API
 */
@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final ContentParserService contentParserService;
    private final KnowledgeAssociationService associationService;

    public KnowledgeController(KnowledgeService knowledgeService,
                                ContentParserService contentParserService,
                                KnowledgeAssociationService associationService) {
        this.knowledgeService = knowledgeService;
        this.contentParserService = contentParserService;
        this.associationService = associationService;
    }

    @PostMapping
    public ApiResponse<Knowledge> addKnowledge(@Valid @RequestBody Knowledge knowledge) {
        return ApiResponse.success(knowledgeService.addKnowledge(knowledge));
    }

    @GetMapping
    public ApiResponse<List<Knowledge>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(knowledgeService.listAll(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Knowledge> getById(@PathVariable Long id) {
        return ApiResponse.success(knowledgeService.getById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Knowledge> updateKnowledge(@PathVariable Long id,
                                                   @Valid @RequestBody Knowledge knowledge) {
        return ApiResponse.success(knowledgeService.updateKnowledge(id, knowledge));
    }

    @GetMapping("/search")
    public ApiResponse<?> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int topN,
            @RequestParam(required = false) String tag) {
        if (tag != null && !tag.isBlank()) {
            return ApiResponse.success(knowledgeService.searchByKeywordWithTag(q, topN, tag));
        }
        return ApiResponse.success(knowledgeService.searchByKeyword(q, topN));
    }

    @GetMapping("/{id}/related")
    public ApiResponse<List<Map<String, Object>>> getRelated(
            @PathVariable Long id,
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.success(associationService.getRelatedKnowledge(id, limit));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteKnowledge(@PathVariable Long id) {
        knowledgeService.deleteKnowledge(id);
        return ApiResponse.success(null);
    }

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

    @GetMapping("/export/json")
    public ResponseEntity<String> exportJson() {
        String json = knowledgeService.exportAllAsJson();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mindvault-export.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    @GetMapping("/export/markdown")
    public ResponseEntity<byte[]> exportMarkdown() {
        byte[] zip = knowledgeService.exportAllAsMarkdown();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mindvault-export.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zip);
    }

    @PostMapping("/import")
    public ApiResponse<Map<String, Integer>> importJson(@RequestBody String jsonBody) {
        int count = knowledgeService.importFromJson(jsonBody);
        return ApiResponse.success(Map.of("imported", count));
    }
}