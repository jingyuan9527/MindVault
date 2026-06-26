package com.mindvault.systemconfig.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.systemconfig.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    @Select("SELECT * FROM system_config WHERE config_key = #{configKey} LIMIT 1")
    SystemConfig findByKey(@Param("configKey") String configKey);
}