package com.mindvault.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体，映射数据库 users 表。
 *
 * <p>负责承载用户认证与授权所需的全部字段，包括登录凭证(用户名/密码哈希)、
 * 显示名称、角色及启用状态。密码哈希字段通过 @JsonProperty(WRITE_ONLY)
 * 确保序列化时不会泄漏到 API 响应中。</p>
 *
 * <p>角色字段当前支持 "USER"(普通用户) 和 "ADMIN"(管理员) 两种取值，
 * 默认值为 "USER"。enabled 字段用于实现账户禁用而不删除的能力。</p>
 *
 * @see UserMapper
 * @see UserService
 */
@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("username")
    private String username;

    @TableField("password_hash")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String passwordHash;

    @TableField("display_name")
    private String displayName;

    @TableField("role")
    private String role = "USER";

    @TableField("enabled")
    private Boolean enabled = true;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
