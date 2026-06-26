package com.mindvault.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.auth.entity.ApiToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * API 令牌数据访问接口，继承 MyBatis-Plus 的 BaseMapper 获得通用 CRUD 能力。
 *
 * <p>提供令牌字符串查询、按用户列出、更新最后使用时间三个自定义操作。
 * 令牌字符串为 64 位无分隔符 UUID，在 api_tokens 表上具有 UNIQUE 索引。</p>
 *
 * @see com.mindvault.auth.entity.ApiToken
 */
@Mapper
public interface ApiTokenMapper extends BaseMapper<ApiToken> {

    /**
     * 根据令牌字符串查找令牌。
     *
     * @param token 完整的 64 位令牌字符串
     * @return 匹配的令牌实体，未找到时返回 null
     */
    @Select("SELECT * FROM api_tokens WHERE token = #{token} LIMIT 1")
    ApiToken findByToken(String token);

    /**
     * 查询指定用户的所有令牌，按创建时间倒序排列。
     *
     * @param userId 用户 ID
     * @return 该用户的令牌列表（可能为空）
     */
    @Select("SELECT * FROM api_tokens WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<ApiToken> findByUserId(Long userId);

    /**
     * 更新令牌的最后使用时间戳为数据库当前时间。
     *
     * @param id 令牌 ID
     */
    @Update("UPDATE api_tokens SET last_used_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    void updateLastUsedAt(Long id);
}
