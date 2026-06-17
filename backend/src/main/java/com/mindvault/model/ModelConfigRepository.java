package com.mindvault.model;

import com.mindvault.model.entity.ModelConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 模型配置 Repository
 *
 * Spring Data JPA 自动实现基础 CRUD
 * 自定义查询：按 model_type 和 is_primary 查找主模型
 */
@Repository
public interface ModelConfigRepository extends JpaRepository<ModelConfig, Long> {

    /** 按类型查找启用的模型列表，按优先级排序 */
    List<ModelConfig> findByModelTypeAndIsEnabledTrueOrderByPriorityDesc(String modelType);

    /** 查找某个类型的主模型 */
    Optional<ModelConfig> findByModelTypeAndIsPrimaryTrue(String modelType);

    /** 检查某个类型是否已存在主模型 */
    boolean existsByModelTypeAndIsPrimaryTrue(String modelType);
}