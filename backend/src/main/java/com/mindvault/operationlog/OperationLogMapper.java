package com.mindvault.operationlog;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.operationlog.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    @Select("SELECT id, module, action, action_type, entity_id, summary, operator, " +
            "operator_id, result, duration_ms, created_at " +
            "FROM operation_log " +
            "WHERE module = #{module} ORDER BY created_at DESC")
    List<OperationLog> findByModuleOrderByCreatedAtDesc(@Param("module") String module);

    @Select("SELECT id, module, action, action_type, entity_id, summary, operator, " +
            "operator_id, result, duration_ms, created_at " +
            "FROM operation_log " +
            "ORDER BY created_at DESC")
    List<OperationLog> listAll();

    @Select("SELECT id, module, action, action_type, entity_id, summary, detail, " +
            "operator, operator_id, ip_address, result, error_message, duration_ms, remark, created_at " +
            "FROM operation_log WHERE id = #{id}")
    OperationLog selectDetailById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM operation_log")
    long countAll();

    @Select("SELECT COUNT(*) FROM operation_log WHERE module = #{module}")
    long countByModule(@Param("module") String module);
}