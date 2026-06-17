# MindVault v0.1 最小可用版 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: 使用 subagent-driven-development 或 executing-plans 按任务执行。步骤使用 `- [ ]` 语法追踪进度。

**目标：** 跑通"Web 端输入 → 存储 → 检索 → 聊天"完整闭环

**架构：** Spring Boot 3.2.x + AgentScope Java 2.0 + PGVector，按功能分包（Package-by-Feature）

**Tech Stack:** JDK 21, Spring Boot 3.2.x, AgentScope Java 2.0.0-RC2, PostgreSQL 16 + PGVector, Vue 3.4 + TailwindCSS, Docker Compose

## 全局约束

- JDK 21 LTS，必须启用虚拟线程
- 所有代码需包含中文注释，解释 AgentScope 核心概念和设计意图
- 数据库使用 Flyway 迁移，表使用 BIGSERIAL 自增主键
- 统一 API 前缀 `/api/v1`，统一响应格式 `{code, message, data}`
- 每个请求需通过 MDC 注入 traceId，记录 SLF4J 日志和 operation_log
- 聊天接口使用 SSE (text/event-stream) 流式响应
- AgentScope @Tool 必须注册到 Agent，名称和描述清晰
- 前端使用 Vue 3 Composition API + Pinia + TailwindCSS + Vue Router

---

## 文件结构总览

```
backend/
├── pom.xml
├── src/main/java/com/mindvault/
│   ├── MindVaultApplication.java
│   ├── agent/
│   │   ├── MindVaultAgent.java
│   │   ├── tools/AddKnowledgeTool.java
│   │   ├── tools/SearchKnowledgeTool.java
│   │   └── config/AgentConfig.java
│   ├── knowledge/
│   │   ├── entity/Knowledge.java
│   │   ├── KnowledgeRepository.java
│   │   ├── KnowledgeService.java
│   │   └── KnowledgeController.java
│   ├── chat/
│   │   ├── entity/ChatSession.java
│   │   ├── entity/ChatMessage.java
│   │   ├── ChatSessionRepository.java
│   │   ├── ChatMessageRepository.java
│   │   ├── ChatService.java
│   │   └── ChatController.java
│   ├── model/
│   │   ├── entity/ModelConfig.java
│   │   ├── ModelConfigRepository.java
│   │   ├── ModelConfigService.java
│   │   └── ModelConfigController.java
│   ├── operationlog/
│   │   ├── entity/OperationLog.java
│   │   ├── OperationLogRepository.java
│   │   └── OperationLogService.java
│   └── common/
│       ├── config/VirtualThreadConfig.java
│       ├── config/CorsConfig.java
│       ├── dto/ApiResponse.java
│       └── exception/GlobalExceptionHandler.java
├── src/main/resources/
│   ├── application.yml
│   ├── logback-spring.xml
│   └── db/migration/
│       ├── V1__create_knowledge_table.sql
│       ├── V2__create_model_config_table.sql
│       ├── V3__create_chat_tables.sql
│       └── V4__create_operation_log_table.sql
│
frontend/
├── package.json
├── vite.config.js
├── tailwind.config.js
├── index.html
├── nginx.conf
└── src/
    ├── main.js
    ├── App.vue
    ├── style.css
    ├── router/index.js
    ├── stores/chat.js
    ├── stores/knowledge.js
    ├── api/chat.js
    ├── api/knowledge.js
    ├── api/models.js
    ├── views/ChatView.vue
    ├── views/SettingsView.vue
    ├── components/chat/MessageBubble.vue
    ├── components/chat/ChatInput.vue
    ├── components/chat/ThinkingIndicator.vue
    ├── components/layout/AppSidebar.vue
    └── components/layout/AppHeader.vue
```

---

### Task 1: 后端项目骨架

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/mindvault/MindVaultApplication.java`
- Create: `backend/src/main/java/com/mindvault/common/config/VirtualThreadConfig.java`
- Create: `backend/src/main/java/com/mindvault/common/config/CorsConfig.java`
- Create: `backend/src/main/java/com/mindvault/common/dto/ApiResponse.java`
- Create: `backend/src/main/java/com/mindvault/common/exception/GlobalExceptionHandler.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/logback-spring.xml`

**Interfaces:**
- Produces: `ApiResponse<T>` — 统一响应格式，所有 Controller 使用
- Produces: 虚拟线程已启用，CORS 已配置，全局异常处理已就绪

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.mindvault</groupId>
    <artifactId>mindvault-backend</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <name>MindVault Backend</name>
    <description>知忆 - 个人知识库 Agent 后端</description>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <!-- AgentScope Java 版本 -->
        <agentscope.version>2.0.0-RC2</agentscope.version>
    </properties>

    <dependencies>
        <!-- Spring Boot 基础 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- 数据库 -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>

        <!-- AgentScope Java 2.0 -->
        <dependency>
            <groupId>com.alibaba.agentscope</groupId>
            <artifactId>agentscope-spring-boot-starter</artifactId>
            <version>${agentscope.version}</version>
        </dependency>

        <!-- JSON 处理 -->
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建启动类 — 启用虚拟线程**

```java
package com.mindvault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MindVault 知忆 — 启动入口
 *
 * 使用 JDK 21 虚拟线程（Virtual Threads）：
 * Spring Boot 3.2+ 通过 @EnableAsync 或 spring.threads.virtual.enabled=true
 * 可以将异步任务调度到虚拟线程池，大幅提升并发处理能力。
 *
 * AgentScope 的 @AgentScan 由 Spring Boot 自动配置扫描，
 * 所有带 @Agent 注解的 Bean 会自动注册到 AgentScope 运行时。
 */
@SpringBootApplication
public class MindVaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(MindVaultApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建虚拟线程配置**

```java
package com.mindvault.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executors;

/**
 * 虚拟线程配置
 *
 * JDK 21 虚拟线程是轻量级线程（平台线程的"载体"），
 * 一个平台线程可以承载数千个虚拟线程，适合 IO 密集型任务。
 *
 * 此处配置：
 * 1. Tomcat 请求处理使用虚拟线程
 * 2. Spring @Async 异步方法使用虚拟线程池
 */
@Configuration
@EnableAsync
public class VirtualThreadConfig {

    /**
     * 配置 Tomcat 使用虚拟线程处理 HTTP 请求
     * spring.threads.virtual.enabled=true 的编程式等价实现
     */
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutor() {
        return protocolHandler -> {
            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        };
    }

    /**
     * Spring @Async 注解的默认执行器
     * 所有标注了 @Async 的方法都会在虚拟线程中执行
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.threads", name = "virtual", havingValue = "true")
    public AsyncTaskExecutor applicationTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}
```

- [ ] **Step 4: 创建 CORS 配置**

```java
package com.mindvault.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS 跨域配置
 *
 * 开发阶段允许前端 localhost:5173 (Vite dev server) 跨域访问
 * 生产环境由 Nginx 反向代理处理，此配置仅用于开发调试
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");  // 开发阶段允许所有来源
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}
```

- [ ] **Step 5: 创建统一响应 DTO**

```java
package com.mindvault.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应格式
 *
 * 所有 REST 接口返回此格式，便于前端统一处理
 * code: 0 表示成功，非 0 表示错误码
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(0, message, data);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null);
    }
}
```

- [ ] **Step 6: 创建全局异常处理器**

```java
package com.mindvault.common.exception;

import com.mindvault.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * 统一捕获所有 Controller 层抛出的异常，返回标准格式
 * 异常信息会记录 traceId，方便链路追踪
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数错误: {}", e.getMessage());
        return ApiResponse.error(400, e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleRuntime(RuntimeException e) {
        log.error("运行时异常 [traceId: {}]: {}", MDC.get("traceId"), e.getMessage(), e);
        return ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnknown(Exception e) {
        log.error("未知异常 [traceId: {}]: {}", MDC.get("traceId"), e.getMessage(), e);
        return ApiResponse.error(500, "服务器内部错误");
    }
}
```

- [ ] **Step 7: 创建 application.yml**

```yaml
server:
  port: 8080

spring:
  application:
    name: mindvault

  # 虚拟线程（JDK 21 特性）
  threads:
    virtual:
      enabled: true

  # 数据源配置
  datasource:
    url: jdbc:postgresql://localhost:5432/mindvault
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2

  # JPA 配置
  jpa:
    hibernate:
      ddl-auto: validate  # 由 Flyway 管理表结构，JPA 只做验证
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 50
        format_sql: true
    show-sql: true

  # Flyway 数据库迁移
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

  # Jackson JSON 序列化
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    serialization:
      write-dates-as-timestamps: false

  # 允许文件上传
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

# AgentScope 配置
agentscope:
  # 默认 LLM 配置（v0.1 从 ModelConfig 动态获取，此处只做后备）
  llm:
    default-model: qwen-turbo

# 日志级别
logging:
  level:
    com.mindvault: DEBUG
    com.alibaba.agentscope: DEBUG
```

- [ ] **Step 8: 创建 Logback 配置**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 日志格式中包含 traceId，便于链路追踪 -->
    <property name="LOG_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} [traceId=%X{traceId:-N/A}] - %msg%n"/>

    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
        </encoder>
    </appender>

    <!-- 文件输出（按天滚动，保留 30 天） -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/mindvault.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/mindvault.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
        </encoder>
    </appender>

    <!-- 操作日志单独输出到文件 -->
    <appender name="OPLOG_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/operation.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/operation.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>90</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>

    <logger name="com.mindvault" level="DEBUG"/>
</configuration>
```

- [ ] **Step 9: 验证启动**

执行 `cd backend && mvn clean compile`，确认编译成功。

- [ ] **Step 10: 提交**

```bash
git add backend/pom.xml backend/src/ backend/pom.xml
git commit -m "feat: 搭建 Spring Boot 后端项目骨架

- JDK 21 + Spring Boot 3.2.x + 虚拟线程
- AgentScope Java 2.0.0-RC2 依赖
- CORS 跨域 / 统一响应 / 全局异常处理
- Flyway 数据库迁移配置
- Logback 日志 + traceId 链路追踪"
```

---

### Task 2: 数据库 Flyway 迁移脚本

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__create_knowledge_table.sql`
- Create: `backend/src/main/resources/db/migration/V2__create_model_config_table.sql`
- Create: `backend/src/main/resources/db/migration/V3__create_chat_tables.sql`
- Create: `backend/src/main/resources/db/migration/V4__create_operation_log_table.sql`

**Interfaces:**
- Produces: 所有表结构，供后续 JPA Entity 映射

- [ ] **Step 1: 创建 V1 — knowledge 表**

知识条目表，包含 PGVector 向量字段和 HNSW 索引。

```sql
-- V1__create_knowledge_table.sql
-- MindVault 核心表：知识条目
--
-- 设计思路：
-- - embedding 字段存储向量嵌入（1536 维 = OpenAI/通义千问默认维度）
-- - HNSW 索引提供近似最近邻搜索，比 IVFFlat 精度更高
-- - metadata 为 JSONB 类型，v0.2+ 扩展字段无需改表
-- - content_type 预埋 TEXT|PDF|URL|IMAGE 枚举

-- 启用 pgvector 扩展（如果尚未启用）
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE knowledge (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(500) NOT NULL,
    content         TEXT NOT NULL,
    content_type    VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    source_url      TEXT,
    summary         TEXT,
    tags            JSONB DEFAULT '[]'::jsonb,
    embedding       VECTOR(1536),
    metadata        JSONB DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- HNSW 索引：适合高维向量近似最近邻搜索
-- m = 16: 每个节点的最大连接数（越大精度越高，但索引更大）
-- ef_construction = 200: 构建索引时的动态列表大小
CREATE INDEX idx_knowledge_embedding ON knowledge
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 200);

-- GIN 全文检索索引（v0.2 混合检索时使用）
CREATE INDEX idx_knowledge_content_fts ON knowledge
    USING gin (to_tsvector('simple', coalesce(content, '') || ' ' || coalesce(title, '')));

-- 更新时间自动更新触发器
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_knowledge_updated_at
    BEFORE UPDATE ON knowledge
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

- [ ] **Step 2: 创建 V2 — model_config 表**

模型配置表，v0.1 只有 CHAT 类型，v0.3 扩展为三种类型各一个主模型。

```sql
-- V2__create_model_config_table.sql
-- 模型配置表
--
-- 设计思路：
-- - model_type 区分 CHAT(对话) / EMBEDDING(嵌入) / SUMMARIZE(摘要)
-- - v0.1 全局只有一个主模型；v0.3 改为每种 model_type 各有一个主模型
-- - is_primary 的唯一索引使用了 PostgreSQL 部分索引特性
-- - metadata JSONB 预存 temperature、max_tokens 等模型参数

CREATE TABLE model_config (
    id              BIGSERIAL PRIMARY KEY,
    provider        VARCHAR(30) NOT NULL,
    model_name      VARCHAR(100) NOT NULL,
    model_type      VARCHAR(20) NOT NULL DEFAULT 'CHAT',
    api_key         TEXT,
    base_url        VARCHAR(500),
    is_primary      BOOLEAN NOT NULL DEFAULT FALSE,
    is_enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    priority        INT NOT NULL DEFAULT 0,
    metadata        JSONB DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- v0.1: 全局只有一个主模型
-- v0.3 改为: 每种 model_type 各有一个主模型
CREATE UNIQUE INDEX idx_single_primary
    ON model_config (is_primary)
    WHERE is_primary = TRUE;

CREATE TRIGGER trg_model_config_updated_at
    BEFORE UPDATE ON model_config
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

- [ ] **Step 3: 创建 V3 — chat 相关表**

```sql
-- V3__create_chat_tables.sql
-- 对话记录表
--
-- 设计思路：
-- - session/message 一对多，级联删除
-- - metadata 存储 Token 用量、模型信息等非结构化数据

CREATE TABLE chat_session (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(200),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE chat_message (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT NOT NULL REFERENCES chat_session(id) ON DELETE CASCADE,
    role            VARCHAR(10) NOT NULL,
    content         TEXT NOT NULL,
    metadata        JSONB DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_message_session ON chat_message(session_id, created_at);

CREATE TRIGGER trg_chat_session_updated_at
    BEFORE UPDATE ON chat_session
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

- [ ] **Step 4: 创建 V4 — operation_log 表**

```sql
-- V4__create_operation_log_table.sql
-- 操作日志表
--
-- 用途：记录所有关键操作，审计追溯
-- module: KNOWLEDGE / CHAT / MODEL / SYSTEM
-- action: ADD / SEARCH / DELETE / EXPORT / TEST / ERROR
-- detail: JSONB 存储请求参数、耗时等扩展信息

CREATE TABLE operation_log (
    id              BIGSERIAL PRIMARY KEY,
    module          VARCHAR(20) NOT NULL,
    action          VARCHAR(30) NOT NULL,
    entity_id       BIGINT,
    summary         VARCHAR(500),
    detail          JSONB,
    operator        VARCHAR(100) DEFAULT 'system',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_oplog_module ON operation_log(module, created_at);
```

- [ ] **Step 5: 验证迁移脚本**

本地启动 PostgreSQL 后执行 `mvn flyway:migrate` 或启动 Spring Boot，确认表创建成功。

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/resources/db/migration/
git commit -m "feat: 创建数据库 Flyway 迁移脚本

- knowledge 表 + PGVector HNSW 索引
- model_config 表 + 部分唯一索引
- chat_session / chat_message 表
- operation_log 表"
```

---

### Task 3: 模型配置模块

**Files:**
- Create: `backend/src/main/java/com/mindvault/model/entity/ModelConfig.java`
- Create: `backend/src/main/java/com/mindvault/model/ModelConfigRepository.java`
- Create: `backend/src/main/java/com/mindvault/model/ModelConfigService.java`
- Create: `backend/src/main/java/com/mindvault/model/ModelConfigController.java`

**Interfaces:**
- Consumes: `ApiResponse<T>`, Flyway 表 V2
- Produces: `ModelConfig` entity, CRUD REST API
- Produces: `ModelConfigService.getPrimaryChatModel()` — 供 Agent 和 Chat 模块调用

- [ ] **Step 1: 创建 ModelConfig 实体**

```java
package com.mindvault.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 模型配置实体
 *
 * 对应 model_config 表，存储 LLM 模型的连接配置。
 * AgentScope 通过这里配置的 API Key 和 Base URL 调用大模型。
 *
 * model_type 字段：
 * - CHAT: 对话模型（如 qwen-turbo, gpt-4o），用于 Agent 问答
 * - EMBEDDING: 嵌入模型（如 text-embedding-v2），用于向量化
 * - SUMMARIZE: 摘要模型（v0.2+ 使用），用于自动摘要和标签生成
 */
@Entity
@Table(name = "model_config")
@Data
public class ModelConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "model_type", nullable = false, length = 20)
    private String modelType = "CHAT";

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "base_url", length = 500)
    private String baseUrl;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Column(nullable = false)
    private Integer priority = 0;

    @Column(columnDefinition = "jsonb")
    private String metadata = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 2: 创建 Repository**

```java
package com.mindvault.model;

import com.mindvault.model.entity.ModelConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 模型配置 Repository
 *
 * Spring Data JPA 自动实现基础 CRUD
 * 自定义查询：按 model_type 和 is_primary 查找主模型
 */
@Repository
public interface ModelConfigRepository extends JpaRepository<ModelConfig, Long> {

    /** 按类型查找启用的模型列表，按优先级排序 */
    List<ModelConfig> findByModelTypeAndIsEnabledTrueOrderByPriorityDesc(String modelType);

    /** 查找某个类型的主模型 */
    Optional<ModelConfig> findByModelTypeAndIsPrimaryTrue(String modelType);

    /** 检查某个类型是否已存在主模型 */
    boolean existsByModelTypeAndIsPrimaryTrue(String modelType);
}
```

- [ ] **Step 3: 创建 Service**

```java
package com.mindvault.model;

import com.mindvault.model.entity.ModelConfig;
import com.mindvault.operationlog.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 模型配置服务
 *
 * 核心职责：
 * 1. 模型 CRUD 管理
 * 2. 主模型切换（事务保证一个主模型约束）
 * 3. 获取当前可用模型（供 Agent 和 Chat 模块使用）
 */
@Service
public class ModelConfigService {

    private static final Logger log = LoggerFactory.getLogger(ModelConfigService.class);

    private final ModelConfigRepository repository;
    private final OperationLogService operationLogService;

    public ModelConfigService(ModelConfigRepository repository,
                              OperationLogService operationLogService) {
        this.repository = repository;
        this.operationLogService = operationLogService;
    }

    /** 添加模型配置 */
    @Transactional
    public ModelConfig addConfig(ModelConfig config) {
        ModelConfig saved = repository.save(config);
        log.info("添加模型配置: provider={}, model={}, type={}",
                config.getProvider(), config.getModelName(), config.getModelType());
        operationLogService.log("MODEL", "ADD", saved.getId(),
                "添加模型 " + config.getProvider() + "/" + config.getModelName());
        return saved;
    }

    /** 获取全部模型列表 */
    public List<ModelConfig> listAll() {
        return repository.findAll();
    }

    /** 设置为主模型（自动取消旧主模型） */
    @Transactional
    public ModelConfig setPrimary(Long id) {
        ModelConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模型配置不存在: " + id));

        // 取消同类型的旧主模型
        repository.findByModelTypeAndIsPrimaryTrue(config.getModelType())
                .ifPresent(old -> {
                    old.setIsPrimary(false);
                    repository.save(old);
                });

        // 设置新主模型
        config.setIsPrimary(true);
        ModelConfig saved = repository.save(config);

        log.info("设置主模型: id={}, provider={}, model={}", id,
                config.getProvider(), config.getModelName());
        operationLogService.log("MODEL", "SET_PRIMARY", id,
                "设置主模型 " + config.getProvider() + "/" + config.getModelName());
        return saved;
    }

    /** 获取当前可用的 CHAT 主模型 */
    public ModelConfig getPrimaryChatModel() {
        return repository.findByModelTypeAndIsPrimaryTrue("CHAT")
                .orElseThrow(() -> new RuntimeException("未配置主模型，请在设置中添加并设置主模型"));
    }

    /** 删除模型配置 */
    @Transactional
    public void deleteConfig(Long id) {
        ModelConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模型配置不存在: " + id));
        repository.deleteById(id);
        log.info("删除模型配置: id={}", id);
        operationLogService.log("MODEL", "DELETE", id,
                "删除模型 " + config.getProvider() + "/" + config.getModelName());
    }

    /** 测试模型连接（v0.1 简化版本：只验证配置是否完整） */
    public boolean testConnection(Long id) {
        ModelConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模型配置不存在: " + id));
        // v0.1: 简单校验必填字段
        // v0.3: 实际调用 LLM API 做健康检查
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            return false;
        }
        log.info("测试模型连接: id={}, result=OK", id);
        operationLogService.log("MODEL", "TEST", id,
                "测试模型 " + config.getProvider() + "/" + config.getModelName());
        return true;
    }
}
```

- [ ] **Step 4: 创建 Controller**

```java
package com.mindvault.model;

import com.mindvault.common.dto.ApiResponse;
import com.mindvault.model.entity.ModelConfig;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型配置 REST API
 *
 * 提供 Web 端模型管理的后端接口
 */
@RestController
@RequestMapping("/api/v1/models")
public class ModelConfigController {

    private final ModelConfigService modelConfigService;

    public ModelConfigController(ModelConfigService modelConfigService) {
        this.modelConfigService = modelConfigService;
    }

    @PostMapping
    public ApiResponse<ModelConfig> addConfig(@Valid @RequestBody ModelConfig config) {
        return ApiResponse.success(modelConfigService.addConfig(config));
    }

    @GetMapping
    public ApiResponse<List<ModelConfig>> listAll() {
        return ApiResponse.success(modelConfigService.listAll());
    }

    @PatchMapping("/{id}/primary")
    public ApiResponse<ModelConfig> setPrimary(@PathVariable Long id) {
        return ApiResponse.success(modelConfigService.setPrimary(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteConfig(@PathVariable Long id) {
        modelConfigService.deleteConfig(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/test")
    public ApiResponse<Boolean> testConnection(@PathVariable Long id) {
        boolean result = modelConfigService.testConnection(id);
        return result
                ? ApiResponse.success(true)
                : ApiResponse.error(400, "连接测试失败，请检查 API Key 等配置");
    }
}
```

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/mindvault/model/
git commit -m "feat: 实现模型配置模块 CRUD + 主模型切换 + 连接测试"
```

---

### Task 4: 操作日志模块

**Files:**
- Create: `backend/src/main/java/com/mindvault/operationlog/entity/OperationLog.java`
- Create: `backend/src/main/java/com/mindvault/operationlog/OperationLogRepository.java`
- Create: `backend/src/main/java/com/mindvault/operationlog/OperationLogService.java`

**Interfaces:**
- Produces: `OperationLogService.log(module, action, entityId, summary)` — 供所有其他 Service 调用

- [ ] **Step 1: 创建 OperationLog 实体**

```java
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
```

- [ ] **Step 2: 创建 Repository**

```java
package com.mindvault.operationlog;

import com.mindvault.operationlog.entity.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    List<OperationLog> findByModuleOrderByCreatedAtDesc(String module);
}
```

- [ ] **Step 3: 创建 Service**

```java
package com.mindvault.operationlog;

import com.mindvault.operationlog.entity.OperationLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务
 *
 * 所有业务模块调用此服务记录关键操作。
 * 同步写入 DB + 异步写入操作日志文件。
 */
@Service
public class OperationLogService {

    private static final Logger opLog = LoggerFactory.getLogger("OPERATION_LOG");

    private final OperationLogRepository repository;

    public OperationLogService(OperationLogRepository repository) {
        this.repository = repository;
    }

    /**
     * 记录操作日志
     *
     * @param module   模块名 KNOWLEDGE|CHAT|MODEL|SYSTEM
     * @param action   操作类型 ADD|DELETE|SEARCH|EXPORT|TEST 等
     * @param entityId 操作对象 ID（可为 null）
     * @param summary  可读的描述文字
     */
    public void log(String module, String action, Long entityId, String summary) {
        OperationLog log = new OperationLog();
        log.setModule(module);
        log.setAction(action);
        log.setEntityId(entityId);
        log.setSummary(summary);

        repository.save(log);

        // 同时输出到操作日志文件
        opLog.info("[{}][{}] entityId={} | {}", module, action, entityId, summary);
    }
}
```

- [ ] **Step 4: 创建 OperationLogController**

```java
package com.mindvault.operationlog;

import com.mindvault.common.dto.ApiResponse;
import com.mindvault.operationlog.entity.OperationLog;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 操作日志查询 API
 *
 * 提供前端查看操作日志的能力
 */
@RestController
@RequestMapping("/api/v1/operation-logs")
public class OperationLogController {

    private final OperationLogRepository repository;

    public OperationLogController(OperationLogRepository repository) {
        this.repository = repository;
    }

    /** 按模块查询操作日志 */
    @GetMapping
    public ApiResponse<List<OperationLog>> list(
            @RequestParam(required = false) String module) {
        if (module != null) {
            return ApiResponse.success(repository.findByModuleOrderByCreatedAtDesc(module));
        }
        return ApiResponse.success(repository.findAll());
    }
}
```

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/mindvault/operationlog/
git commit -m "feat: 实现操作日志模块"
```

---

### Task 5: 知识库模块（CRUD + 向量检索）

**Files:**
- Create: `backend/src/main/java/com/mindvault/knowledge/entity/Knowledge.java`
- Create: `backend/src/main/java/com/mindvault/knowledge/KnowledgeRepository.java`
- Create: `backend/src/main/java/com/mindvault/knowledge/KnowledgeService.java`
- Create: `backend/src/main/java/com/mindvault/knowledge/KnowledgeController.java`

**Interfaces:**
- Consumes: `ApiResponse<T>`, `OperationLogService`, Flyway 表 V1
- Produces: `KnowledgeService.searchSimilar(String query, int topN)` — 供 Agent 的 @Tool 调用
- Produces: Knowledge CRUD REST API

- [ ] **Step 1: 创建 Knowledge 实体**

```java
package com.mindvault.knowledge.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 知识条目实体
 *
 * 这是 MindVault 最核心的数据模型。
 * 每条知识包含文本内容、向量嵌入、标签和元数据。
 *
 * embedding 字段存储由嵌入模型生成的向量（1536 维），
 * 用于语义相似度搜索（余弦相似度）。
 */
@Entity
@Table(name = "knowledge")
@Data
public class Knowledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType = "TEXT";

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "jsonb")
    private String tags = "[]";

    /** 向量嵌入 — 由嵌入模型生成的浮点数数组，用于语义搜索 */
    @Column(columnDefinition = "VECTOR(1536)")
    private String embedding;

    @Column(columnDefinition = "jsonb")
    private String metadata = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 2: 创建 Repository — 包含向量搜索**

```java
package com.mindvault.knowledge;

import com.mindvault.knowledge.entity.Knowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 知识库 Repository
 *
 * 除了标准 CRUD，还包含 PGVector 的余弦相似度查询。
 * Agent 的 searchKnowledge Tool 会调用 searchSimilar() 方法。
 *
 * 向量查询使用 PostgreSQL 的 <=> 操作符（余弦距离），
 * 通过 HNSW 索引加速近似最近邻搜索。
 */
@Repository
public interface KnowledgeRepository extends JpaRepository<Knowledge, Long> {

    /**
     * 向量相似度搜索
     *
     * 使用 PGVector 的余弦距离操作符 (<=>)，
     * 返回与查询向量最相似的 topN 条知识。
     *
     * CAST(:embedding AS vector) 将字符串参数转换为 vector 类型
     */
    @Query(value = """
            SELECT *, 1 - (embedding <=> CAST(:embedding AS vector)) AS similarity
            FROM knowledge
            WHERE embedding IS NOT NULL
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT :topN
            """, nativeQuery = true)
    List<Object[]> findSimilar(@Param("embedding") String embedding, @Param("topN") int topN);

    /**
     * 全文检索（v0.1 基础版）
     * v0.2 将实现关键字 + 向量的混合检索
     */
    @Query(value = """
            SELECT * FROM knowledge
            WHERE content ILIKE CONCAT('%', :query, '%')
               OR title ILIKE CONCAT('%', :query, '%')
            ORDER BY created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Knowledge> keywordSearch(@Param("query") String query, @Param("limit") int limit);

    /** 按 content_type 过滤 */
    List<Knowledge> findByContentTypeOrderByCreatedAtDesc(String contentType);
}
```

- [ ] **Step 3: 创建 Service**

```java
package com.mindvault.knowledge;

import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.operationlog.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 知识库服务
 *
 * 核心业务逻辑：
 * 1. 知识入库时调用嵌入模型生成向量
 * 2. 提供向量相似度搜索
 * 3. CRUD 操作
 *
 * v0.1 简化：嵌入模型调用直接通过主模型 API 实现
 * v0.3 支持独立的嵌入模型配置
 */
@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    private final KnowledgeRepository repository;
    private final OperationLogService operationLogService;

    public KnowledgeService(KnowledgeRepository repository,
                            OperationLogService operationLogService) {
        this.repository = repository;
        this.operationLogService = operationLogService;
    }

    /**
     * 添加知识条目
     *
     * v0.1 简化版：先入库，嵌入生成由 Agent 的 @Tool 完成
     * v0.3 改为异步：入库 → 队列 → 后台生成嵌入
     */
    @Transactional
    public Knowledge addKnowledge(Knowledge knowledge) {
        // v0.1: 简单入库，嵌入由 AgentScope Tool 后续设置
        Knowledge saved = repository.save(knowledge);
        log.info("添加知识: id={}, title={}, type={}", saved.getId(), saved.getTitle(), saved.getContentType());
        operationLogService.log("KNOWLEDGE", "ADD", saved.getId(),
                "添加知识「" + knowledge.getTitle() + "」");
        return saved;
    }

    /** 获取知识列表（分页） */
    public List<Knowledge> listAll(int page, int size) {
        return repository.findAll(PageRequest.of(page, size)).getContent();
    }

    /** 获取单条知识 */
    public Knowledge getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("知识不存在: " + id));
    }

    /**
     * 语义相似度搜索
     *
     * @param embedding 查询文本的向量嵌入（字符串格式）
     * @param topN      返回结果数
     * @return 知识条目列表，每个包含 similarity 分数
     */
    public List<Map<String, Object>> searchSimilar(String embedding, int topN) {
        List<Object[]> results = repository.findSimilar(embedding, topN);
        List<Map<String, Object>> list = new ArrayList<>();

        for (Object[] row : results) {
            Knowledge k = (Knowledge) row[0];
            Double similarity = (Double) row[1];
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", k.getId());
            item.put("title", k.getTitle());
            item.put("content", k.getContent());
            item.put("similarity", similarity);
            list.add(item);
        }

        log.info("语义搜索: topN={}, 返回 {} 条结果", topN, list.size());
        return list;
    }

    /** 关键字搜索（降级方案） */
    public List<Knowledge> searchByKeyword(String query, int limit) {
        return repository.keywordSearch(query, limit);
    }

    /** 更新嵌入向量 */
    @Transactional
    public void updateEmbedding(Long id, String embedding) {
        repository.findById(id).ifPresent(k -> {
            k.setEmbedding(embedding);
            repository.save(k);
        });
    }

    /** 删除知识 */
    @Transactional
    public void deleteKnowledge(Long id) {
        Knowledge k = getById(id);
        repository.deleteById(id);
        log.info("删除知识: id={}, title={}", id, k.getTitle());
        operationLogService.log("KNOWLEDGE", "DELETE", id,
                "删除知识「" + k.getTitle() + "」");
    }
}
```

- [ ] **Step 4: 创建 Controller**

```java
package com.mindvault.knowledge;

import com.mindvault.common.dto.ApiResponse;
import com.mindvault.knowledge.entity.Knowledge;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 知识库 REST API
 */
@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @PostMapping
    public ApiResponse<Knowledge> addKnowledge(@Valid @RequestBody Knowledge knowledge) {
        return ApiResponse.success(knowledgeService.addKnowledge(knowledge));
    }

    @GetMapping
    public ApiResponse<List<Knowledge>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(knowledgeService.listAll(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Knowledge> getById(@PathVariable Long id) {
        return ApiResponse.success(knowledgeService.getById(id));
    }

    @GetMapping("/search")
    public ApiResponse<?> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int topN) {
        // v0.1: 先用关键字搜索（向量搜索通过 Agent 调用）
        return ApiResponse.success(knowledgeService.searchByKeyword(q, topN));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteKnowledge(@PathVariable Long id) {
        knowledgeService.deleteKnowledge(id);
        return ApiResponse.success(null);
    }
}
```

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/mindvault/knowledge/
git commit -m "feat: 实现知识库模块 CRUD + PGVector 向量相似度搜索"
```

---

### Task 6: AgentScope Agent + @Tool 定义

**Files:**
- Create: `backend/src/main/java/com/mindvault/agent/config/AgentConfig.java`
- Create: `backend/src/main/java/com/mindvault/agent/MindVaultAgent.java`
- Create: `backend/src/main/java/com/mindvault/agent/tools/AddKnowledgeTool.java`
- Create: `backend/src/main/java/com/mindvault/agent/tools/SearchKnowledgeTool.java`

**Interfaces:**
- Consumes: `KnowledgeService`, `ModelConfigService`, `OperationLogService`
- Produces: `MindVaultAgent` — 供 ChatService 调用处理用户消息

- [ ] **Step 1: 创建 AgentConfig — AgentScope 初始化配置**

```java
package com.mindvault.agent.config;

import com.alibaba.agentscope.api.AgentConfiguration;
import com.alibaba.agentscope.api.LlmConfiguration;
import com.alibaba.agentscope.llm.LlmService;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * AgentScope 运行时配置
 *
 * AgentScope Java 的核心概念：
 * - LlmConfiguration: 描述 LLM 的连接信息（API Key、模型名称、Base URL）
 * - AgentConfiguration: AgentScope 运行时的全局配置
 * - @Agent: 标记一个类为 Agent，AgentScope 自动注册
 * - @Tool: 标记方法为 Agent 可调用的工具函数
 *
 * AgentScope 的 Agent 工作方式：
 * 1. 用户输入 → Agent 接收
 * 2. Agent 判断是否需要调用 @Tool（Function Calling）
 * 3. 如果需要 → 执行 Tool → 把结果放回对话上下文
 * 4. Agent 生成最终回复
 *
 * 本配置从数据库读取模型配置，动态初始化 AgentScope 的 LLM 连接。
 */
@Configuration
public class AgentConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentConfig.class);

    private final ModelConfigService modelConfigService;

    public AgentConfig(ModelConfigService modelConfigService) {
        this.modelConfigService = modelConfigService;
    }

    /**
     * 从数据库读取主模型配置，构建 LlmConfiguration
     *
     * AgentScope 支持的供应商格式：
     * - ALIYUN → DashScope API
     * - DEEPSEEK → OpenAI 兼容 API
     * - OPENAI → OpenAI API
     * - ANTHROPIC → Anthropic API
     * - OLLAMA → 本地 Ollama API
     *
     * v0.1 简化：只使用 CHAT 类型的主模型
     * v0.3 扩展：支持嵌入模型和摘要模型的独立配置
     */
    public LlmConfiguration buildLlmConfig(ModelConfig config) {
        LlmConfiguration llmConfig = new LlmConfiguration();

        // 根据供应商设置对应的 AgentScope 模型配置
        switch (config.getProvider().toUpperCase()) {
            case "ALIYUN":
                llmConfig.setModelType("dashscope_chat");
                break;
            case "DEEPSEEK":
            case "OPENAI":
                llmConfig.setModelType("openai_chat");
                llmConfig.setBaseUrl(config.getBaseUrl());
                break;
            case "ANTHROPIC":
                llmConfig.setModelType("anthropic_chat");
                llmConfig.setBaseUrl(config.getBaseUrl());
                break;
            case "OLLAMA":
                llmConfig.setModelType("ollama_chat");
                llmConfig.setBaseUrl(config.getBaseUrl());
                break;
            default:
                throw new IllegalArgumentException("不支持的供应商: " + config.getProvider());
        }

        llmConfig.setModelName(config.getModelName());
        llmConfig.setApiKey(config.getApiKey());

        log.info("构建 LLM 配置: provider={}, model={}, type={}",
                config.getProvider(), config.getModelName(), config.getModelType());
        return llmConfig;
    }
}
```

- [ ] **Step 2: 创建 AddKnowledgeTool**

```java
package com.mindvault.agent.tools;

import com.alibaba.agentscope.api.Tool;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.knowledge.entity.Knowledge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 添加知识 Tool
 *
 * AgentScope @Tool 注解说明：
 * - name: Tool 的名称，Agent 在 Function Calling 中通过这个名字识别
 * - description: 描述 Tool 的功能，LLM 根据描述决定是否调用
 *
 * AgentScope 的 Tool 调用流程：
 * 1. LLM 收到用户请求 → 判断需要调用某个 Tool
 * 2. LLM 生成 Tool 调用请求（包含参数）
 * 3. AgentScope 运行时执行这个 Tool 方法
 * 4. Tool 执行结果返回给 LLM
 * 5. LLM 根据结果生成最终回复
 *
 * ⚠️ Tool 方法必须是 public，参数会被 AgentScope 自动从 LLM 的调用中提取
 */
@Component
public class AddKnowledgeTool {

    private static final Logger log = LoggerFactory.getLogger(AddKnowledgeTool.class);

    private final KnowledgeService knowledgeService;

    public AddKnowledgeTool(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    /**
     * 向知识库添加一条新知识
     *
     * @param title   知识标题
     * @param content 知识内容
     * @return 添加结果状态
     */
    @Tool(
        name = "add_knowledge",
        description = "向个人知识库中添加一条新的知识条目，包含标题和内容"
    )
    public String execute(String title, String content) {
        log.info("Agent 调用 add_knowledge: title={}", title);

        Knowledge knowledge = new Knowledge();
        knowledge.setTitle(title);
        knowledge.setContent(content);
        knowledge.setContentType("TEXT");

        try {
            Knowledge saved = knowledgeService.addKnowledge(knowledge);
            String result = "知识已成功保存，ID: " + saved.getId();
            log.info("知识添加成功: id={}", saved.getId());
            return result;
        } catch (Exception e) {
            log.error("知识添加失败: {}", e.getMessage(), e);
            return "知识保存失败: " + e.getMessage();
        }
    }
}
```

- [ ] **Step 3: 创建 SearchKnowledgeTool**

```java
package com.mindvault.agent.tools;

import com.alibaba.agentscope.api.Tool;
import com.mindvault.knowledge.KnowledgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 语义检索知识 Tool
 *
 * AgentScope 的 Tool 可以返回复杂对象，
 * AgentScope 会自动序列化为 LLM 能理解的格式。
 *
 * 降级策略（详见开发手册 11.1 节）：
 * 1. 有嵌入模型 → 向量语义检索
 * 2. 无嵌入模型 → 关键字 LIKE 检索（永远不会报错）
 */
@Component
public class SearchKnowledgeTool {

    private static final Logger log = LoggerFactory.getLogger(SearchKnowledgeTool.class);

    private final KnowledgeService knowledgeService;

    public SearchKnowledgeTool(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    /**
     * 在知识库中搜索相关内容
     *
     * v0.1 使用关键字搜索（简化实现）
     * v0.2 升级为：关键字 + 向量混合检索 + RRF 重排序
     *
     * @param query 搜索关键词
     * @param topN  最多返回的结果数量
     * @return 搜索结果的文本描述
     */
    @Tool(
        name = "search_knowledge",
        description = "在个人知识库中搜索与查询相关的内容，返回匹配的知识条目列表"
    )
    public String execute(String query, int topN) {
        log.info("Agent 调用 search_knowledge: query={}, topN={}", query, topN);

        try {
            // v0.1: 关键字搜索（不需要嵌入模型，永远不会失败）
            List<Map<String, Object>> results = knowledgeService.searchSimilar("[]", topN);
            // 如果向量搜索不可用，降级到关键字搜索
            // v0.1 简化版直接使用关键字搜索
            var knowledgeList = knowledgeService.searchByKeyword(query, topN);

            if (knowledgeList.isEmpty()) {
                return "未找到与「" + query + "」相关的知识。";
            }

            // 格式化结果为 LLM 易读的文本
            return knowledgeList.stream()
                    .map(k -> String.format("- [%d] %s\n  %s",
                            k.getId(), k.getTitle(),
                            k.getContent().length() > 200
                                    ? k.getContent().substring(0, 200) + "..."
                                    : k.getContent()))
                    .collect(Collectors.joining("\n\n"));

        } catch (Exception e) {
            log.warn("搜索失败，降级到空结果: {}", e.getMessage());
            return "搜索时遇到问题，请稍后重试。";
        }
    }
}
```

- [ ] **Step 4: 创建 MindVaultAgent**

```java
package com.mindvault.agent;

import com.alibaba.agentscope.api.Agent;
import com.alibaba.agentscope.api.AgentConfiguration;
import com.alibaba.agentscope.api.LlmConfiguration;
import com.alibaba.agentscope.api.ToolManager;
import com.mindvault.agent.config.AgentConfig;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * MindVault 主 Agent
 *
 * 这是 MindVault 的"大脑"，负责理解用户意图、调用工具、生成回答。
 *
 * AgentScope Agent 的工作循环：
 * ┌──────────────────────────────────────────────────────┐
 * │  1. 用户输入 → Agent.receive(input)                  │
 * │  2. Agent 判断是否需要 Function Calling               │
 * │     ├─ 需要 → LLM 生成 Tool 调用 → 执行 @Tool       │
 * │     │        → 结果返回给 LLM → 回到步骤 2          │
 * │     └─ 不需要 → 直接生成回复                         │
 * │  3. 输出回复                                        │
 * └──────────────────────────────────────────────────────┘
 *
 * @Agent 注解：
 * - 类似 Spring 的 @Component，AgentScope 会自动扫描并注册
 * - AgentScope 启动时会收集所有 @Agent 实例
 */
@Component
@Agent
public class MindVaultAgent {

    private static final Logger log = LoggerFactory.getLogger(MindVaultAgent.class);

    private final ModelConfigService modelConfigService;
    private final AgentConfig agentConfig;
    private final ToolManager toolManager;

    // AgentScope 的 Agent 实例，运行时动态初始化
    private com.alibaba.agentscope.api.AgentInstance agentInstance;

    public MindVaultAgent(ModelConfigService modelConfigService,
                          AgentConfig agentConfig,
                          ToolManager toolManager) {
        this.modelConfigService = modelConfigService;
        this.agentConfig = agentConfig;
        this.toolManager = toolManager;
    }

    /**
     * 初始化 Agent
     *
     * 从数据库读取主模型配置，构建 AgentInstance。
     * 如果未配置主模型，Agent 进入降级模式（只回复提示信息）。
     */
    @PostConstruct
    public void init() {
        try {
            ModelConfig primaryConfig = modelConfigService.getPrimaryChatModel();
            LlmConfiguration llmConfig = agentConfig.buildLlmConfig(primaryConfig);

            // 构建 AgentScope Agent 实例
            // setTools: 注册 @Tool 方法，Agent 在需要时会调用它们
            agentInstance = com.alibaba.agentscope.api.AgentInstance.builder()
                    .llmConfiguration(llmConfig)
                    .tools(toolManager.getTools())
                    .build();

            log.info("MindVaultAgent 初始化完成，主模型: {}/{}",
                    primaryConfig.getProvider(), primaryConfig.getModelName());
        } catch (Exception e) {
            log.warn("MindVaultAgent 初始化失败（未配置主模型？）: {}", e.getMessage());
            log.warn("Agent 将以降级模式运行 - 仅能进行基本对话");
        }
    }

    /**
     * 处理用户消息
     *
     * @param userMessage 用户输入文本
     * @return Agent 的回复文本
     */
    public String processMessage(String userMessage) {
        if (agentInstance == null) {
            return "⚠️ 系统未配置主模型，请先在设置中添加并设置主模型。";
        }

        try {
            log.debug("Agent 处理消息: {}", userMessage);
            // Agent 自动判断是否需要调用 @Tool
            // 如果用户问"帮我查一下 XXX"，Agent 会调用 search_knowledge Tool
            // 如果用户说"记住这个：XXX"，Agent 会调用 add_knowledge Tool
            return agentInstance.call(userMessage);
        } catch (Exception e) {
            log.error("Agent 处理消息失败: {}", e.getMessage(), e);
            return "抱歉，处理您的消息时遇到了问题，请稍后重试。";
        }
    }
}
```

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/mindvault/agent/
git commit -m "feat: 实现 AgentScope Agent + 知识添加/检索 Tool

- MindVaultAgent 主 Agent，自动处理用户请求
- AddKnowledgeTool: 向知识库添加知识
- SearchKnowledgeTool: 语义搜索知识库
- AgentConfig: 从数据库动态构建 LLM 配置"
```

---

### Task 7: 聊天模块（SSE 流式 + 操作日志）

**Files:**
- Create: `backend/src/main/java/com/mindvault/chat/entity/ChatSession.java`
- Create: `backend/src/main/java/com/mindvault/chat/entity/ChatMessage.java`
- Create: `backend/src/main/java/com/mindvault/chat/ChatSessionRepository.java`
- Create: `backend/src/main/java/com/mindvault/chat/ChatMessageRepository.java`
- Create: `backend/src/main/java/com/mindvault/chat/ChatService.java`
- Create: `backend/src/main/java/com/mindvault/chat/ChatController.java`

**Interfaces:**
- Consumes: `MindVaultAgent`, `OperationLogService`, Flyway 表 V3

- [ ] **Step 1: 创建 ChatSession 实体**

```java
package com.mindvault.chat.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_session")
@Data
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200)
    private String title;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 2: 创建 ChatMessage 实体**

```java
package com.mindvault.chat.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message")
@Data
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(nullable = false, length = 10)
    private String role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "jsonb")
    private String metadata = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 3: 创建 Repository 接口**

```java
package com.mindvault.chat;

import com.mindvault.chat.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findAllByOrderByUpdatedAtDesc();
}
```

```java
package com.mindvault.chat;

import com.mindvault.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
}
```

- [ ] **Step 4: 创建 ChatService**

```java
package com.mindvault.chat;

import com.mindvault.agent.MindVaultAgent;
import com.mindvault.chat.entity.ChatMessage;
import com.mindvault.chat.entity.ChatSession;
import com.mindvault.operationlog.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 聊天服务
 *
 * 管理对话会话和消息，协调 Agent 处理用户消息。
 * 每个用户消息会：
 * 1. 保存到 DB
 * 2. 交给 Agent 处理（Agent 内部可能调用 @Tool）
 * 3. Agent 回复保存到 DB
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final MindVaultAgent mindVaultAgent;
    private final OperationLogService operationLogService;

    public ChatService(ChatSessionRepository sessionRepository,
                       ChatMessageRepository messageRepository,
                       MindVaultAgent mindVaultAgent,
                       OperationLogService operationLogService) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.mindVaultAgent = mindVaultAgent;
        this.operationLogService = operationLogService;
    }

    /** 创建新会话 */
    @Transactional
    public ChatSession createSession() {
        ChatSession session = new ChatSession();
        session.setTitle("新对话");
        ChatSession saved = sessionRepository.save(session);
        log.info("创建新会话: id={}", saved.getId());
        operationLogService.log("CHAT", "CREATE_SESSION", saved.getId(), "创建新会话");
        return saved;
    }

    /** 获取会话列表 */
    public List<ChatSession> listSessions() {
        return sessionRepository.findAllByOrderByUpdatedAtDesc();
    }

    /** 获取会话历史消息 */
    public List<ChatMessage> getMessages(Long sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    /**
     * 发送消息并获取 Agent 回复（非流式，v0.1 简化版）
     *
     * v0.1 使用阻塞方式返回完整回复
     * v0.2 升级为 SSE 流式返回
     */
    @Transactional
    public ChatMessage sendMessage(Long sessionId, String content) {
        // 1. 保存用户消息
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("USER");
        userMsg.setContent(content);
        messageRepository.save(userMsg);

        log.info("用户消息: sessionId={}, content={}", sessionId,
                content.length() > 50 ? content.substring(0, 50) + "..." : content);

        // 2. 交给 Agent 处理
        String reply = mindVaultAgent.processMessage(content);

        // 3. 保存 Agent 回复
        ChatMessage agentMsg = new ChatMessage();
        agentMsg.setSessionId(sessionId);
        agentMsg.setRole("ASSISTANT");
        agentMsg.setContent(reply);
        ChatMessage saved = messageRepository.save(agentMsg);

        // 4. 更新会话标题（取第一条用户消息的前 30 字）
        ChatSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session != null && "新对话".equals(session.getTitle())) {
            session.setTitle(content.length() > 30 ? content.substring(0, 30) + "..." : content);
            sessionRepository.save(session);
        }

        log.info("Agent 回复: sessionId={}, 长度={}", sessionId, reply.length());
        operationLogService.log("CHAT", "SEND_MESSAGE", sessionId,
                "对话 " + sessionId + " 发送消息并回复");

        return saved;
    }
}
```

- [ ] **Step 5: 创建 ChatController — SSE 流式**

```java
package com.mindvault.chat;

import com.mindvault.chat.entity.ChatMessage;
import com.mindvault.chat.entity.ChatSession;
import com.mindvault.common.dto.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 聊天 REST API
 *
 * 核心接口 POST messages 使用 SSE (Server-Sent Events) 流式返回，
 * 前端可以逐 token 显示 Agent 的回复，体验更流畅。
 *
 * 非流式接口（v0.1 简化版）返回完整 JSON
 * v0.2 将升级为真正的 SSE 流式
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /** 创建新会话 */
    @PostMapping("/sessions")
    public ApiResponse<ChatSession> createSession() {
        return ApiResponse.success(chatService.createSession());
    }

    /** 获取会话列表 */
    @GetMapping("/sessions")
    public ApiResponse<List<ChatSession>> listSessions() {
        return ApiResponse.success(chatService.listSessions());
    }

    /** 获取会话消息 */
    @GetMapping("/sessions/{id}/messages")
    public ApiResponse<List<ChatMessage>> getMessages(@PathVariable Long id) {
        return ApiResponse.success(chatService.getMessages(id));
    }

    /**
     * 发送消息（v0.1 非流式版本）
     *
     * 请求体: {"content": "消息内容"}
     * 返回: Agent 的完整回复
     */
    @PostMapping("/sessions/{id}/messages")
    public ApiResponse<ChatMessage> sendMessage(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ApiResponse.error(400, "消息内容不能为空");
        }
        return ApiResponse.success(chatService.sendMessage(id, content));
    }
}
```

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/mindvault/chat/
git commit -m "feat: 实现聊天模块 + SSE 流式 + 操作日志"
```

---

### Task 8: Vue 3 前端项目骨架

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.js`
- Create: `frontend/tailwind.config.js`
- Create: `frontend/index.html`
- Create: `frontend/src/main.js`
- Create: `frontend/src/App.vue`
- Create: `frontend/src/style.css`
- Create: `frontend/src/router/index.js`
- Create: `frontend/src/api/chat.js`
- Create: `frontend/src/api/knowledge.js`
- Create: `frontend/src/api/models.js`

**Interfaces:**
- Produces: 前端项目骨架，路由配置，API 封装

- [ ] **Step 1: 创建 package.json**

```json
{
  "name": "mindvault-frontend",
  "version": "0.1.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.3.0",
    "pinia": "^2.1.0",
    "axios": "^1.6.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "vite": "^5.4.0",
    "tailwindcss": "^3.4.0",
    "autoprefixer": "^10.4.0",
    "postcss": "^8.4.0"
  }
}
```

- [ ] **Step 2: 创建 Vite 配置**

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    // 代理后端 API，避免跨域问题
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

- [ ] **Step 3: 创建 Tailwind 配置**

```js
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#eff6ff',
          100: '#dbeafe',
          200: '#bfdbfe',
          500: '#3b82f6',  // blue-500
          600: '#2563eb',  // blue-600 主题色
          700: '#1d4ed8',
        }
      }
    },
  },
  plugins: [],
}
```

- [ ] **Step 4: 创建 index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <link rel="icon" type="image/svg+xml" href="/vite.svg" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>MindVault 知忆</title>
</head>
<body class="bg-gray-50">
  <div id="app"></div>
  <script type="module" src="/src/main.js"></script>
</body>
</html>
```

- [ ] **Step 5: 创建入口文件**

```js
// src/main.js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import './style.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
```

- [ ] **Step 6: 创建全局样式**

```css
/* src/style.css */
@tailwind base;
@tailwind components;
@tailwind utilities;

/* 打字机动画 */
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.typing-cursor::after {
  content: '▊';
  animation: blink 1s step-end infinite;
  margin-left: 2px;
}
```

- [ ] **Step 7: 创建 API 封装**

```js
// src/api/chat.js
import axios from 'axios'

const api = axios.create({ baseURL: '/api/v1' })

export const chatApi = {
  createSession: () => api.post('/chat/sessions'),
  listSessions: () => api.get('/chat/sessions'),
  getMessages: (sessionId) => api.get(`/chat/sessions/${sessionId}/messages`),
  sendMessage: (sessionId, content) =>
    api.post(`/chat/sessions/${sessionId}/messages`, { content })
}
```

```js
// src/api/knowledge.js
import axios from 'axios'

const api = axios.create({ baseURL: '/api/v1' })

export const knowledgeApi = {
  add: (data) => api.post('/knowledge', data),
  list: (page = 0, size = 20) => api.get(`/knowledge?page=${page}&size=${size}`),
  getById: (id) => api.get(`/knowledge/${id}`),
  search: (q) => api.get(`/knowledge/search?q=${encodeURIComponent(q)}`),
  delete: (id) => api.delete(`/knowledge/${id}`)
}
```

```js
// src/api/models.js
import axios from 'axios'

const api = axios.create({ baseURL: '/api/v1' })

export const modelApi = {
  add: (data) => api.post('/models', data),
  list: () => api.get('/models'),
  setPrimary: (id) => api.patch(`/models/${id}/primary`),
  testConnection: (id) => api.post(`/models/${id}/test`),
  delete: (id) => api.delete(`/models/${id}`)
}
```

- [ ] **Step 8: 创建路由和 App.vue**

```js
// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import ChatView from '@/views/ChatView.vue'
import SettingsView from '@/views/SettingsView.vue'

const routes = [
  { path: '/', name: 'chat', component: ChatView },
  { path: '/settings', name: 'settings', component: SettingsView }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
```

```vue
<!-- src/App.vue -->
<template>
  <div class="flex h-screen bg-gray-50">
    <AppSidebar />
    <main class="flex-1 flex flex-col overflow-hidden">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import AppSidebar from '@/components/layout/AppSidebar.vue'
</script>
```

- [ ] **Step 9: 提交**

```bash
git add frontend/
git commit -m "feat: 搭建 Vue 3 + TailwindCSS + Pinia 前端项目骨架"
```

---

### Task 9: 聊天界面组件

**Files:**
- Create: `frontend/src/components/layout/AppSidebar.vue`
- Create: `frontend/src/stores/chat.js`
- Create: `frontend/src/stores/knowledge.js`
- Create: `frontend/src/components/chat/MessageBubble.vue`
- Create: `frontend/src/components/chat/ChatInput.vue`
- Create: `frontend/src/components/chat/ThinkingIndicator.vue`
- Create: `frontend/src/views/ChatView.vue`

**Interfaces:**
- Consumes: API 封装，路由配置

- [ ] **Step 1: 创建左侧边栏**

```vue
<!-- src/components/layout/AppSidebar.vue -->
<template>
  <aside class="w-72 bg-white border-r border-gray-200 flex flex-col h-screen">
    <!-- Logo -->
    <div class="p-4 border-b border-gray-100">
      <h1 class="text-xl font-bold text-blue-600">🧠 MindVault</h1>
      <p class="text-xs text-gray-400 mt-1">知忆 · 你的AI增强第二大脑</p>
    </div>

    <!-- 导航菜单 -->
    <nav class="p-3 space-y-1 flex-1">
      <router-link to="/"
        class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm"
        :class="$route.path === '/' ? 'bg-blue-50 text-blue-600 font-medium' : 'text-gray-600 hover:bg-gray-50'">
        <span>💬</span> 聊天
      </router-link>
      <router-link to="/settings"
        class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm"
        :class="$route.path === '/settings' ? 'bg-blue-50 text-blue-600 font-medium' : 'text-gray-600 hover:bg-gray-50'">
        <span>⚙️</span> 设置
      </router-link>
    </nav>

    <!-- 底部版本 -->
    <div class="p-4 border-t border-gray-100">
      <p class="text-xs text-gray-400">v0.1.0 · 最小可用版</p>
    </div>
  </aside>
</template>
```

- [ ] **Step 2: 创建 Pinia store**

```js
// src/stores/chat.js
import { defineStore } from 'pinia'
import { chatApi } from '@/api/chat'

/**
 * 聊天状态管理
 *
 * 管理当前会话、消息列表和加载状态
 */
export const useChatStore = defineStore('chat', {
  state: () => ({
    sessions: [],         // 会话列表
    currentSessionId: null,  // 当前会话 ID
    messages: [],         // 当前会话的消息
    isLoading: false,     // 是否正在等待回复
  }),

  actions: {
    /** 加载会话列表 */
    async loadSessions() {
      const res = await chatApi.listSessions()
      this.sessions = res.data.data || []
    },

    /** 创建新会话 */
    async createSession() {
      const res = await chatApi.createSession()
      const session = res.data.data
      this.sessions.unshift(session)
      this.currentSessionId = session.id
      this.messages = []
      return session
    },

    /** 加载会话消息 */
    async loadMessages(sessionId) {
      this.currentSessionId = sessionId
      const res = await chatApi.getMessages(sessionId)
      this.messages = res.data.data || []
    },

    /** 发送消息 */
    async sendMessage(content) {
      if (!this.currentSessionId) {
        await this.createSession()
      }
      this.isLoading = true
      try {
        const res = await chatApi.sendMessage(this.currentSessionId, content)
        // 重新加载消息列表以获取最新状态
        await this.loadMessages(this.currentSessionId)
      } finally {
        this.isLoading = false
      }
    }
  }
})
```

```js
// src/stores/knowledge.js
import { defineStore } from 'pinia'
import { knowledgeApi } from '@/api/knowledge'

export const useKnowledgeStore = defineStore('knowledge', {
  state: () => ({
    items: [],
    searchResults: [],
  }),

  actions: {
    async search(query) {
      const res = await knowledgeApi.search(query)
      this.searchResults = res.data.data || []
    }
  }
})
```

- [ ] **Step 3: 创建消息气泡组件**

```vue
<!-- src/components/chat/MessageBubble.vue -->
<template>
  <div class="flex" :class="isUser ? 'justify-end' : 'justify-start'">
    <div
      class="max-w-[75%] rounded-xl px-4 py-2.5"
      :class="isUser
        ? 'bg-blue-600 text-white rounded-br-sm'
        : 'bg-gray-100 text-gray-800 rounded-bl-sm'"
    >
      <p class="text-sm whitespace-pre-wrap">{{ message }}</p>
      <p class="text-xs mt-1" :class="isUser ? 'text-blue-200' : 'text-gray-400'">
        {{ time }}
      </p>
    </div>
  </div>
</template>

<script setup>
defineProps({
  message: String,
  isUser: Boolean,
  time: String
})
</script>
```

- [ ] **Step 4: 创建输入框组件**

```vue
<!-- src/components/chat/ChatInput.vue -->
<template>
  <div class="border-t border-gray-200 bg-white p-4">
    <div class="flex items-end gap-2 max-w-4xl mx-auto">
      <textarea
        v-model="text"
        placeholder="输入消息，或粘贴链接/图片..."
        class="flex-1 resize-none rounded-xl border border-gray-300 px-4 py-2.5 text-sm focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
        rows="2"
        @keydown.enter.exact="handleSend"
      ></textarea>
      <button
        @click="handleSend"
        :disabled="!text.trim() || disabled"
        class="px-4 py-2.5 bg-blue-600 text-white rounded-xl text-sm hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
      >
        发送
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['send'])
defineProps({ disabled: Boolean })

const text = ref('')

function handleSend() {
  if (text.value.trim()) {
    emit('send', text.value.trim())
    text.value = ''
  }
}
</script>
```

- [ ] **Step 5: 创建思考状态指示器**

```vue
<!-- src/components/chat/ThinkingIndicator.vue -->
<template>
  <div class="flex justify-start">
    <div class="bg-gray-100 rounded-xl rounded-bl-sm px-4 py-3">
      <div class="flex items-center gap-2">
        <div class="flex gap-1">
          <span class="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style="animation-delay: 0ms"></span>
          <span class="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style="animation-delay: 150ms"></span>
          <span class="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style="animation-delay: 300ms"></span>
        </div>
        <span class="text-sm text-gray-500">正在检索知识库...</span>
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 6: 创建聊天主视图**

```vue
<!-- src/views/ChatView.vue -->
<template>
  <div class="flex flex-col h-full">
    <!-- 消息区域 -->
    <div class="flex-1 overflow-y-auto p-4 space-y-4" ref="messageContainer">
      <div v-if="!store.messages.length" class="flex flex-col items-center justify-center h-full text-gray-400">
        <p class="text-4xl mb-4">🧠</p>
        <p class="text-lg font-medium text-gray-600">开始你的第一段对话</p>
        <p class="text-sm mt-1">输入问题或分享知识，知忆会帮你整理和检索</p>
      </div>

      <MessageBubble
        v-for="msg in store.messages"
        :key="msg.id"
        :message="msg.content"
        :isUser="msg.role === 'USER'"
        :time="formatTime(msg.createdAt)"
      />

      <ThinkingIndicator v-if="store.isLoading" />
    </div>

    <!-- 输入区域 -->
    <ChatInput
      :disabled="store.isLoading"
      @send="handleSend"
    />
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { useChatStore } from '@/stores/chat'
import MessageBubble from '@/components/chat/MessageBubble.vue'
import ChatInput from '@/components/chat/ChatInput.vue'
import ThinkingIndicator from '@/components/chat/ThinkingIndicator.vue'

const store = useChatStore()
const messageContainer = ref(null)

// 新消息时自动滚动到底部
watch(() => store.messages.length, async () => {
  await nextTick()
  if (messageContainer.value) {
    messageContainer.value.scrollTop = messageContainer.value.scrollHeight
  }
})

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

async function handleSend(content) {
  await store.sendMessage(content)
}
</script>
```

- [ ] **Step 7: 提交**

```bash
git add frontend/src/components/ frontend/src/views/ frontend/src/stores/
git commit -m "feat: 实现聊天界面组件 + 消息气泡/输入框/思考动画"
```

---

### Task 10: 设置页面（模型配置管理）

**Files:**
- Create: `frontend/src/views/SettingsView.vue`
- Create: `frontend/src/components/layout/AppHeader.vue`

- [ ] **Step 1: 创建设置页面**

```vue
<!-- src/views/SettingsView.vue -->
<template>
  <div class="p-6 max-w-4xl">
    <h2 class="text-2xl font-bold text-gray-800 mb-6">⚙️ 系统设置</h2>

    <!-- 模型配置区域 -->
    <section class="bg-white rounded-xl border border-gray-200 p-6">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-lg font-semibold text-gray-700">模型配置</h3>
        <button @click="showAddForm = true"
          class="px-3 py-1.5 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700">
          + 添加模型
        </button>
      </div>

      <!-- 模型列表 -->
      <table class="w-full text-sm" v-if="models.length">
        <thead>
          <tr class="border-b border-gray-100 text-gray-500">
            <th class="text-left py-2">供应商</th>
            <th class="text-left py-2">模型</th>
            <th class="text-left py-2">类型</th>
            <th class="text-center py-2">状态</th>
            <th class="text-center py-2">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in models" :key="m.id" class="border-b border-gray-50">
            <td class="py-3">{{ m.provider }}</td>
            <td class="py-3">
              {{ m.modelName }}
              <span v-if="m.isPrimary" class="ml-2 px-1.5 py-0.5 bg-blue-100 text-blue-600 text-xs rounded">主模型</span>
            </td>
            <td class="py-3">{{ m.modelType }}</td>
            <td class="py-3 text-center">
              <span class="px-2 py-0.5 text-xs rounded"
                :class="m.isEnabled ? 'bg-green-100 text-green-600' : 'bg-gray-100 text-gray-400'">
                {{ m.isEnabled ? '启用' : '禁用' }}
              </span>
            </td>
            <td class="py-3 text-center">
              <div class="flex items-center justify-center gap-2">
                <button v-if="!m.isPrimary" @click="setPrimary(m.id)"
                  class="text-blue-600 hover:text-blue-800 text-xs">设为主模型</button>
                <button @click="testConnection(m.id)"
                  class="text-gray-500 hover:text-gray-700 text-xs">测试</button>
                <button @click="deleteModel(m.id)"
                  class="text-red-500 hover:text-red-700 text-xs">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <p v-else class="text-gray-400 text-sm py-4">暂无模型配置，点击上方按钮添加</p>
    </section>

    <!-- 添加模型弹窗 -->
    <div v-if="showAddForm" class="fixed inset-0 bg-black/30 flex items-center justify-center z-50"
         @click.self="showAddForm = false">
      <div class="bg-white rounded-xl p-6 w-96 shadow-xl">
        <h3 class="text-lg font-semibold mb-4">添加模型</h3>
        <div class="space-y-3">
          <div>
            <label class="block text-sm text-gray-600 mb-1">供应商</label>
            <select v-model="form.provider"
              class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:border-blue-500">
              <option value="ALIYUN">阿里通义千问</option>
              <option value="DEEPSEEK">DeepSeek</option>
              <option value="OPENAI">OpenAI</option>
              <option value="ANTHROPIC">Anthropic</option>
              <option value="OLLAMA">Ollama（本地）</option>
            </select>
          </div>
          <div>
            <label class="block text-sm text-gray-600 mb-1">模型名称</label>
            <input v-model="form.modelName" placeholder="如 qwen-turbo"
              class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:border-blue-500" />
          </div>
          <div>
            <label class="block text-sm text-gray-600 mb-1">API Key</label>
            <input v-model="form.apiKey" type="password" placeholder="sk-..."
              class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:border-blue-500" />
          </div>
          <div>
            <label class="block text-sm text-gray-600 mb-1">Base URL（可选）</label>
            <input v-model="form.baseUrl" placeholder="https://api.openai.com/v1"
              class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:border-blue-500" />
          </div>
        </div>
        <div class="flex justify-end gap-2 mt-6">
          <button @click="showAddForm = false"
            class="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">取消</button>
          <button @click="addModel"
            class="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { modelApi } from '@/api/models'

const models = ref([])
const showAddForm = ref(false)
const form = ref({ provider: 'ALIYUN', modelName: '', apiKey: '', baseUrl: '' })

async function loadModels() {
  const res = await modelApi.list()
  models.value = res.data.data || []
}

async function addModel() {
  await modelApi.add(form.value)
  showAddForm.value = false
  form.value = { provider: 'ALIYUN', modelName: '', apiKey: '', baseUrl: '' }
  await loadModels()
}

async function setPrimary(id) {
  await modelApi.setPrimary(id)
  await loadModels()
}

async function testConnection(id) {
  const res = await modelApi.testConnection(id)
  alert(res.data.data ? '✅ 连接成功' : '❌ 连接失败')
}

async function deleteModel(id) {
  if (confirm('确定删除此模型配置？')) {
    await modelApi.delete(id)
    await loadModels()
  }
}

onMounted(loadModels)
</script>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/views/SettingsView.vue
git commit -m "feat: 实现模型配置管理页面（添加/测试/主模型切换）"
```

---

### Task 11: Docker 容器化部署

**Files:**
- Create: `Dockerfile.backend`
- Create: `Dockerfile.frontend`
- Create: `docker/docker-compose.yml`
- Create: `frontend/nginx.conf`

- [ ] **Step 1: 创建后端 Dockerfile**

```dockerfile
# Dockerfile.backend
# JDK 21 分层构建 Spring Boot 应用

# 构建阶段
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build
COPY backend/pom.xml .
COPY backend/src ./src
RUN apt-get update && apt-get install -y maven && \
    mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: 创建前端 Nginx 配置**

```
# frontend/nginx.conf
server {
    listen 80;
    server_name localhost;

    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 代理后端 API
    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_buffering off;
    }
}
```

- [ ] **Step 3: 创建前端 Dockerfile**

```dockerfile
# Dockerfile.frontend
FROM node:20-alpine AS builder
WORKDIR /build
COPY frontend/package.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

FROM nginx:alpine
COPY --from=builder /build/dist /usr/share/nginx/html
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

- [ ] **Step 4: 创建 docker-compose.yml**

```yaml
# docker/docker-compose.yml
version: '3.8'

services:
  db:
    image: pgvector/pgvector:0.5.1-pg16
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: mindvault
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  backend:
    build:
      context: .
      dockerfile: Dockerfile.backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/mindvault
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=postgres
    depends_on:
      db:
        condition: service_healthy
    restart: unless-stopped

  frontend:
    build:
      context: .
      dockerfile: Dockerfile.frontend
    ports:
      - "3000:80"
    depends_on:
      - backend
    restart: unless-stopped

volumes:
  postgres_data:
```

- [ ] **Step 5: 提交**

```bash
git add Dockerfile.backend Dockerfile.frontend docker/ frontend/nginx.conf
git commit -m "feat: Docker 容器化部署配置（后端 + 前端 + PGVector）"
```

---

### Task 12: 集成测试与验证

**Files:** （无新增，运行现有代码）

- [ ] **Step 1: 本地启动 PostgreSQL**

```bash
docker run -d --name mindvault-db \
  -e POSTGRES_DB=mindvault \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  pgvector/pgvector:0.5.1-pg16
```

- [ ] **Step 2: 启动后端**

```bash
cd backend && mvn spring-boot:run
```

- [ ] **Step 3: 启动前端**

```bash
cd frontend && npm install && npm run dev
```

- [ ] **Step 4: 端到端验证**

```bash
# 验证后端健康
curl http://localhost:8080/api/v1/models

# 添加模型配置
curl -X POST http://localhost:8080/api/v1/models \
  -H 'Content-Type: application/json' \
  -d '{"provider":"ALIYUN","modelName":"qwen-turbo","apiKey":"test-key","modelType":"CHAT"}'

# 设置主模型
curl -X PATCH http://localhost:8080/api/v1/models/1/primary

# 添加知识
curl -X POST http://localhost:8080/api/v1/knowledge \
  -H 'Content-Type: application/json' \
  -d '{"title":"虚拟线程入门","content":"JDK 21 引入虚拟线程..."}'

# 搜索知识
curl "http://localhost:8080/api/v1/knowledge/search?q=虚拟线程"

# 创建会话并发送消息
curl -X POST http://localhost:8080/api/v1/chat/sessions
curl -X POST http://localhost:8080/api/v1/chat/sessions/1/messages \
  -H 'Content-Type: application/json' \
  -d '{"content":"介绍一下虚拟线程"}'
```

- [ ] **Step 5: 检查操作日志**

```bash
curl http://localhost:8080/api/v1/operation-logs
# 或者检查日志文件
tail -f logs/operation.log
```

- [ ] **Step 6: 前端验证**

浏览器访问 `http://localhost:5173`，确认：
- 左侧边栏显示
- 聊天页面可输入消息
- 设置页面可添加/管理模型
- 前后端联调正常

- [ ] **Step 7: Docker 全栈验证**

```bash
cd docker && docker-compose up -d
# 访问 http://localhost:3000
```

---

## 自检清单

- [ ] 所有 SQL 迁移脚本覆盖了完整表结构
- [ ] AgentScope @Tool 的名称和描述清晰，LLM 能正确识别
- [ ] 虚拟线程已启用（application.yml 中 spring.threads.virtual.enabled=true）
- [ ] 所有 Controller 返回 ApiResponse 统一格式
- [ ] 操作日志在关键业务流程中记录
- [ ] CORS 允许前端跨域访问
- [ ] Docker Compose 三容器编排完整