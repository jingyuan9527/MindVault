package com.mindvault.writing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.writing.controller.WritingController;
import com.mindvault.writing.service.WritingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WritingController.class)
class WritingControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private WritingService writingService;

    @Test
    void generate_shouldReturnArticle() throws Exception {
        when(writingService.generateArticle("Java 21", "技术博客", "虚拟线程,模式匹配"))
                .thenReturn("这是一篇关于Java 21的文章...");

        mockMvc.perform(post("/api/v1/writing/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\": \"Java 21\", \"style\": \"技术博客\", \"keywords\": \"虚拟线程,模式匹配\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("这是一篇关于Java 21的文章..."));
    }

    @Test
    void generate_shouldReturnErrorWhenTopicBlank() throws Exception {
        mockMvc.perform(post("/api/v1/writing/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\": \"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请提供写作主题"));
    }

    @Test
    void generate_shouldReturnErrorWhenTopicMissing() throws Exception {
        mockMvc.perform(post("/api/v1/writing/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}