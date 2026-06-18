package com.mindvault.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.chat.entity.ChatMessage;
import com.mindvault.chat.entity.ChatSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ChatService chatService;

    private ChatSession createSession(Long id) {
        ChatSession s = new ChatSession();
        s.setId(id);
        s.setTitle("Session " + id);
        s.setCreatedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        return s;
    }

    private ChatMessage createMessage(Long id) {
        ChatMessage m = new ChatMessage();
        m.setId(id);
        m.setSessionId(1L);
        m.setRole("user");
        m.setContent("Hello");
        m.setCreatedAt(LocalDateTime.now());
        return m;
    }

    @Test
    void createSession_shouldReturnSession() throws Exception {
        when(chatService.createSession()).thenReturn(createSession(1L));

        mockMvc.perform(post("/api/v1/chat/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Session 1"));
    }

    @Test
    void listSessions_shouldReturnList() throws Exception {
        when(chatService.listSessions()).thenReturn(List.of(createSession(1L), createSession(2L)));

        mockMvc.perform(get("/api/v1/chat/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void getMessages_shouldReturnMessages() throws Exception {
        when(chatService.getMessages(1L)).thenReturn(List.of(createMessage(1L)));

        mockMvc.perform(get("/api/v1/chat/sessions/1/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("Hello"));
    }

    @Test
    void sendMessage_shouldReturnResponse() throws Exception {
        ChatMessage reply = createMessage(2L);
        reply.setRole("assistant");
        reply.setContent("Reply");
        when(chatService.sendMessage(1L, "Hello")).thenReturn(reply);

        mockMvc.perform(post("/api/v1/chat/sessions/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Reply"));
    }

    @Test
    void sendMessage_shouldReturnErrorForBlankContent() throws Exception {
        mockMvc.perform(post("/api/v1/chat/sessions/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("消息内容不能为空"));
    }

    @Test
    void sendMessage_shouldReturnErrorForNullContent() throws Exception {
        mockMvc.perform(post("/api/v1/chat/sessions/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}