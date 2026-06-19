package com.mindvault.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.auth.entity.ApiToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ApiTokenMapper extends BaseMapper<ApiToken> {
    @Select("SELECT * FROM api_tokens WHERE token = #{token} LIMIT 1")
    ApiToken findByToken(String token);

    @Select("SELECT * FROM api_tokens WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<ApiToken> findByUserId(Long userId);

    @Update("UPDATE api_tokens SET last_used_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    void updateLastUsedAt(Long id);
}
