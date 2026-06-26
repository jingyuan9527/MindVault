package com.mindvault.model;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.model.entity.ModelConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/**
 * 模型配置 Mapper 接口。
 * <p>
 * 提供主模型查询、按类型查询可用模型（按优先级降序）、主模型存在性检查等操作。
 * 继承 MyBatis-Plus BaseMapper 获得基础 CRUD。
 * </p>
 */
@Mapper
public interface ModelConfigMapper extends BaseMapper<ModelConfig> {

    /**
     * 按模型类型查找主模型（is_primary = true）。
     * @param modelType 模型类型（CHAT / EMBEDDING）
     * @return 主模型配置，不存在则返回空
     */
    @Select("SELECT * FROM model_config WHERE model_type = #{modelType} AND is_primary = true LIMIT 1")
    Optional<ModelConfig> findByModelTypeAndIsPrimaryTrue(@Param("modelType") String modelType);

    /**
     * 按模型类型查找所有已启用的模型，按优先级降序排列。
     * @param modelType 模型类型（CHAT / EMBEDDING）
     * @return 可用模型列表
     */
    @Select("SELECT * FROM model_config WHERE model_type = #{modelType} AND is_enabled = true ORDER BY priority DESC")
    List<ModelConfig> findByModelTypeAndIsEnabledTrueOrderByPriorityDesc(@Param("modelType") String modelType);

    /**
     * 检查指定类型是否存在主模型。
     * @param modelType 模型类型（CHAT / EMBEDDING）
     * @return 是否存在主模型
     */
    @Select("SELECT COUNT(*) > 0 FROM model_config WHERE model_type = #{modelType} AND is_primary = true")
    boolean existsByModelTypeAndIsPrimaryTrue(@Param("modelType") String modelType);
}