package com.mindvault.backup;

import com.mindvault.backup.controller.BackupController;
import com.mindvault.backup.service.BackupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BackupController.class)
class BackupControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private BackupService backupService;

    @Test
    void createBackup_shouldReturnFilename() throws Exception {
        when(backupService.createBackup()).thenReturn("backup-2024-01-01.json");

        mockMvc.perform(post("/api/v1/backup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.filename").value("backup-2024-01-01.json"));
    }

    @Test
    void listBackups_shouldReturnList() throws Exception {
        when(backupService.listBackups()).thenReturn(List.of("backup-1.json", "backup-2.json"));

        mockMvc.perform(get("/api/v1/backup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("backup-1.json"))
                .andExpect(jsonPath("$.data[1]").value("backup-2.json"));
    }

    @Test
    void downloadBackup_shouldReturnBytes() throws Exception {
        byte[] data = "backup content".getBytes();
        when(backupService.getBackup("backup-1.json")).thenReturn(data);

        mockMvc.perform(get("/api/v1/backup/download/backup-1.json"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=backup-1.json"))
                .andExpect(content().bytes(data));
    }
}