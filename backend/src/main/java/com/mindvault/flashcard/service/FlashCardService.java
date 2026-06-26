package com.mindvault.flashcard.service;

import com.mindvault.flashcard.entity.FlashCard;

import java.util.List;

/**
 * 闪卡服务接口。
 * <p>提供闪卡的自动生成（基于 LLM）、查询和删除功能。</p>
 */
public interface FlashCardService {

    List<FlashCard> generateCards(Long knowledgeId);

    List<FlashCard> listByKnowledge(Long knowledgeId);

    List<FlashCard> listAll();

    List<FlashCard> listBySourceType(String sourceType);

    long countByKnowledge(Long knowledgeId);

    void deleteCard(Long id);
}