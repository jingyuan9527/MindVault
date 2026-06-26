package com.mindvault.backup.service;

import com.mindvault.common.service.MetricsService;
import com.mindvault.knowledge.service.KnowledgeService;
import com.mindvault.operationlog.service.OperationLogService;
import com.mindvault.systemconfig.service.SystemConfigService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class BackupServiceImpl implements BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupServiceImpl.class);

    private final KnowledgeService knowledgeService;
    private final OperationLogService operationLogService;
    private final MetricsService metricsService;
    private final SystemConfigService config;

    @Value("${mindvault.backup.dir:backups}")
    private String backupDir;

    @Value("${mindvault.backup.retention-days:7}")
    private int retentionDays;

    public BackupServiceImpl(KnowledgeService knowledgeService,
                             OperationLogService operationLogService,
                             MetricsService metricsService,
                             SystemConfigService config) {
        this.knowledgeService = knowledgeService;
        this.operationLogService = operationLogService;
        this.metricsService = metricsService;
        this.config = config;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(backupDir));
            log.info("备份目录初始化: {}", Paths.get(backupDir).toAbsolutePath());
        } catch (Exception e) {
            log.warn("创建备份目录失败: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledBackup() {
        if (!config.getBool("task.backup.enabled", true)) return;
        log.info("开始定时自动备份...");
        try {
            String filename = createBackup();
            log.info("自动备份完成: {}", filename);
            cleanOldBackups();
        } catch (Exception e) {
            log.error("自动备份失败: {}", e.getMessage());
        }
    }

    @Override
    public String createBackup() {
        String json = knowledgeService.exportAllAsJson();
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String filename = "mindvault-backup-" + dateStr + ".json";

        try {
            Path filePath = Paths.get(backupDir, filename);
            Files.writeString(filePath, json, StandardCharsets.UTF_8);
            log.info("备份创建成功: {} ({} bytes)", filename, json.getBytes(StandardCharsets.UTF_8).length);
            metricsService.recordBackup();
            operationLogService.log("SYSTEM", "BACKUP", null, "自动备份: " + filename);
            return filename;
        } catch (Exception e) {
            log.error("创建备份文件失败: {}", e.getMessage());
            throw new RuntimeException("备份失败: " + e.getMessage());
        }
    }

    @Override
    public List<String> listBackups() {
        File dir = new File(backupDir);
        File[] files = dir.listFiles((d, name) -> name.startsWith("mindvault-backup-") && name.endsWith(".json"));
        if (files == null) return List.of();

        return Arrays.stream(files)
                .map(File::getName)
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    @Override
    public byte[] getBackup(String filename) {
        try {
            Path filePath = Paths.get(backupDir, filename);
            if (!Files.exists(filePath)) {
                throw new IllegalArgumentException("备份文件不存在: " + filename);
            }
            return Files.readAllBytes(filePath);
        } catch (Exception e) {
            throw new RuntimeException("读取备份文件失败: " + e.getMessage());
        }
    }

    @Override
    public void cleanOldBackups() {
        File dir = new File(backupDir);
        File[] files = dir.listFiles((d, name) -> name.startsWith("mindvault-backup-") && name.endsWith(".json"));
        if (files == null || files.length <= retentionDays) return;

        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        int toDelete = files.length - retentionDays;
        for (int i = 0; i < toDelete; i++) {
            if (files[i].delete()) {
                log.info("删除旧备份: {}", files[i].getName());
            }
        }
    }
}