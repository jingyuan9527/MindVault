# MindVault 知忆 v0.1 — 最小可用版设计文档

> 日期：2026-06-17
> 状态：已批准

## 一、概述

v0.1 目标是跑通"Web 端输入 → 存储 → 检索"完整闭环，实现一个可用的基础聊天 + 知识库系统。

## 二、技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| JDK | OpenJDK | 21 LTS（虚拟线程） |
| 后端框架 | Spring Boot | 3.2.x |
| Agent 框架 | AgentScope Java | 2.0.0-RC2 |
| 数据库 | PostgreSQL | 16 + PGVector |
| 前端 | Vue 3 + TailwindCSS | 3.4.x |
| 构建 | Maven | - |
| 部署 | Docker Compose | - |

## 三、项目结构

采用 **Package-by-Feature**（按功能分包）策略：

```
mind-vault/
├── backend/                          # Spring Boot 后端
│   ├── src/main/java/com/mindvault/
│   │   ├── MindVaultApplication.java
│   │   ├── agent/                    # AgentScope Agent 层
│   │   │   ├── MindVaultAgent.java
│   │   │   ├── tools/
│   │   │   │   ├── AddKnowledgeTool.java
│   │   │   │   └── SearchKnowledgeTool.java
│   │   │   └── config/
│   │   │       └── AgentConfig.java
│   │   ├── knowledge/                # 知识库模块
│   │   │   ├── KnowledgeController.java
│   │   │   ├── KnowledgeService.java
│   │   │   ├── KnowledgeRepository.java
│   │   │   └── entity/Knowledge.java
│   │   ├── chat/                     # 聊天模块
│   │   │   ├── ChatController.java
│   │   │   └── ChatService.java
│   │   ├── model/                    # 模型配置模块
│   │   │   ├── ModelConfigController.java
│   │   │   ├── ModelConfigService.java
│   │   │   ├── ModelConfigRepository.java
│   │   │   └── entity/ModelConfig.java
│   │   ├── operationlog/             # 操作日志模块
│   │   │   ├── OperationLogService.java
│   │   │   └── entity/OperationLog.java
│   │   └── common/                   # 通用
│   │       ├── config/
│   │       │   ├── VirtualThreadConfig.java
│   │       │   └── CorsConfig.java
│   │       ├── dto/ApiResponse.java
│   │       └── exception/GlobalExceptionHandler.java
│   └── src/main/resources/
│       ├── application.yml
│       ├── logback-spring.xml        # 日志配置
│       └── db/migration/             # Flyway
├── frontend/                         # Vue 3 前端
│   ├── src/
│   │   ├── router/index.js
│   │   ├── stores/chat.js, knowledge.js
│   │   ├── views/ChatView.vue, SettingsView.vue
│   │   ├── components/chat/, layout/, common/
│   │   └── api/chat.js, knowledge.js
│   └── package.json, vite.config.js, tailwind.config.js
└── docker/docker-compose.yml
```

## 四、数据库表设计

### 4.1 knowledge — 知识条目

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | 自增主键 |
| title | VARCHAR(500) | 知识标题 |
| content | TEXT | 正文 |
| content_type | VARCHAR(20) | TEXT\|PDF\|URL\|IMAGE |
| source_url | TEXT | 来源 URL |
| summary | TEXT | 摘要（v0.2 用） |
| tags | JSONB | 标签数组 |
| embedding | VECTOR(1536) | 向量嵌入 |
| metadata | JSONB | 扩展元数据 |
| created_at / updated_at | TIMESTAMPTZ | 时间戳 |

### 4.2 model_config — 模型配置

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | 自增主键 |
| provider | VARCHAR(30) | 供应商 |
| model_name | VARCHAR(100) | 模型名 |
| model_type | VARCHAR(20) | CHAT\|EMBEDDING\|SUMMARIZE |
| api_key | TEXT | 密钥 |
| base_url | VARCHAR(500) | 自定义端点 |
| is_primary | BOOLEAN | 是否主模型 |
| priority | INT | 备用优先级 |
| metadata | JSONB | 扩展 |
| created_at / updated_at | TIMESTAMPTZ | 时间戳 |

### 4.3 chat_session / chat_message — 对话

**chat_session：** id (BIGSERIAL PK), title (VARCHAR(200)), created_at, updated_at

**chat_message：** id (BIGSERIAL PK), session_id (BIGINT FK → chat_session), role (USER|ASSISTANT|SYSTEM), content (TEXT), metadata (JSONB), created_at

### 4.4 operation_log — 操作日志

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | 自增主键 |
| module | VARCHAR(20) | KNOWLEDGE\|CHAT\|MODEL\|SYSTEM |
| action | VARCHAR(30) | ADD\|SEARCH\|DELETE 等 |
| entity_id | BIGINT | 操作对象 ID |
| summary | VARCHAR(500) | 可读描述 |
| detail | JSONB | 详细信息 |
| created_at | TIMESTAMPTZ | 记录时间 |

## 五、API 接口

统一前缀 `/api/v1`，统一响应 `{code, message, data}`：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/v1/chat/sessions | 创建会话 |
| GET | /api/v1/chat/sessions | 会话列表 |
| POST | /api/v1/chat/sessions/{id}/messages | 发消息（SSE 流式） |
| POST | /api/v1/knowledge | 添加知识 |
| GET | /api/v1/knowledge | 知识列表 |
| GET | /api/v1/knowledge/{id} | 知识详情 |
| DELETE | /api/v1/knowledge/{id} | 删除知识 |
| GET | /api/v1/knowledge/search?q= | 语义检索 |
| POST | /api/v1/models | 添加模型 |
| GET | /api/v1/models | 模型列表 |
| PATCH | /api/v1/models/{id}/primary | 设为主模型 |
| POST | /api/v1/models/{id}/test | 测试连接 |

## 六、Agent 工作流

用户消息 → MindVaultAgent 接收 → 判断是否调用 @Tool → 执行 search_knowledge / add_knowledge → LLM 生成回答 → SSE 流式返回。

AgentScope Java 关键点：
- `@Agent` 注解：声明 Agent 组件
- `@Tool(name, description)`：注册可调用工具
- Agent 自动决定何时调用工具（Function Calling）

## 七、日志设计

- **应用日志**：SLF4J + Logback，MDC 注入 traceId，按天滚动
- **操作日志**：写入 `operation_log` 表，记录操作人、操作对象、结果
- 日志贯穿：请求 → traceId → 业务日志 → DB 操作日志

## 八、前端设计

- Vue 3 Composition API + Pinia + Vue Router
- TailwindCSS 配色：blue-600 主色，green-500 成功，amber-500 警告
- 布局：w-72 左侧边栏 + flex-1 右侧主内容
- 聊天页：消息气泡（用户右/Agent 左）+ 输入框 + 思考动画
- 设置页：模型配置表格 + 添加/测试操作

## 九、部署

Docker Compose 三容器编排：backend（JDK 21）、frontend（Nginx）、db（pgvector/pgvector:0.5.1-pg16）。