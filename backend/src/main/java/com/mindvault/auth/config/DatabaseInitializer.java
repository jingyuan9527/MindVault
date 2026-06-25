package com.mindvault.auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@ConditionalOnProperty(name = "mindvault.auth.enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    private boolean isPostgres() {
        try {
            return jdbcTemplate.getDataSource().getConnection().getMetaData()
                    .getDatabaseProductName().toLowerCase().contains("postgresql");
        } catch (Exception e) {
            return false;
        }
    }

    private void createOperationLogTable() {
        if (!isPostgres()) return;

        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS operation_log (
                    id BIGSERIAL,
                    module VARCHAR(32) NOT NULL,
                    action VARCHAR(32) NOT NULL,
                    action_type VARCHAR(10) NOT NULL DEFAULT 'OTHER',
                    entity_id VARCHAR(64),
                    summary VARCHAR(500) NOT NULL DEFAULT '',
                    detail JSONB,
                    before_snapshot JSONB,
                    after_snapshot JSONB,
                    operator VARCHAR(64) NOT NULL DEFAULT 'system',
                    operator_id BIGINT,
                    ip_address VARCHAR(45),
                    result VARCHAR(10) NOT NULL DEFAULT 'SUCCESS',
                    error_message TEXT,
                    duration_ms INTEGER NOT NULL DEFAULT 0,
                    remark VARCHAR(255),
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id, created_at)
                ) PARTITION BY RANGE (created_at)
            """);
            log.info("表 operation_log (分区) 已就绪");

            createPartitionIfNotExists(0);
            createPartitionIfNotExists(1);
            createPartitionIfNotExists(2);
        } catch (Exception e) {
            log.warn("创建分区表失败，尝试创建普通表: {}", e.getMessage());
            try {
                jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS operation_log (
                        id BIGSERIAL PRIMARY KEY,
                        module VARCHAR(32) NOT NULL,
                        action VARCHAR(32) NOT NULL,
                        action_type VARCHAR(10) NOT NULL DEFAULT 'OTHER',
                        entity_id VARCHAR(64),
                        summary VARCHAR(500) NOT NULL DEFAULT '',
                        detail JSONB,
                        before_snapshot JSONB,
                        after_snapshot JSONB,
                        operator VARCHAR(64) NOT NULL DEFAULT 'system',
                        operator_id BIGINT,
                        ip_address VARCHAR(45),
                        result VARCHAR(10) NOT NULL DEFAULT 'SUCCESS',
                        error_message TEXT,
                        duration_ms INTEGER NOT NULL DEFAULT 0,
                        remark VARCHAR(255),
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                """);
                log.info("表 operation_log (普通) 已就绪");
            } catch (Exception e2) {
                log.warn("创建 operation_log 表失败: {}", e2.getMessage());
            }
        }

        migrateOldOperationLogTable();

        createIndexIfNotExists("idx_operation_log_module",
                "CREATE INDEX IF NOT EXISTS idx_operation_log_module ON operation_log (module, created_at DESC)");
        createIndexIfNotExists("idx_operation_log_operator",
                "CREATE INDEX IF NOT EXISTS idx_operation_log_operator ON operation_log (operator_id, created_at DESC)");
    }

    private void migrateOldOperationLogTable() {
        String[][] migrations = {
            {"action_type", "ALTER TABLE operation_log ADD COLUMN IF NOT EXISTS action_type VARCHAR(10) NOT NULL DEFAULT 'OTHER'"},
            {"before_snapshot", "ALTER TABLE operation_log ADD COLUMN IF NOT EXISTS before_snapshot JSONB"},
            {"after_snapshot", "ALTER TABLE operation_log ADD COLUMN IF NOT EXISTS after_snapshot JSONB"},
            {"operator_id", "ALTER TABLE operation_log ADD COLUMN IF NOT EXISTS operator_id BIGINT"},
            {"ip_address", "ALTER TABLE operation_log ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45)"},
            {"result", "ALTER TABLE operation_log ADD COLUMN IF NOT EXISTS result VARCHAR(10) NOT NULL DEFAULT 'SUCCESS'"},
            {"error_message", "ALTER TABLE operation_log ADD COLUMN IF NOT EXISTS error_message TEXT"},
            {"duration_ms", "ALTER TABLE operation_log ADD COLUMN IF NOT EXISTS duration_ms INTEGER NOT NULL DEFAULT 0"},
            {"remark", "ALTER TABLE operation_log ADD COLUMN IF NOT EXISTS remark VARCHAR(255)"},
        };
        for (String[] m : migrations) {
            try {
                jdbcTemplate.execute(m[1]);
            } catch (Exception ignored) {}
        }
    }

    private void createPartitionIfNotExists(int monthsAhead) {
        try {
            String partitionName = "operation_log_" + java.time.LocalDate.now()
                    .plusMonths(monthsAhead).format(java.time.format.DateTimeFormatter.ofPattern("yyyy_MM"));
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS " + partitionName +
                    " PARTITION OF operation_log FOR VALUES FROM ('" +
                    java.time.LocalDate.now().withDayOfMonth(1).plusMonths(monthsAhead) +
                    "') TO ('" +
                    java.time.LocalDate.now().withDayOfMonth(1).plusMonths(monthsAhead + 1) + "')");
        } catch (Exception e) {
            log.debug("创建分区 {} 跳过: {}", monthsAhead, e.getMessage());
        }
    }

    private void createIndexIfNotExists(String indexName, String ddl) {
        try {
            jdbcTemplate.queryForObject(
                    "SELECT 1 FROM pg_indexes WHERE indexname = ?", Integer.class, indexName);
        } catch (Exception e) {
            try {
                jdbcTemplate.execute(ddl);
            } catch (Exception ignored) {}
        }
    }

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id BIGSERIAL PRIMARY KEY,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    password_hash VARCHAR(255) NOT NULL,
                    display_name VARCHAR(100),
                    role VARCHAR(20) NOT NULL DEFAULT 'USER',
                    enabled BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
            """);
            log.info("表 users 已就绪");

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS api_tokens (
                    id BIGSERIAL PRIMARY KEY,
                    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    token VARCHAR(64) NOT NULL UNIQUE,
                    name VARCHAR(100) NOT NULL,
                    last_used_at TIMESTAMP,
                    expires_at TIMESTAMP,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
            """);
            log.info("表 api_tokens 已就绪");

            createOperationLogTable();
        } catch (Exception e) {
            log.error("初始化数据库表失败: {}", e.getMessage(), e);
        }
    }
}
