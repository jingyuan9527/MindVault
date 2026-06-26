package com.mindvault.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API 令牌实体，映射数据库 api_tokens 表。
 *
 * <p>用于长期有效的程序化访问认证，区别于 Session 的短生命周期。
 * 每个令牌关联一个用户(userId)，支持过期时间(expiresAt)和最后使用
 * 时间(lastUsedAt)追踪。令牌本身为 64 位无分隔符 UUID 字符串。</p>
 *
 * <p>令牌与用户为多对一关系，删除用户时级联删除关联令牌
 * (ON DELETE CASCADE)。</p>
 *
 * @see ApiTokenMapper
 * @see ApiTokenService
 */
@Data
@TableName("api_tokens")
public class ApiToken {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("token")
    private String token;

    @TableField("name")
    private String name;

    @TableField("last_used_at")
    private LocalDateTime lastUsedAt;

    @TableField("expires_at")
    private LocalDateTime expiresAt;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
