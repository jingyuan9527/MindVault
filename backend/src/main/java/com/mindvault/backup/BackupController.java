package com.mindvault.backup;

import com.mindvault.common.dto.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/backup")
public class BackupController {

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    @PostMapping
    public ApiResponse<Map<String, String>> createBackup() {
        String filename = backupService.createBackup();
        return ApiResponse.success(Map.of("filename", filename));
    }

    @GetMapping
    public ApiResponse<List<String>> listBackups() {
        return ApiResponse.success(backupService.listBackups());
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<byte[]> downloadBackup(@PathVariable String filename) {
        byte[] data = backupService.getBackup(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_JSON)
                .body(data);
    }
}