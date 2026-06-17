package com.mindvault.knowledge;

import com.mindvault.common.dto.ApiResponse;
import com.mindvault.knowledge.entity.Knowledge;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 知识库 REST API
 */
@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
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

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteKnowledge(@PathVariable Long id) {
        knowledgeService.deleteKnowledge(id);
        return ApiResponse.success(null);
    }
}