package com.mindvault.operationlog.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志实体
 *
 * 记录用户的关键操作，用于审计和追溯。
 * 所有 Service 层通过 OperationLogService 记录日志。
 */
@Entity
@Table(name = "operation_log")
@Data
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String module;

    @Column(nullable = false, length = 30)
    private String action;

    private Long entityId;

    @Column(length = 500)
    private String summary;

    @Column(columnDefinition = "jsonb")
    private String detail;

    @Column(length = 100)
    private String operator = "system";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}