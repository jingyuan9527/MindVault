package com.mindvault.flashcard;

import com.mindvault.agent.config.AgentConfig;
import com.mindvault.flashcard.entity.FlashCard;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.model.ModelConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlashCardServiceTest {

    @Mock private ModelConfigService modelConfigService;
    @Mock private AgentConfig agentConfig;
    @Mock private KnowledgeService knowledgeService;
    @Mock private FlashCardMapper mapper;

    private FlashCardService service;

    @BeforeEach
    void setUp() {
        service = new FlashCardService(modelConfigService, agentConfig, knowledgeService, mapper);
    }

    private FlashCard createCard(Long id, Long knowledgeId, String question, String answer) {
        FlashCard card = new FlashCard();
        card.setId(id);
        card.setKnowledgeId(knowledgeId);
        card.setQuestion(question);
        card.setAnswer(answer);
        card.setDifficulty("MEDIUM");
        card.setSourceType("AUTO");
        card.setCreatedAt(LocalDateTime.now());
        return card;
    }

    @Test
    void listByKnowledge_shouldDelegate() {
        Long knowledgeId = 1L;
        List<FlashCard> expected = List.of(
                createCard(1L, knowledgeId, "Q1", "A1"),
                createCard(2L, knowledgeId, "Q2", "A2")
        );
        when(mapper.findByKnowledgeId(knowledgeId)).thenReturn(expected);

        List<FlashCard> result = service.listByKnowledge(knowledgeId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Q1", result.get(0).getQuestion());
        verify(mapper).findByKnowledgeId(knowledgeId);
    }

    @Test
    void listAll_shouldDelegate() {
        List<FlashCard> expected = List.of(
                createCard(1L, 1L, "Q1", "A1"),
                createCard(2L, 1L, "Q2", "A2")
        );
        when(mapper.selectList(null)).thenReturn(expected);

        List<FlashCard> result = service.listAll();

        assertEquals(2, result.size());
        verify(mapper).selectList(null);
    }

    @Test
    void listBySourceType_shouldDelegate() {
        String sourceType = "AUTO";
        List<FlashCard> expected = List.of(createCard(1L, 1L, "Q1", "A1"));
        when(mapper.findBySourceType(sourceType)).thenReturn(expected);

        List<FlashCard> result = service.listBySourceType(sourceType);

        assertEquals(1, result.size());
        verify(mapper).findBySourceType(sourceType);
    }

    @Test
    void countByKnowledge_shouldDelegate() {
        Long knowledgeId = 1L;
        when(mapper.countByKnowledgeId(knowledgeId)).thenReturn(3L);

        long count = service.countByKnowledge(knowledgeId);

        assertEquals(3L, count);
        verify(mapper).countByKnowledgeId(knowledgeId);
    }

    @Test
    void deleteCard_shouldDeleteById() {
        Long cardId = 42L;

        service.deleteCard(cardId);

        verify(mapper).deleteById(cardId);
    }
}