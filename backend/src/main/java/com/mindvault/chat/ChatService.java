package com.mindvault.chat;

import com.mindvault.agent.AgentService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.chat.entity.ChatMessage;
import com.mindvault.chat.entity.ChatSession;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.operationlog.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final AgentService agentService;
    private final OperationLogService operationLogService;
    private final KnowledgeService knowledgeService;
    private final KeywordBlockingService keywordBlockingService;
    private final ObjectMapper objectMapper;

    public ChatService(ChatSessionMapper sessionMapper,
                       ChatMessageMapper messageMapper,
                       AgentService agentService,
                       OperationLogService operationLogService,
                       KnowledgeService knowledgeService,
                       KeywordBlockingService keywordBlockingService) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.agentService = agentService;
        this.operationLogService = operationLogService;
        this.knowledgeService = knowledgeService;
        this.keywordBlockingService = keywordBlockingService;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public ChatSession createSession() {
        ChatSession session = new ChatSession();
        session.setTitle("新对话");
        LocalDateTime now = LocalDateTime.now();
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        sessionMapper.insert(session);
        log.info("创建新会话: id={}", session.getId());
        operationLogService.log("CHAT", "CREATE_SESSION", session.getId(), "创建新会话");
        return session;
    }

    public List<ChatSession> listSessions() {
        return sessionMapper.findAllByOrderByUpdatedAtDesc();
    }

    public List<ChatMessage> getMessages(Long sessionId) {
        return messageMapper.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Transactional
    public ChatMessage sendMessage(Long sessionId, String content) {
        if (keywordBlockingService.isBlocked(content)) {
            log.info("消息被关键字拦截: sessionId={}", sessionId);
            operationLogService.log("CHAT", "BLOCKED", sessionId, "消息被关键字拦截");
            ChatMessage userMsg = new ChatMessage();
            userMsg.setSessionId(sessionId);
            userMsg.setRole("USER");
            userMsg.setContent(content);
            userMsg.setCreatedAt(LocalDateTime.now());
            messageMapper.insert(userMsg);

            ChatMessage blockMsg = new ChatMessage();
            blockMsg.setSessionId(sessionId);
            blockMsg.setRole("SYSTEM");
            blockMsg.setContent(keywordBlockingService.getBlockMessage());
            blockMsg.setCreatedAt(LocalDateTime.now());
            messageMapper.insert(blockMsg);
            return blockMsg;
        }

        LocalDateTime now = LocalDateTime.now();
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("USER");
        userMsg.setContent(content);
        userMsg.setCreatedAt(now);
        messageMapper.insert(userMsg);

        log.info("用户消息: sessionId={}, content={}", sessionId,
                content.length() > 50 ? content.substring(0, 50) + "..." : content);

        String reply = agentService.processMessage(content);

        ChatMessage agentMsg = new ChatMessage();
        agentMsg.setSessionId(sessionId);
        agentMsg.setRole("ASSISTANT");
        agentMsg.setContent(reply);
        agentMsg.setSources(extractSources(reply));
        agentMsg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(agentMsg);

        updateSessionTitle(sessionId, content);

        log.info("Agent 回复: sessionId={}, 长度={}", sessionId, reply.length());
        operationLogService.log("CHAT", "SEND_MESSAGE", sessionId,
                "对话 " + sessionId + " 发送消息并回复");

        return agentMsg;
    }

    public SseEmitter sendMessageStream(Long sessionId, String content) {
        SseEmitter emitter = new SseEmitter(300_000L);

        if (keywordBlockingService.isBlocked(content)) {
            log.info("消息被关键字拦截(流式): sessionId={}", sessionId);
            operationLogService.log("CHAT", "BLOCKED", sessionId, "消息被关键字拦截");
            LocalDateTime now = LocalDateTime.now();
            ChatMessage userMsg = new ChatMessage();
            userMsg.setSessionId(sessionId);
            userMsg.setRole("USER");
            userMsg.setContent(content);
            userMsg.setCreatedAt(now);
            messageMapper.insert(userMsg);
            ChatMessage blockMsg = new ChatMessage();
            blockMsg.setSessionId(sessionId);
            blockMsg.setRole("SYSTEM");
            blockMsg.setContent(keywordBlockingService.getBlockMessage());
            blockMsg.setCreatedAt(now);
            messageMapper.insert(blockMsg);
            try {
                emitter.send(SseEmitter.event().name("blocked").data(keywordBlockingService.getBlockMessage()));
                emitter.send(SseEmitter.event().name("done").data(""));
                emitter.complete();
            } catch (IOException e) {
                log.warn("发送拦截 SSE 事件失败: {}", e.getMessage());
            }
            return emitter;
        }

        LocalDateTime now = LocalDateTime.now();
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("USER");
        userMsg.setContent(content);
        userMsg.setCreatedAt(now);
        messageMapper.insert(userMsg);

        log.info("用户消息(流式): sessionId={}, content={}", sessionId,
                content.length() > 50 ? content.substring(0, 50) + "..." : content);
        operationLogService.log("CHAT", "SEND_MESSAGE", sessionId,
                "对话 " + sessionId + " 发送消息(流式)");

        ChatMessage agentMsg = new ChatMessage();
        agentMsg.setSessionId(sessionId);
        agentMsg.setRole("ASSISTANT");
        agentMsg.setContent("");
        agentMsg.setSources("[]");

        CompletableFuture.runAsync(() -> {
            try {
                StringBuilder fullReply = new StringBuilder();
                agentService.processMessageStream(content, new AgentService.StreamCallback() {
                    @Override
                    public void onToken(String token) {
                        try {
                            emitter.send(SseEmitter.event().name("token").data(token));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        fullReply.append(token);
                    }

                    @Override
                    public void onComplete() {
                        try {
                            String reply = fullReply.toString();
                            agentMsg.setContent(reply);
                            agentMsg.setCreatedAt(LocalDateTime.now());
                            String sourceJson = extractSources(reply);
                            agentMsg.setSources(sourceJson);
                            messageMapper.insert(agentMsg);
                            updateSessionTitle(sessionId, content);
                            if (!"[]".equals(sourceJson)) {
                                emitter.send(SseEmitter.event().name("sources").data(sourceJson));
                            }
                            emitter.send(SseEmitter.event().name("done").data(""));
                            emitter.complete();
                        } catch (IOException e) {
                            log.warn("SSE complete 发送失败: {}", e.getMessage());
                        }
                    }

                    @Override
                    public void onError(String error) {
                        try {
                            emitter.send(SseEmitter.event().name("error").data(error));
                            emitter.complete();
                        } catch (IOException e) {
                            log.warn("SSE 发送错误消息失败: {}", e.getMessage());
                        }
                    }
                });
            } catch (Exception ex) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(ex.getMessage()));
                    emitter.complete();
                } catch (IOException ie) {
                    log.warn("SSE 异常处理失败: {}", ie.getMessage());
                }
            }
        });

        emitter.onCompletion(() -> log.info("SSE 流结束: sessionId={}", sessionId));
        emitter.onTimeout(() -> log.warn("SSE 流超时: sessionId={}", sessionId));

        return emitter;
    }

    private void updateSessionTitle(Long sessionId, String content) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session != null && "新对话".equals(session.getTitle())) {
            session.setTitle(content.length() > 30 ? content.substring(0, 30) + "..." : content);
            session.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
    }

    private String extractSources(String reply) {
        try {
            Pattern pattern = Pattern.compile("\\[(\\d+)\\]");
            Matcher matcher = pattern.matcher(reply);
            List<Map<String, Object>> sources = new ArrayList<>();
            Set<Long> seen = new HashSet<>();
            while (matcher.find()) {
                Long id = Long.parseLong(matcher.group(1));
                if (seen.add(id)) {
                    try {
                        var knowledge = knowledgeService.getById(id);
                        Map<String, Object> source = new LinkedHashMap<>();
                        source.put("id", knowledge.getId());
                        source.put("title", knowledge.getTitle());
                        source.put("url", knowledge.getSourceUrl());
                        sources.add(source);
                    } catch (Exception e) {
                        log.warn("提取来源失败: id={}", id, e);
                    }
                }
            }
            return objectMapper.writeValueAsString(sources);
        } catch (Exception e) {
            return "[]";
        }
    }
}