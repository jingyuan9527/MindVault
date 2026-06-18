package com.mindvault.backup;

import com.mindvault.common.annotation.OperationLog;
import com.mindvault.common.dto.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "数据备份", description = "数据库的备份与恢复")
@RestController
@RequestMapping("/api/v1/backup")
public class BackupController {

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    @OperationLog(module = "backup", action = "create", description = "创建数据库备份")
    @Operation(summary = "创建备份", description = "创建当前数据库的完整备份")
    @PostMapping
    public ApiResponse<Map<String, String>> createBackup() {
        String filename = backupService.createBackup();
        return ApiResponse.success(Map.of("filename", filename));
    }

    @Operation(summary = "备份列表", description = "获取所有备份文件列表")
    @GetMapping
    public ApiResponse<List<String>> listBackups() {
        return ApiResponse.success(backupService.listBackups());
    }

    @Operation(summary = "下载备份", description = "下载指定文件名的备份文件")
    @GetMapping("/download/{filename}")
    public ResponseEntity<byte[]> downloadBackup(@Parameter(description = "备份文件名") @PathVariable String filename) {
        byte[] data = backupService.getBackup(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_JSON)
                .body(data);
    }
}