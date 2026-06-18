package com.mindvault.chat;

import com.mindvault.chat.entity.ChatMessage;
import com.mindvault.chat.entity.ChatSession;
import com.mindvault.common.dto.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "会话与消息管理", description = "聊天会话的创建、列表、删除，消息的发送、历史查询、SSE 流式推送")
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(summary = "创建会话", description = "创建一个新的聊天会话")
    @PostMapping("/sessions")
    public ApiResponse<ChatSession> createSession() {
        return ApiResponse.success(chatService.createSession());
    }

    @Operation(summary = "会话列表", description = "获取所有聊天会话列表")
    @GetMapping("/sessions")
    public ApiResponse<List<ChatSession>> listSessions() {
        return ApiResponse.success(chatService.listSessions());
    }

    @Operation(summary = "消息历史", description = "获取指定会话的历史消息列表")
    @GetMapping("/sessions/{id}/messages")
    public ApiResponse<List<ChatMessage>> getMessages(@Parameter(description = "会话 ID") @PathVariable Long id) {
        return ApiResponse.success(chatService.getMessages(id));
    }

    @Operation(summary = "发送消息", description = "向指定会话发送消息并获取 AI 回复")
    @PostMapping("/sessions/{id}/messages")
    public ApiResponse<ChatMessage> sendMessage(
            @Parameter(description = "会话 ID") @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ApiResponse.error(400, "消息内容不能为空");
        }
        return ApiResponse.success(chatService.sendMessage(id, content));
    }

    @Operation(summary = "流式发送消息", description = "向指定会话发送消息并通过 SSE 流式获取 AI 回复")
    @PostMapping(value = "/sessions/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessageStream(
            @Parameter(description = "会话 ID") @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("error").data("消息内容不能为空"));
                emitter.complete();
            } catch (Exception ignored) {}
            return emitter;
        }
        return chatService.sendMessageStream(id, content);
    }
}