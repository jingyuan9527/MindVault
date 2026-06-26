package com.mindvault.auth.controller;

import com.mindvault.auth.config.SessionManager;
import com.mindvault.auth.config.UserContext;
import com.mindvault.auth.dto.ChangePasswordRequest;
import com.mindvault.auth.dto.CreateTokenRequest;
import com.mindvault.auth.dto.LoginRequest;
import com.mindvault.auth.entity.ApiToken;
import com.mindvault.auth.entity.User;
import com.mindvault.auth.service.ApiTokenService;
import com.mindvault.auth.service.UserService;
import com.mindvault.common.annotation.OperationLog;
import com.mindvault.common.dto.ApiResponse;
import com.mindvault.common.annotation.RateLimit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证控制器，提供登录、登出、当前用户信息、密码修改及 API 令牌管理接口。
 *
 * <p>所有接口都需要认证（除 /login 外），认证通过后使用 SessionManager
 * 维护会话状态，或通过 ApiTokenService 管理长期令牌。登录成功时返回会话
 * 令牌及用户基本信息；API 令牌相关接口供用户在个人设置中管理。</p>
 *
 * <p>敏感操作（登录、修改密码、创建/删除令牌）均记录 @OperationLog 审计日志。
 * 登录接口还受 @RateLimit 限流保护（5 次/60 秒），防止暴力破解。</p>
 *
 * @see SessionManager
 * @see UserService
 * @see ApiTokenService
 */
@Tag(name = "认证", description = "用户认证与 API Token 管理")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final ApiTokenService apiTokenService;
    private final SessionManager sessionManager;

    public AuthController(UserService userService, ApiTokenService apiTokenService, SessionManager sessionManager) {
        this.userService = userService;
        this.apiTokenService = apiTokenService;
        this.sessionManager = sessionManager;
    }

    @OperationLog(module = "认证", action = "用户登录", actionType = "OTHER")
    @RateLimit(capacity = 5, duration = 60, key = "login")
    @Operation(summary = "登录", description = "用户名密码登录，返回会话令牌")
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.authenticate(request.username(), request.password());
        if (user == null) {
            return ApiResponse.error(401, "用户名或密码错误");
        }
        String token = sessionManager.createSession(user);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("displayName", user.getDisplayName());
        data.put("role", user.getRole());
        return ApiResponse.success(data);
    }

    @OperationLog(module = "认证", action = "用户登出", actionType = "OTHER")
    @Operation(summary = "退出登录", description = "清除会话令牌")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            sessionManager.remove(authHeader.substring(7));
        }
        return ApiResponse.success(null);
    }

    @Operation(summary = "当前用户信息", description = "获取当前登录用户的信息")
    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        UserContext.UserInfo info = UserContext.get();
        if (info == null) {
            return ApiResponse.error(401, "未登录");
        }
        User user = userService.getById(info.userId());
        if (user == null) {
            return ApiResponse.error(401, "用户不存在");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("displayName", user.getDisplayName());
        data.put("role", user.getRole());
        return ApiResponse.success(data);
    }

    @OperationLog(module = "认证", action = "修改密码", actionType = "UPDATE", entityType = User.class)
    @Operation(summary = "修改密码", description = "修改当前登录用户的密码")
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        UserContext.UserInfo info = UserContext.get();
        if (info == null) {
            return ApiResponse.error(401, "未登录");
        }
        boolean ok = userService.changePassword(info.userId(), request.oldPassword(), request.newPassword());
        if (!ok) {
            return ApiResponse.error(400, "旧密码错误");
        }
        return ApiResponse.success(null);
    }

    @OperationLog(module = "认证", action = "创建 API Token", actionType = "CREATE")
    @Operation(summary = "创建 API Token", description = "创建新的 API 访问令牌")
    @PostMapping("/tokens")
    public ApiResponse<Map<String, Object>> createToken(@Valid @RequestBody CreateTokenRequest request) {
        UserContext.UserInfo info = UserContext.get();
        if (info == null) {
            return ApiResponse.error(401, "未登录");
        }
        ApiToken token = apiTokenService.createToken(info.userId(), request.name(), request.expireDays());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", token.getId());
        data.put("token", token.getToken());
        data.put("name", token.getName());
        data.put("expiresAt", token.getExpiresAt() != null ? token.getExpiresAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        return ApiResponse.success(data);
    }

    @Operation(summary = "API Token 列表", description = "获取当前用户的所有 API 令牌")
    @GetMapping("/tokens")
    public ApiResponse<List<Map<String, Object>>> listTokens() {
        UserContext.UserInfo info = UserContext.get();
        if (info == null) {
            return ApiResponse.error(401, "未登录");
        }
        List<ApiToken> tokens = apiTokenService.listByUser(info.userId());
        List<Map<String, Object>> result = tokens.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("lastUsedAt", t.getLastUsedAt() != null ? t.getLastUsedAt().toString() : null);
            m.put("expiresAt", t.getExpiresAt() != null ? t.getExpiresAt().toString() : null);
            m.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().toString() : null);
            return m;
        }).toList();
        return ApiResponse.success(result);
    }

    @OperationLog(module = "认证", action = "删除 API Token", actionType = "DELETE")
    @Operation(summary = "删除 API Token", description = "删除指定 ID 的 API 令牌")
    @DeleteMapping("/tokens/{id}")
    public ApiResponse<Void> deleteToken(@PathVariable Long id) {
        UserContext.UserInfo info = UserContext.get();
        if (info == null) {
            return ApiResponse.error(401, "未登录");
        }
        apiTokenService.deleteToken(info.userId(), id);
        return ApiResponse.success(null);
    }
}
