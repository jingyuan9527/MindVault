package com.mindvault.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 统一 API 响应格式
 *
 * 所有 RestController 返回值统一封装为此类，
 * 前端基于 code 判断请求是否成功（0 成功，非 0 错误）。
 * 提供三个静态工厂方法简化构造：
 * - success(data): 成功响应
 * - success(message, data): 成功响应（自定义消息）
 * - error(code, message): 错误响应
 * - error(message): 错误响应（默认 500）
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一 API 响应格式")
public class ApiResponse<T> {
    @Schema(description = "响应码（0 成功，非 0 错误）")
    private int code;
    @Schema(description = "响应消息")
    private String message;
    @Schema(description = "响应数据")
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(0, message, data);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null);
    }
}