package com.mindvault.relation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.relation.entity.KnowledgeRelation;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识关联数据访问层
 *
 * 自定义查询：
 * - findByKnowledgeId: 以某知识为主体的关联列表
 * - findByRelatedId: 以某知识为客体的关联列表（反向查询）
 * - countByKnowledgeId: 关联数统计
 * - deleteByKnowledgeId: 级联删除（用于知识删除时清除关联）
 */
@Mapper
public interface KnowledgeRelationMapper extends BaseMapper<KnowledgeRelation> {

    @Select("SELECT * FROM knowledge_relation WHERE knowledge_id = #{knowledgeId} ORDER BY score DESC")
    List<KnowledgeRelation> findByKnowledgeId(@Param("knowledgeId") Long knowledgeId);

    @Select("SELECT kr.* FROM knowledge_relation kr " +
           "WHERE kr.related_id = #{knowledgeId} " +
           "ORDER BY kr.score DESC")
    List<KnowledgeRelation> findByRelatedId(@Param("knowledgeId") Long knowledgeId);

    @Select("SELECT COUNT(*) FROM knowledge_relation WHERE knowledge_id = #{knowledgeId}")
    int countByKnowledgeId(@Param("knowledgeId") Long knowledgeId);

    @Delete("DELETE FROM knowledge_relation WHERE knowledge_id = #{knowledgeId}")
    void deleteByKnowledgeId(@Param("knowledgeId") Long knowledgeId);
}
