package com.mindvault.chat;

import com.mindvault.agent.AgentService;
import com.mindvault.chat.entity.ChatMessage;
import com.mindvault.chat.entity.ChatSession;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.operationlog.OperationLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ChatSessionMapper sessionMapper;
    @Mock private ChatMessageMapper messageMapper;
    @Mock private AgentService agentService;
    @Mock private OperationLogService operationLogService;
    @Mock private KnowledgeService knowledgeService;

    private ChatService service;

    @Captor private ArgumentCaptor<ChatSession> sessionCaptor;
    @Captor private ArgumentCaptor<ChatMessage> messageCaptor;

    @BeforeEach
    void setUp() {
        service = new ChatService(sessionMapper, messageMapper, agentService,
                operationLogService, knowledgeService);
    }

    @Test
    void createSession_shouldInsertAndLog() {
        ChatSession result = service.createSession();

        verify(sessionMapper).insert(sessionCaptor.capture());
        ChatSession captured = sessionCaptor.getValue();
        assertEquals("新对话", captured.getTitle());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());

        assertNotNull(result);
        verify(operationLogService).log(eq("CHAT"), eq("CREATE_SESSION"), any(), eq("创建新会话"));
    }

    @Test
    void listSessions_shouldDelegate() {
        ChatSession s1 = new ChatSession();
        s1.setId(1L);
        s1.setTitle("会话1");
        ChatSession s2 = new ChatSession();
        s2.setId(2L);
        s2.setTitle("会话2");
        when(sessionMapper.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(s1, s2));

        List<ChatSession> result = service.listSessions();

        assertEquals(2, result.size());
        assertEquals("会话1", result.get(0).getTitle());
        verify(sessionMapper).findAllByOrderByUpdatedAtDesc();
    }

    @Test
    void getMessages_shouldDelegate() {
        ChatMessage m1 = new ChatMessage();
        m1.setId(1L);
        m1.setSessionId(10L);
        m1.setRole("USER");
        m1.setContent("你好");
        ChatMessage m2 = new ChatMessage();
        m2.setId(2L);
        m2.setSessionId(10L);
        m2.setRole("ASSISTANT");
        m2.setContent("你好！");
        when(messageMapper.findBySessionIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(m1, m2));

        List<ChatMessage> result = service.getMessages(10L);

        assertEquals(2, result.size());
        assertEquals("USER", result.get(0).getRole());
        verify(messageMapper).findBySessionIdOrderByCreatedAtAsc(10L);
    }

    @Test
    void sendMessage_shouldSaveBothMessagesAndUpdateTitle() {
        ChatSession session = new ChatSession();
        session.setId(1L);
        session.setTitle("新对话");
        when(sessionMapper.selectById(1L)).thenReturn(session);
        when(agentService.processMessage("hello")).thenReturn("some reply");

        ChatMessage result = service.sendMessage(1L, "hello");

        verify(messageMapper, times(2)).insert(messageCaptor.capture());
        List<ChatMessage> captured = messageCaptor.getAllValues();
        assertEquals(2, captured.size());

        assertEquals("USER", captured.get(0).getRole());
        assertEquals("hello", captured.get(0).getContent());
        assertEquals(1L, captured.get(0).getSessionId());

        assertEquals("ASSISTANT", captured.get(1).getRole());
        assertEquals("some reply", captured.get(1).getContent());
        assertEquals(1L, captured.get(1).getSessionId());

        assertEquals("some reply", result.getContent());
        assertEquals("ASSISTANT", result.getRole());

        verify(sessionMapper).updateById(sessionCaptor.capture());
        assertTrue(sessionCaptor.getValue().getTitle().contains("hello"));

        verify(operationLogService).log(eq("CHAT"), eq("SEND_MESSAGE"), eq(1L), anyString());
    }

    @Test
    void sendMessage_shouldNotUpdateTitleIfAlreadySet() {
        ChatSession session = new ChatSession();
        session.setId(1L);
        session.setTitle("已有标题");
        when(sessionMapper.selectById(1L)).thenReturn(session);
        when(agentService.processMessage("hello")).thenReturn("some reply");

        service.sendMessage(1L, "hello");

        verify(sessionMapper, never()).updateById(any(ChatSession.class));
        verify(messageMapper, times(2)).insert(any(ChatMessage.class));
    }
}