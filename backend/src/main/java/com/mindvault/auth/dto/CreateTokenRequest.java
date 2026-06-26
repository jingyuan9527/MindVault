package com.mindvault.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 创建 API Token 请求 DTO。
 *
 * <p>令牌名称(name)为必填，用于在 UI 上区分不同用途的令牌。
 * expireDays 为可选参数，不传时创建永不过期的令牌；传入 >0 的值时，
 * 在创建时间基础上增加对应天数作为过期时间。</p>
 */
@Schema(description = "创建 API Token 请求")
public record CreateTokenRequest(
        @NotBlank @Schema(description = "Token 名称") String name,
        @Schema(description = "过期天数（不传则永不过期）") Integer expireDays
) {}
