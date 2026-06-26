package com.mindvault.backup.service;

import java.util.List;

public interface BackupService {

    String createBackup();

    List<String> listBackups();

    byte[] getBackup(String filename);

    void cleanOldBackups();
}