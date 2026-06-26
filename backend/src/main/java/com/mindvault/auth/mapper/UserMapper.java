package com.mindvault.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户数据访问接口，继承 MyBatis-Plus 的 BaseMapper 获得通用 CRUD 能力。
 *
 * <p>除通用方法外，提供按用户名精确查找的自定义查询——该查询在登录认证
 * 和用户创建时的重名校验中均被使用。用户名具有 UNIQUE 约束，故 LIMIT 1
 * 即为安全保证。</p>
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查找用户。
     *
     * @param username 用户名（精确匹配）
     * @return 匹配的用户实体，未找到时返回 null
     */
    @Select("SELECT * FROM users WHERE username = #{username} LIMIT 1")
    User findByUsername(String username);
}
