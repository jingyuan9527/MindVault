package com.mindvault.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 修改密码请求 DTO。
 *
 * <p>需要同时提供旧密码和新密码。业务层会先验证旧密码是否匹配当前用户的
 * 密码哈希，匹配成功后才将新密码加密存储。新旧密码不能相同由前端校验，
 * 后端不做此约束。</p>
 */
@Schema(description = "修改密码请求")
public record ChangePasswordRequest(
        @NotBlank @Schema(description = "旧密码") String oldPassword,
        @NotBlank @Schema(description = "新密码") String newPassword
) {}
