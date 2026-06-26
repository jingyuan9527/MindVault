package com.mindvault.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求 DTO。
 *
 * <p>接收用户名和密码两个必填字段。使用 Jakarta Validation @NotBlank
 * 确保空字符串不会被传递到业务层。密码字段在日志和 API 响应中不会明文输出。</p>
 */
@Schema(description = "登录请求")
public record LoginRequest(
        @NotBlank @Schema(description = "用户名") String username,
        @NotBlank @Schema(description = "密码") String password
) {}
