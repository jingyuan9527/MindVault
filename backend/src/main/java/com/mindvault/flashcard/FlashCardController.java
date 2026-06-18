package com.mindvault.flashcard;

import com.mindvault.common.dto.ApiResponse;
import com.mindvault.flashcard.entity.FlashCard;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/flashcards")
public class FlashCardController {

    private final FlashCardService flashCardService;

    public FlashCardController(FlashCardService flashCardService) {
        this.flashCardService = flashCardService;
    }

    @GetMapping
    public ApiResponse<List<FlashCard>> listAll() {
        return ApiResponse.success(flashCardService.listAll());
    }

    @GetMapping("/knowledge/{knowledgeId}")
    public ApiResponse<List<FlashCard>> listByKnowledge(@PathVariable Long knowledgeId) {
        return ApiResponse.success(flashCardService.listByKnowledge(knowledgeId));
    }

    @PostMapping("/generate/{knowledgeId}")
    public ApiResponse<List<FlashCard>> generate(@PathVariable Long knowledgeId) {
        return ApiResponse.success(flashCardService.generateCards(knowledgeId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        flashCardService.deleteCard(id);
        return ApiResponse.success(null);
    }
}