package com.mindvault.backup;

import com.mindvault.common.service.MetricsService;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.operationlog.OperationLogService;
import com.mindvault.systemconfig.SystemConfigService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 数据备份服务。
 * <p>
 * 核心职责: 将知识库数据导出为 JSON 并写入文件系统，支持定时自动备份、
 * 备份列表查询、文件下载和旧备份清理。
 * </p>
 * <p>
 * 关键设计:
 * <ul>
 *   <li>备份以 JSON 文件存储，文件名格式: mindvault-backup-yyyyMMdd-HHmmss.json</li>
 *   <li>每天凌晨 3:00 自动执行定时备份，可通过 system_config 开关控制</li>
 *   <li>旧备份清理策略: 保留 retention-days 配置天数内的文件，超出的按修改时间删除最旧的</li>
 *   <li>每次备份自动记录操作日志并更新备份指标计数</li>
 * </ul>
 * </p>
 * <p>依赖: KnowledgeService, OperationLogService, MetricsService, SystemConfigService</p>
 */
@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    private final KnowledgeService knowledgeService;
    private final OperationLogService operationLogService;
    private final MetricsService metricsService;
    private final SystemConfigService config;

    @Value("${mindvault.backup.dir:backups}")
    private String backupDir;

    @Value("${mindvault.backup.retention-days:7}")
    private int retentionDays;

    public BackupService(KnowledgeService knowledgeService,
                         OperationLogService operationLogService,
                         MetricsService metricsService,
                         SystemConfigService config) {
        this.knowledgeService = knowledgeService;
        this.operationLogService = operationLogService;
        this.metricsService = metricsService;
        this.config = config;
    }

    /**
     * 初始化备份目录。在服务启动时自动创建配置的备份目录（如不存在）。
     */
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(backupDir));
            log.info("备份目录初始化: {}", Paths.get(backupDir).toAbsolutePath());
        } catch (Exception e) {
            log.warn("创建备份目录失败: {}", e.getMessage());
        }
    }

    /**
     * 定时任务: 每天凌晨 3:00 自动创建备份并清理过期文件。
     * 可通过 task.backup.enabled 配置项开关。
     */
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

    /**
     * 创建一次新的备份。
     * 将知识库全部数据导出为 JSON，写入备份目录，记录操作日志和备份指标。
     * @return 备份文件名
     * @throws RuntimeException 当文件写入失败时
     */
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

    /**
     * 列出所有备份文件，按文件名倒序排列（最新的在前）。
     * @return 备份文件名列表
     */
    public List<String> listBackups() {
        File dir = new File(backupDir);
        File[] files = dir.listFiles((d, name) -> name.startsWith("mindvault-backup-") && name.endsWith(".json"));
        if (files == null) return List.of();

        return Arrays.stream(files)
                .map(File::getName)
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    /**
     * 读取指定备份文件的字节内容。
     * @param filename 备份文件名
     * @return 文件字节数据
     * @throws IllegalArgumentException 文件不存在时
     * @throws RuntimeException 读取失败时
     */
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

    /**
     * 清理过期备份文件。
     * 保留最近 retentionDays 个备份，删除其余更早的文件。
     * 如果文件总数不超过保留天数则不执行清理。
     */
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