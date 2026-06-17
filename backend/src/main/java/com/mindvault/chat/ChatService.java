package com.mindvault.chat;

import com.mindvault.agent.AgentService;
import com.mindvault.chat.entity.ChatMessage;
import com.mindvault.chat.entity.ChatSession;
import com.mindvault.operationlog.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final AgentService agentService;
    private final OperationLogService operationLogService;

    public ChatService(ChatSessionRepository sessionRepository,
                       ChatMessageRepository messageRepository,
                       AgentService agentService,
                       OperationLogService operationLogService) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.agentService = agentService;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public ChatSession createSession() {
        ChatSession session = new ChatSession();
        session.setTitle("新对话");
        ChatSession saved = sessionRepository.save(session);
        log.info("创建新会话: id={}", saved.getId());
        operationLogService.log("CHAT", "CREATE_SESSION", saved.getId(), "创建新会话");
        return saved;
    }

    public List<ChatSession> listSessions() {
        return sessionRepository.findAllByOrderByUpdatedAtDesc();
    }

    public List<ChatMessage> getMessages(Long sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Transactional
    public ChatMessage sendMessage(Long sessionId, String content) {
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("USER");
        userMsg.setContent(content);
        messageRepository.save(userMsg);

        log.info("用户消息: sessionId={}, content={}", sessionId,
                content.length() > 50 ? content.substring(0, 50) + "..." : content);

        String reply = agentService.processMessage(content);

        ChatMessage agentMsg = new ChatMessage();
        agentMsg.setSessionId(sessionId);
        agentMsg.setRole("ASSISTANT");
        agentMsg.setContent(reply);
        ChatMessage saved = messageRepository.save(agentMsg);

        ChatSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session != null && "新对话".equals(session.getTitle())) {
            session.setTitle(content.length() > 30 ? content.substring(0, 30) + "..." : content);
            sessionRepository.save(session);
        }

        log.info("Agent 回复: sessionId={}, 长度={}", sessionId, reply.length());
        operationLogService.log("CHAT", "SEND_MESSAGE", sessionId,
                "对话 " + sessionId + " 发送消息并回复");

        return saved;
    }
}