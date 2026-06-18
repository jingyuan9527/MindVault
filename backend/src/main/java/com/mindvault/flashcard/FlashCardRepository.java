package com.mindvault.flashcard;

import com.mindvault.flashcard.entity.FlashCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlashCardRepository extends JpaRepository<FlashCard, Long> {
    List<FlashCard> findByKnowledgeId(Long knowledgeId);
    List<FlashCard> findBySourceType(String sourceType);
    long countByKnowledgeId(Long knowledgeId);
}