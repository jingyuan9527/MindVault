package com.mindvault.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "创建 API Token 请求")
public record CreateTokenRequest(
        @NotBlank @Schema(description = "Token 名称") String name,
        @Schema(description = "过期天数（不传则永不过期）") Integer expireDays
) {}
