package com.mindvault.operationlog;

import com.mindvault.common.dto.ApiResponse;
import com.mindvault.operationlog.entity.OperationLog;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "操作日志", description = "用户操作行为审计日志")
@RestController
@RequestMapping("/api/v1/operation-logs")
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Operation(summary = "操作日志列表", description = "获取操作日志列表，可按模块筛选")
    @GetMapping
    public ApiResponse<List<OperationLog>> list(
            @Parameter(description = "模块名称（可选）") @RequestParam(required = false) String module) {
        if (module != null) {
            return ApiResponse.success(operationLogService.listByModule(module));
        }
        return ApiResponse.success(operationLogService.listAll());
    }
}