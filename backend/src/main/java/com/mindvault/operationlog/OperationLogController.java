package com.mindvault.operationlog;

import com.mindvault.common.dto.ApiResponse;
import com.mindvault.operationlog.entity.OperationLog;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 操作日志查询 API
 *
 * 提供前端查看操作日志的能力
 */
@RestController
@RequestMapping("/api/v1/operation-logs")
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    /** 按模块查询操作日志 */
    @GetMapping
    public ApiResponse<List<OperationLog>> list(
            @RequestParam(required = false) String module) {
        if (module != null) {
            return ApiResponse.success(operationLogService.listByModule(module));
        }
        return ApiResponse.success(operationLogService.listAll());
    }
}