package com.mindvault.flashcard;

import com.mindvault.common.annotation.OperationLog;
import com.mindvault.common.dto.ApiResponse;
import com.mindvault.flashcard.entity.FlashCard;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "闪卡管理", description = "闪卡的增删改查")
@RestController
@RequestMapping("/api/v1/flashcards")
public class FlashCardController {

    private final FlashCardService flashCardService;

    public FlashCardController(FlashCardService flashCardService) {
        this.flashCardService = flashCardService;
    }

    @Operation(summary = "闪卡列表", description = "获取所有闪卡")
    @GetMapping
    public ApiResponse<List<FlashCard>> listAll() {
        return ApiResponse.success(flashCardService.listAll());
    }

    @Operation(summary = "知识闪卡", description = "获取指定知识关联的所有闪卡")
    @GetMapping("/knowledge/{knowledgeId}")
    public ApiResponse<List<FlashCard>> listByKnowledge(@Parameter(description = "知识 ID") @PathVariable Long knowledgeId) {
        return ApiResponse.success(flashCardService.listByKnowledge(knowledgeId));
    }

    @OperationLog(module = "闪卡", action = "生成闪卡", actionType = "CREATE")
    @Operation(summary = "生成闪卡", description = "基于指定知识的内容自动生成闪卡")
    @PostMapping("/generate/{knowledgeId}")
    public ApiResponse<List<FlashCard>> generate(@Parameter(description = "知识 ID") @PathVariable Long knowledgeId) {
        return ApiResponse.success(flashCardService.generateCards(knowledgeId));
    }

    @OperationLog(module = "闪卡", action = "删除闪卡", actionType = "DELETE")
    @Operation(summary = "删除闪卡", description = "删除指定 ID 的闪卡")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "闪卡 ID") @PathVariable Long id) {
        flashCardService.deleteCard(id);
        return ApiResponse.success(null);
    }
}