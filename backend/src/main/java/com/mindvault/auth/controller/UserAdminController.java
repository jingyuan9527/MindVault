package com.mindvault.auth.controller;

import com.mindvault.auth.config.UserContext;
import com.mindvault.auth.entity.User;
import com.mindvault.auth.service.UserService;
import com.mindvault.common.annotation.OperationLog;
import com.mindvault.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器（管理员专用），提供用户列表查看和状态管理功能。
 *
 * <p>所有接口在执行前校验当前用户角色是否为 ADMIN，非管理员返回 403。
 * 用户列表返回不含密码哈希的安全视图；启用/禁用操作记录 @OperationLog
 * 审计日志，并支持快照记录以便追溯变更历史。</p>
 *
 * @see UserService
 * @see UserContext
 */
@Tag(name = "用户管理", description = "管理员用户管理接口")
@RestController
@RequestMapping("/api/v1/users")
public class UserAdminController {

    private final UserService userService;

    public UserAdminController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "用户列表", description = "获取所有用户列表（仅管理员）")
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listUsers() {
        UserContext.UserInfo current = UserContext.get();
        if (current == null || !"ADMIN".equals(current.role())) {
            return ApiResponse.error(403, "无权限");
        }
        List<User> users = userService.listAll();
        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("displayName", u.getDisplayName());
            m.put("role", u.getRole());
            m.put("enabled", u.getEnabled());
            m.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
            return m;
        }).toList();
        return ApiResponse.success(result);
    }

    @OperationLog(module = "用户管理", action = "启用/禁用用户", actionType = "UPDATE", entityType = User.class, recordSnapshot = true)
    @Operation(summary = "启用/禁用用户", description = "设置用户启用状态（仅管理员）")
    @PutMapping("/{id}/enabled")
    public ApiResponse<Void> setEnabled(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        UserContext.UserInfo current = UserContext.get();
        if (current == null || !"ADMIN".equals(current.role())) {
            return ApiResponse.error(403, "无权限");
        }
        boolean enabled = body.getOrDefault("enabled", true);
        userService.setEnabled(id, enabled);
        return ApiResponse.success(null);
    }
}
