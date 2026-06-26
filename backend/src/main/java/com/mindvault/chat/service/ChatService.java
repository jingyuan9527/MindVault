package com.mindvault.chat.service;

import com.mindvault.chat.entity.ChatMessage;
import com.mindvault.chat.entity.ChatSession;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 聊天核心服务接口。
 * <p>管理聊天会话和消息的生命周期，提供会话创建、消息发送（同步/流式 SSE）、关键字拦截等功能。</p>
 */
public interface ChatService {

    ChatSession createSession();

    List<ChatSession> listSessions();

    List<ChatMessage> getMessages(Long sessionId);

    ChatMessage sendMessage(Long sessionId, String content);

    SseEmitter sendMessageStream(Long sessionId, String content);
}