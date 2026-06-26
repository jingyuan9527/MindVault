package com.mindvault.flashcard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.flashcard.controller.FlashCardController;
import com.mindvault.flashcard.entity.FlashCard;
import com.mindvault.flashcard.service.FlashCardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FlashCardController.class)
class FlashCardControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private FlashCardService flashCardService;

    private FlashCard createCard(Long id, String question) {
        FlashCard card = new FlashCard();
        card.setId(id);
        card.setKnowledgeId(1L);
        card.setQuestion(question);
        card.setAnswer("Answer " + question);
        card.setCreatedAt(LocalDateTime.now());
        return card;
    }

    @Test
    void listAll_shouldReturnCards() throws Exception {
        when(flashCardService.listAll()).thenReturn(List.of(createCard(1L, "Q1")));

        mockMvc.perform(get("/api/v1/flashcards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].question").value("Q1"));
    }

    @Test
    void listByKnowledge_shouldReturnCards() throws Exception {
        when(flashCardService.listByKnowledge(1L)).thenReturn(List.of(createCard(1L, "Q1")));

        mockMvc.perform(get("/api/v1/flashcards/knowledge/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].question").value("Q1"));
    }

    @Test
    void generate_shouldReturnCards() throws Exception {
        when(flashCardService.generateCards(1L)).thenReturn(List.of(createCard(1L, "GenQ")));

        mockMvc.perform(post("/api/v1/flashcards/generate/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].question").value("GenQ"));
    }

    @Test
    void delete_shouldSucceed() throws Exception {
        doNothing().when(flashCardService).deleteCard(1L);

        mockMvc.perform(delete("/api/v1/flashcards/1"))
                .andExpect(status().isOk());
    }
}