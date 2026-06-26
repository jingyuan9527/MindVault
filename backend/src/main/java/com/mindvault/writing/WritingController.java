package com.mindvault.writing;

import com.mindvault.common.annotation.OperationLog;
import com.mindvault.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 写作助手 REST 接口。
 * <p>
 * 提供基于主题/风格/关键词生成文章的 API。支持从知识库检索相关参考内容辅助创作。
 * 主题为必填参数，风格和关键词可选。
 * </p>
 */
@Tag(name = "写作助手", description = "AI 辅助写作：根据主题/风格/关键词生成文章")
@RestController
@RequestMapping("/api/v1/writing")
public class WritingController {

    private final WritingService writingService;

    public WritingController(WritingService writingService) {
        this.writingService = writingService;
    }

    @OperationLog(module = "写作助手", action = "生成文章", actionType = "CREATE")
    @Operation(summary = "生成文章", description = "根据主题、风格、关键词生成 AI 文章")
    @PostMapping("/generate")
    public ApiResponse<String> generate(@RequestBody Map<String, String> request) {
        String topic = request.get("topic");
        if (topic == null || topic.isBlank()) {
            return ApiResponse.error(400, "请提供写作主题");
        }
        String style = request.get("style");
        String keywords = request.get("keywords");
        String article = writingService.generateArticle(topic, style, keywords);
        return ApiResponse.success(article);
    }
}