package com.mindvault.operationlog;

import com.mindvault.common.dto.ApiResponse;
import com.mindvault.common.dto.PageResult;
import com.mindvault.operationlog.entity.OperationLog;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 操作审计日志 REST 接口。
 * <p>
 * 提供操作日志的分页查询和单条详情查看功能。
 * 日志由 @OperationLog AOP 自动记录，覆盖主要业务模块的关键操作。
 * </p>
 */
@Tag(name = "操作日志", description = "用户操作行为审计日志")
@RestController
@RequestMapping("/api/v1/operation-logs")
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Operation(summary = "操作日志列表", description = "分页获取操作日志列表，可按模块筛选")
    @GetMapping
    public ApiResponse<PageResult<OperationLog>> list(
            @Parameter(description = "模块名称（可选）") @RequestParam(required = false) String module,
            @Parameter(description = "页码，从 0 开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(operationLogService.listPage(module, page, size));
    }

    @Operation(summary = "操作日志详情", description = "获取单条操作日志的完整信息，含前后快照")
    @GetMapping("/{id}")
    public ApiResponse<OperationLog> detail(@Parameter(description = "日志 ID") @PathVariable Long id) {
        OperationLog log = operationLogService.getDetail(id);
        if (log == null) {
            return ApiResponse.error(404, "日志不存在");
        }
        return ApiResponse.success(log);
    }
}