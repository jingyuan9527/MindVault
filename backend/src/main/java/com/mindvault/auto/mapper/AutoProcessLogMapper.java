package com.mindvault.auto.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.auto.entity.AutoProcessLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 自动处理日志数据访问层 */
@Mapper
public interface AutoProcessLogMapper extends BaseMapper<AutoProcessLog> {

    @Select("SELECT * FROM auto_process_log WHERE knowledge_id = #{knowledgeId} ORDER BY created_at DESC")
    List<AutoProcessLog> findByKnowledgeId(@Param("knowledgeId") Long knowledgeId);
}