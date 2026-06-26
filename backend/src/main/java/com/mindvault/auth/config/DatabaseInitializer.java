package com.mindvault.auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 数据库表初始化器，在应用启动时自动建表和迁移。
 *
 * <p>使用 JdbcTemplate 直接执行 DDL（而非 Flyway/Liquibase），保证轻量简洁。
 * 仅在 PostgreSQL 环境下执行完整的分区表创建逻辑，非 PG 环境（如测试 H2）
 * 跳过分区相关步骤。</p>
 *
 * <p>初始化内容：
 * <ul>
 *   <li><b>users</b> — 用户表，含用户名唯一约束和角色/状态字段</li>
 *   <li><b>api_tokens</b> — API 令牌表，外键关联 users，级联删除</li>
 *   <li><b>operation_log</b> — 操作日志分区表，按月分区(RANGE)，含列迁移逻辑</li>
 * </ul>
 * </p>
 *
 * <p>分区策略：operation_log 按 created_at 月份 RANGE 分区，预创建当前月
 * 及下两个月的分区。新列通过 ALTER TABLE ADD COLUMN IF NOT EXISTS 逐列迁移，
 * 兼容旧版 schema。分区创建失败时回退为普通表。</p>
 */
@Component
@ConditionalOnProperty(name = "mindvault.auth.enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    /**
     * 检测当前数据库是否为 PostgreSQL。
     *
     * 通过 JDBC MetaData 获取数据库产品名判断。非 PG 环境（如 H2）时跳过
     * PostgreSQL 专有的分区表创建逻辑。
     *
     * @return true 表示当前数据库为 PostgreSQL
     */
    private boolean isPostgres() {
        try {
            return jdbcTemplate.getDataSource().getConnection().getMetaData()
                    .getDatabaseProductName().toLowerCase().contains("postgresql");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 创建或迁移 operation_log 表。
     *
     * 优先创建按月 RANGE 分区表，并预创建当前月及后两个月的分区。
     * 若分区表创建失败（如 PG 版本过低），回退为普通非分区表。
     * 最后执行旧表列迁移（添加新增列）和索引创建。
     */
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

    /**
     * 迁移旧版 operation_log 表的缺失列。
     *
     * 逐列执行 ALTER TABLE ADD COLUMN IF NOT EXISTS，确保从早期版本升级
     * 时新增列不会缺失。每列迁移独立执行，单列失败不影响其他列。
     */
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

    /**
     * 创建 operation_log 的月度分区（如果不存在）。
     *
     * 分区名为 "operation_log_yyyy_MM" 格式。名称冲突或 PG 版本不支持时
     * 静默跳过，不中断初始化流程。
     *
     * @param monthsAhead 从当前月开始的偏移量（0=本月，1=下月…）
     */
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

    /**
     * 创建索引（如果不存在）。
     *
     * 先查询 pg_indexes 系统表判断索引是否已存在，不存在时才执行 DDL。
     * 仅对 PostgreSQL 生效。
     *
     * @param indexName 索引名称
     * @param ddl       CREATE INDEX DDL 语句
     */
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

    /**
     * 初始化核心数据库表。
     *
     * 依次创建 users、api_tokens 和 operation_log 三张核心表。
     * 所有 DDL 使用 IF NOT EXISTS 保证幂等性。任何表创建失败时记录
     * 错误日志但不阻止应用启动。
     */
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
