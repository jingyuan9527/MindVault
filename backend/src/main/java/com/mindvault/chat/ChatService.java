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

/**
 * 聊天核心服务。
 * <p>管理聊天会话和消息的生命周期，提供会话创建、消息发送（同步/流式 SSE）、关键字拦截等功能。
 * 输入为用户消息文本，输出为 AI 回复（同步返回 String 或通过 SseEmitter 流式推送）。
 * 依赖 AgentService 调用 LLM 生成回复，KeywordBlockingService 做敏感内容过滤。
 * 关键设计：首次消息自动更新会话标题；流式模式下异步写入消息记录并推送 token/sources/done/error 事件。</p>
 */
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

    /**
     * 创建新的聊天会话。
     * 初始标题设为"新对话"，记录操作日志。
     * @return 创建的会话对象（含自增 ID）
     */
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

    /**
     * 获取所有会话列表，按更新时间降序排列。
     * @return 会话列表
     */
    public List<ChatSession> listSessions() {
        return sessionMapper.findAllByOrderByUpdatedAtDesc();
    }

    /**
     * 获取指定会话的历史消息列表。
     * @param sessionId 会话 ID
     * @return 按时间升序排列的消息列表
     */
    public List<ChatMessage> getMessages(Long sessionId) {
        return messageMapper.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    /**
     * 向指定会话发送消息并获取 AI 同步回复。
     * 先检查关键字拦截；若拦截则返回系统提示消息。否则保存用户消息，
     * 调用 AgentService 生成回复，提取引用来源，自动更新会话标题。
     * @param sessionId 会话 ID
     * @param content 用户消息内容
     * @return AI 回复消息对象
     */
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

    /**
     * 向指定会话发送消息并通过 SSE 流式获取 AI 回复。
     * 异步处理 LLM 流式输出，逐 token 推送，完成后写入消息记录。
     * 事件类型：token（逐字输出）、sources（引用来源 JSON）、done（完成）、error（异常）、blocked（被拦截）。
     * @param sessionId 会话 ID
     * @param content 用户消息内容
     * @return SSE 发射器，客户端可通过 EventSource 监听
     */
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

    /**
     * 更新会话标题。仅当当前标题为默认值"新对话"时，截取消息前 30 字作为新标题。
     */
    private void updateSessionTitle(Long sessionId, String content) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session != null && "新对话".equals(session.getTitle())) {
            session.setTitle(content.length() > 30 ? content.substring(0, 30) + "..." : content);
            session.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
    }

    /**
     * 从 AI 回复文本中提取引用的知识来源。
     * 匹配形如 [123] 的数字标记，查询知识库获取标题和 URL，去重后返回 JSON 数组。
     * @param reply AI 回复文本
     * @return 来源列表的 JSON 字符串，格式：[{"id":1, "title":"...", "url":"..."}]
     */
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