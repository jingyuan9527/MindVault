package com.mindvault.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "修改密码请求")
public record ChangePasswordRequest(
        @NotBlank @Schema(description = "旧密码") String oldPassword,
        @NotBlank @Schema(description = "新密码") String newPassword
) {}
