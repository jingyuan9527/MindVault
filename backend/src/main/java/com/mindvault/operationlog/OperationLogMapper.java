package com.mindvault.operationlog;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.operationlog.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 操作日志 Mapper 接口。
 * <p>
 * 提供按模块查询（排除快照字段）、全量列表、带详情单条查询、计数统计等操作。
 * 默认的 listAll 和 findByModule 查询不包含 beforeSnapshot/afterSnapshot 以提高查询性能。
 * </p>
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    /**
     * 按模块查询所有日志，按创建时间降序排列（不含快照字段）。
     * @param module 模块名称
     * @return 日志列表
     */
    @Select("SELECT id, module, action, action_type, entity_id, summary, operator, " +
            "operator_id, result, duration_ms, created_at " +
            "FROM operation_log " +
            "WHERE module = #{module} ORDER BY created_at DESC")
    List<OperationLog> findByModuleOrderByCreatedAtDesc(@Param("module") String module);

    /**
     * 查询所有操作日志，按创建时间降序排列（不含快照字段）。
     * @return 全量日志列表
     */
    @Select("SELECT id, module, action, action_type, entity_id, summary, operator, " +
            "operator_id, result, duration_ms, created_at " +
            "FROM operation_log " +
            "ORDER BY created_at DESC")
    List<OperationLog> listAll();

    /**
     * 根据 ID 查询单条日志详情（含 detail 字段，不含前后快照）。
     * @param id 日志主键
     * @return 日志详情
     */
    @Select("SELECT id, module, action, action_type, entity_id, summary, detail, " +
            "operator, operator_id, ip_address, result, error_message, duration_ms, remark, created_at " +
            "FROM operation_log WHERE id = #{id}")
    OperationLog selectDetailById(@Param("id") Long id);

    /**
     * 统计日志总条数。
     * @return 总条数
     */
    @Select("SELECT COUNT(*) FROM operation_log")
    long countAll();

    /**
     * 按模块统计日志条数。
     * @param module 模块名称
     * @return 该模块的日志条数
     */
    @Select("SELECT COUNT(*) FROM operation_log WHERE module = #{module}")
    long countByModule(@Param("module") String module);
}