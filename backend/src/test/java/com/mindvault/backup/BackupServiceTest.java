package com.mindvault.backup;

import com.mindvault.common.service.MetricsService;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.operationlog.OperationLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    @Mock private KnowledgeService knowledgeService;
    @Mock private OperationLogService operationLogService;
    @Mock private MetricsService metricsService;

    private BackupService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new BackupService(knowledgeService, operationLogService, metricsService);
        ReflectionTestUtils.setField(service, "backupDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "retentionDays", 7);
    }

    @Test
    void createBackup_shouldWriteFile() {
        when(knowledgeService.exportAllAsJson()).thenReturn("{\"count\":0,\"items\":[]}");

        String filename = service.createBackup();

        assertTrue(filename.startsWith("mindvault-backup-"));
        assertTrue(filename.endsWith(".json"));
        assertTrue(tempDir.resolve(filename).toFile().exists());
        verify(operationLogService).log(eq("SYSTEM"), eq("BACKUP"), isNull(), contains(filename));
    }

    @Test
    void createBackup_shouldThrowOnFailure() {
        when(knowledgeService.exportAllAsJson()).thenThrow(new RuntimeException("export failed"));

        assertThrows(RuntimeException.class, () -> service.createBackup());
    }

    @Test
    void listBackups_shouldReturnSorted() throws Exception {
        when(knowledgeService.exportAllAsJson()).thenReturn("{}");
        service.createBackup();
        Thread.sleep(1100);
        service.createBackup();

        List<String> backups = service.listBackups();

        assertEquals(2, backups.size());
        assertTrue(backups.get(0).compareTo(backups.get(1)) > 0);
    }

    @Test
    void listBackups_shouldReturnEmptyWhenNoBackups() {
        List<String> backups = service.listBackups();

        assertTrue(backups.isEmpty());
    }

    @Test
    void getBackup_shouldReturnContent() {
        when(knowledgeService.exportAllAsJson()).thenReturn("{\"data\":\"test\"}");
        String filename = service.createBackup();

        byte[] content = service.getBackup(filename);

        assertNotNull(content);
        assertTrue(content.length > 0);
    }

    @Test
    void getBackup_shouldThrowWhenNotFound() {
        assertThrows(RuntimeException.class, () -> service.getBackup("nonexistent.json"));
    }

    @Test
    void init_shouldCreateDirectory() {
        Path newDir = tempDir.resolve("new-backup-dir");
        ReflectionTestUtils.setField(service, "backupDir", newDir.toString());

        service.init();

        assertTrue(newDir.toFile().exists());
    }

    @Test
    void cleanOldBackups_shouldRemoveExcess() throws Exception {
        when(knowledgeService.exportAllAsJson()).thenReturn("{}");
        ReflectionTestUtils.setField(service, "retentionDays", 2);

        for (int i = 0; i < 5; i++) {
            String fn = service.createBackup();
            Path fp = tempDir.resolve(fn);
            fp.toFile().setLastModified(System.currentTimeMillis() - (long) (5 - i) * 60_000);
            Thread.sleep(1100);
        }

        service.cleanOldBackups();

        List<String> remaining = service.listBackups();
        assertTrue(remaining.size() <= 2);
    }

    @Test
    void cleanOldBackups_shouldNotDeleteWhenUnderLimit() throws Exception {
        when(knowledgeService.exportAllAsJson()).thenReturn("{}");
        ReflectionTestUtils.setField(service, "retentionDays", 10);

        for (int i = 0; i < 3; i++) {
            service.createBackup();
            Thread.sleep(1100);
        }

        service.cleanOldBackups();

        assertEquals(3, service.listBackups().size());
    }

    @Test
    void cleanOldBackups_shouldHandleEmptyDir() {
        service.cleanOldBackups();
        assertTrue(service.listBackups().isEmpty());
    }
}