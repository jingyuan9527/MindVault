package com.mindvault.chat;

import com.mindvault.chat.entity.ChatMessage;
import com.mindvault.chat.entity.ChatSession;
import com.mindvault.common.dto.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/sessions")
    public ApiResponse<ChatSession> createSession() {
        return ApiResponse.success(chatService.createSession());
    }

    @GetMapping("/sessions")
    public ApiResponse<List<ChatSession>> listSessions() {
        return ApiResponse.success(chatService.listSessions());
    }

    @GetMapping("/sessions/{id}/messages")
    public ApiResponse<List<ChatMessage>> getMessages(@PathVariable Long id) {
        return ApiResponse.success(chatService.getMessages(id));
    }

    @PostMapping("/sessions/{id}/messages")
    public ApiResponse<ChatMessage> sendMessage(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ApiResponse.error(400, "消息内容不能为空");
        }
        return ApiResponse.success(chatService.sendMessage(id, content));
    }

    @PostMapping(value = "/sessions/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessageStream(
            @PathVariable Long id,
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