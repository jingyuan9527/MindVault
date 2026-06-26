package com.mindvault.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.model.entity.ModelConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ModelConfigMapper extends BaseMapper<ModelConfig> {

    @Select("SELECT * FROM model_config WHERE model_type = #{modelType} AND is_primary = true LIMIT 1")
    Optional<ModelConfig> findByModelTypeAndIsPrimaryTrue(@Param("modelType") String modelType);

    @Select("SELECT * FROM model_config WHERE model_type = #{modelType} AND is_enabled = true ORDER BY priority DESC")
    List<ModelConfig> findByModelTypeAndIsEnabledTrueOrderByPriorityDesc(@Param("modelType") String modelType);

    @Select("SELECT COUNT(*) > 0 FROM model_config WHERE model_type = #{modelType} AND is_primary = true")
    boolean existsByModelTypeAndIsPrimaryTrue(@Param("modelType") String modelType);
}