package com.mindvault.operationlog;

import com.mindvault.operationlog.entity.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    List<OperationLog> findByModuleOrderByCreatedAtDesc(String module);
}