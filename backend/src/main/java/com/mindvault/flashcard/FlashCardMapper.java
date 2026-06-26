package com.mindvault.flashcard;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.flashcard.entity.FlashCard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 闪卡数据访问层。
 * <p>继承 MyBatis-Plus BaseMapper 提供标准 CRUD。
 * 提供按知识 ID 查询、按来源类型查询、按知识 ID 计数等自定义查询。</p>
 */
@Mapper
public interface FlashCardMapper extends BaseMapper<FlashCard> {

    /**
     * 查询指定知识关联的所有闪卡。
     * @param knowledgeId 知识 ID
     * @return 闪卡列表
     */
    @Select("SELECT * FROM flash_card WHERE knowledge_id = #{knowledgeId}")
    List<FlashCard> findByKnowledgeId(@Param("knowledgeId") Long knowledgeId);

    /**
     * 按来源类型查询闪卡。
     * @param sourceType 来源类型（AUTO / MANUAL）
     * @return 闪卡列表
     */
    @Select("SELECT * FROM flash_card WHERE source_type = #{sourceType}")
    List<FlashCard> findBySourceType(@Param("sourceType") String sourceType);

    /**
     * 统计指定知识的闪卡数量。
     * @param knowledgeId 知识 ID
     * @return 闪卡数量
     */
    @Select("SELECT COUNT(*) FROM flash_card WHERE knowledge_id = #{knowledgeId}")
    long countByKnowledgeId(@Param("knowledgeId") Long knowledgeId);
}