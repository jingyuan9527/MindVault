package com.mindvault.operationlog;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.operationlog.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    @Select("SELECT * FROM operation_log WHERE module = #{module} ORDER BY created_at DESC")
    List<OperationLog> findByModuleOrderByCreatedAtDesc(@Param("module") String module);
}