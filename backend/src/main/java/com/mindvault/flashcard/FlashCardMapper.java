package com.mindvault.flashcard;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.flashcard.entity.FlashCard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FlashCardMapper extends BaseMapper<FlashCard> {

    @Select("SELECT * FROM flash_card WHERE knowledge_id = #{knowledgeId}")
    List<FlashCard> findByKnowledgeId(@Param("knowledgeId") Long knowledgeId);

    @Select("SELECT * FROM flash_card WHERE source_type = #{sourceType}")
    List<FlashCard> findBySourceType(@Param("sourceType") String sourceType);

    @Select("SELECT COUNT(*) FROM flash_card WHERE knowledge_id = #{knowledgeId}")
    long countByKnowledgeId(@Param("knowledgeId") Long knowledgeId);
}