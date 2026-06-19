package com.mindvault.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Select("SELECT * FROM users WHERE username = #{username} LIMIT 1")
    User findByUsername(String username);
}
