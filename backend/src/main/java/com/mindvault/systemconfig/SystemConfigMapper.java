package com.mindvault.systemconfig;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.systemconfig.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统配置 Mapper 接口。
 * <p>
 * 提供按 configKey 精确查询的数据库操作。
 * 继承 MyBatis-Plus BaseMapper 获得基础 CRUD。
 * </p>
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    /**
     * 根据配置键查询单条配置。
     * @param configKey 配置键
     * @return 配置实体，不存在则返回 null
     */
    @Select("SELECT * FROM system_config WHERE config_key = #{configKey} LIMIT 1")
    SystemConfig findByKey(@Param("configKey") String configKey);
}
