# MindVault 知忆 — 软件设计说明书 (SDD)

> **版本**: 1.1.0 | **最后更新**: 2026-06-26 | **状态**: 正式发布

---

## 目录

1. [概述](#1-概述)
2. [技术栈](#2-技术栈)
3. [系统架构](#3-系统架构)
4. [模块设计](#4-模块设计)
5. [数据库设计](#5-数据库设计)
6. [API 接口设计](#6-api-接口设计)
7. [AI 自动处理流水线](#7-ai-自动处理流水线)
8. [前端设计](#8-前端设计)
9. [部署设计](#9-部署设计)
10. [安全设计](#10-安全设计)
11. [监控与可观测性](#11-监控与可观测性)

---

## 1. 概述

MindVault（知忆）是一个**个人知识库 AI Agent** 系统，核心能力：

- **知识管理**：支持文本、URL 网页、PDF 文件的知识采集与结构化存储
- **AI 自动处理**：自动生成标题/标签/摘要/向量嵌入、发现知识关联、聚合统计
- **智能聊天**：基于知识库上下文的 AI 对话，支持 SSE 流式响应
- **间隔复习**：SM-2 算法调度知识复习，支持闪卡（问答卡）
- **每日复盘**：LLM 自动生成每日知识总结报告
- **写作助手**：基于知识库内容的 AI 文章生成

### 1.1 设计原则

| 原则 | 说明 |
|------|------|
| **Package-by-Feature** | 按功能分包，每个模块自包含 controller/service/mapper/entity/dto |
| **SOLID** | 单一职责、开闭原则、接口隔离、依赖倒置、策略模式 |
| **TDD** | 80%+ 业务逻辑可自动化测试，先写验证再写实现 |
| **DDD 分层** | Controller → Service(接口) → ServiceImpl(实现) → Mapper → DB |
| **AI 优先** | 核心流水线自动处理，用户可随时覆盖 AI 结果 |

### 1.2 项目结构

```
mind-vault/
├── backend/                          # Spring Boot 3.4 + JDK 21
│   ├── src/main/java/com/mindvault/
│   │   ├── MindVaultApplication.java # 启动入口（虚拟线程）
│   │   ├── ai/                       # Spring AI 封装（ModelFactory + PromptRegistry）
│   │   ├── agent/                    # Agent 服务 + LLM failover + 工具
│   │   ├── auth/                     # 认证授权 + 用户管理 + API Token
│   │   ├── auto/                     # AI 自动处理流水线（R1/R2/R3 + 调度器）
│   │   ├── backup/                   # 数据备份（pg_dump）
│   │   ├── chat/                     # 聊天对话 + SSE 流式 + 关键词拦截
│   │   ├── common/                   # 全局异常处理、CORS、监控、工具
│   │   ├── content/                  # URL（Jsoup）+ PDF（PDFBox）内容解析
│   │   ├── dailyreview/              # 每日复盘（LLM 报告生成）
│   │   ├── flashcard/                # 闪卡 CRUD + AI 生成
│   │   ├── knowledge/                # 知识库核心（CRUD/搜索/导入导出/标签）
│   │   ├── model/                    # LLM 模型配置管理
│   │   ├── operationlog/             # 操作审计日志（AOP 自动记录）
│   │   ├── review/                   # SM-2 间隔复习调度
│   │   ├── systemconfig/             # 系统配置 KV 管理 + 定时任务
│   │   ├── tokenusage/               # Token 用量追踪
│   │   └── writing/                  # 写作助手（AI 文章生成）
│   ├── src/main/resources/
│   │   └── application.yml
│   └── src/test/resources/
│       └── application.properties (auth disabled)
├── frontend/                         # Vue 3 + Tailwind + Naive UI
│   ├── src/
│   │   ├── router/index.ts           # 14 条路由
│   │   ├── stores/                   # 5 个 Pinia Store
│   │   ├── views/                    # 14 个视图
│   │   ├── components/               # 10 个组件（chat/common/knowledge/layout）
│   │   └── api/                      # 15 个 API 模块
│   └── package.json
├── docker/
│   ├── docker-compose.yml            # 三容器编排
│   └── migration-v0.4.sql            # AI 流水线表
│   └── migration-v0.5.sql            # system_config + 120 默认项
│   └── migration-v0.6-operationlog.sql # 操作日志分区
├── Dockerfile.backend                # 多阶段 Maven 构建
├── Dockerfile.frontend               # node build → nginx
└── docs/
    ├── SDD.md                        # ← 本文档
    └── deployment.md
```

---

## 2. 技术栈

| 层级 | 技术 | 版本 | 用途 |
|------|------|------|------|
| **语言** | Java (OpenJDK) | 21 LTS | 虚拟线程、模式匹配 |
| **后端框架** | Spring Boot | 3.4.3 | REST API、DI、AOP |
| **ORM** | MyBatis-Plus | 3.5.9 | 数据访问（非 JPA） |
| **AI 框架** | Spring AI | 2.0.0 | ChatModel / EmbeddingModel |
| **数据库** | PostgreSQL | 16 + pgvector | 关系数据 + 向量搜索（HNSW） |
| **API 文档** | Knife4j + SpringDoc | 4.5.0 | OpenAPI 3 交互式文档 |
| **监控** | Micrometer + Prometheus | - | 指标暴露 |
| **缓存** | Caffeine | - | 限流、配置缓存 |
| **前端框架** | Vue 3 | 3.4+ | Composition API + `<script setup>` |
| **状态管理** | Pinia | 2.1+ | auth/chat/knowledge/theme/toast |
| **UI 库** | Naive UI | 2.44 | 组件库 + 主题覆盖 |
| **CSS** | Tailwind CSS | 3.4 | 原子化样式 |
| **测试（后端）** | JUnit 5 + WireMock + H2 | - | 326 测试，53% 覆盖率 |
| **测试（前端）** | Vitest + happy-dom | - | 88 测试，19 文件 |
| **容器化** | Docker Compose | - | db/backend/frontend 三容器 |

---

## 3. 系统架构

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                      Nginx (反向代理)                      │
│  ┌─────────────────────┐  ┌───────────────────────────┐  │
│  │   Frontend (Vue 3)  │  │   Backend (Spring Boot)   │  │
│  │   localhost:3000     │  │   localhost:8080          │  │
│  │                      │  │                           │  │
│  │  ┌───────────────┐  │  │  ┌─────────────────────┐  │  │
│  │  │    14 Views    │  │  │  │   18 模块 / 80 接口   │  │  │
│  │  │  5 Stores      │  │  │  │  Controller → Service│  │  │
│  │  │  10 Components │  │  │  │  → Mapper → DB       │  │  │
│  │  └───────────────┘  │  │  └─────────────────────┘  │  │
│  └─────────────────────┘  └───────────────────────────┘  │
│                                │                          │
│                        ┌───────┴───────┐                  │
│                        │  PostgreSQL   │                  │
│                        │  16 + pgvector│                  │
│                        └───────────────┘                  │
└─────────────────────────────────────────────────────────┘
```

### 3.2 分层架构（后端）

```
Controller Layer    → @RestController, 接收 HTTP 请求
      ↓
Service Layer       → 接口 + 实现（DIP），业务逻辑编排
      ↓
Mapper Layer        → MyBatis-Plus BaseMapper，数据访问
      ↓
Database Layer      → PostgreSQL 16 + pgvector
```

横切关注点：
- **Security**: AuthFilter → SessionManager → AuthService
- **Audit**: @OperationLog → OperationLogAspect → operation_log
- **Metrics**: Micrometer → Prometheus → /actuator/prometheus
- **Exception**: @RestControllerAdvice → 统一错误响应

### 3.3 AI Pipeline 架构

```
用户创建知识 ──→ @Async (R1) ──→ aiTitle + aiTags + summary + embedding
                                   ↓ status = TITLE_TAG_DONE
                             AutoProcessScheduler (每 5 分钟)
                                   ↓ R2
                             RelationService ──→ knowledge_relation
                                   ↓ status = RELATION_DONE
                             AutoProcessScheduler (每 30 分钟)
                                   ↓ R3
                             AggregationService ──→ tag cloud + stats
                                   ↓ status = COMPLETED
```

### 3.4 请求生命周期

```
HTTP Request
  → Nginx (/api/ → backend:8080)
    → AuthFilter (验证 session / API token)
      → Controller (参数校验)
        → Service (业务逻辑 + AI 调用)
          → Mapper (数据库操作)
        ← Service
      ← Controller
    ← AuthFilter (操作日志)
  ← Nginx
```

---

## 4. 模块设计

### 4.1 后端模块一览

| 包名 | 职责 | 实体 | 接口数 | 测试状态 |
|------|------|------|--------|----------|
| `auth` | 用户认证、Token 管理、会话 | users, api_tokens | 7 (+1: `UserAdminController`) | ✅ |
| `knowledge` | 知识 CRUD、搜索、标签、导入导出 | knowledge | 23 | ✅ |
| `chat` | 对话、SSE 流式 | chat_session, chat_message | 5 | ✅ |
| `model` | AI 模型配置 CRUD、测试连接 | model_config | 7 | ✅ |
| `review` | SM-2 间隔复习 | review_schedule | 4 | ✅ |
| `flashcard` | 闪卡管理 | flash_card | 4 | ✅ |
| `dailyreview` | 每日回顾报告 | daily_review | 4 | ✅ |
| `writing` | AI 写作助手 | - | 1 | ✅ |
| `operationlog` | 操作审计日志 | operation_log | 2 | ✅ |
| `tokenusage` | Token 用量追踪 | token_usage | 3 | ✅ |
| `backup` | 数据库备份恢复 | - | 3 | ✅ |
| `user`（auth.controller）| 用户管理 | users | 2 | ✅ |
| `auto`（含 r2/r3/scheduler）| R1/R2/R3 自动处理 | auto_process_log, knowledge_relation | 0¹ | ✅ |
| `agent` | LLM failover + 工具调用 | - | 0² | ✅ |
| `content` | URL/PDF 内容解析 | - | 0¹ | ✅ |
| `ai` | Spring AI 封装 | - | - | - |
| `system` | 系统监控/管理 | - | 3 | ✅ |
| `systemconfig` | 系统配置 + 定时任务管理 | system_config | 12 | ✅ |

¹ 功能通过 `KnowledgeController` 或调度器暴露  
² 纯服务层模块，无 REST 接口，但有 Service 测试

### 4.2 核心模块详述

#### 4.2.1 auth — 认证模块
- **用户模型**: id, username, password(bcrypt), display_name, enabled, role(USER/ADMIN)
- **认证方式**: Session（共享内存）+ API Token（UUID，不限量）
- **Token 管理**: 创建/列表/删除，支持设置过期天数
- **初始化**: 首次启动自动创建 admin 用户（环境变量配置）

#### 4.2.2 knowledge — 知识库模块
- **双字段设计**: 用户字段（title, content, user_tags）+ AI 字段（ai_title, ai_tags, summary）
- **显示策略**: displayTitle() = aiTitle || title，合并标签 = ai_tags + user_tags
- **搜索**: 多维度（关键词 + 标签 + 向量相似度），分页
- **导入导出**: JSON（保留完整结构）、Markdown、CSV
- **内容解析**: URL（Jsoup）→ Markdown，PDF（PDFBox）→ Markdown

#### 4.2.3 chat — 对话模块
- **会话管理**: 创建/删除/重命名会话
- **流式响应**: SSE（Server-Sent Events）实现打字机效果
- **消息模型**: role(USER/AI/SYSTEM), content, tokens, model

#### 4.2.4 review — 间隔复习模块
- **算法**: SM-2（SuperMemo 2），5 级质量评分
- **调度参数**: 首次间隔 1 天，第二次 6 天，之后按 EF 计算
- **到期查询**: 查询今日到期复习项，支持手动更新

#### 4.2.5 relation — 关联发现模块 (R2)
- **语义相似度**: 基于 embedding 向量余弦相似度
- **标签重叠**: 标签交集 + 加权
- **LLM 分析**: 调用模型进行深层语义关联
- **存储**: knowledge_relation 表（knowledge_id, related_id, relation_type, score, source）

### 4.3 前端模块

14 个路由视图、5 个 Pinia Store、公共组件按功能域组织。

**菜单分组**: 侧边栏按产品心智模型分为三组，降低用户认知负担：

| 分组 | 路由 | 视图 | Store |
|------|------|------|-------|
| **📝 笔记** | `/` | 知识库（Memos 风格输入置顶 + 信息流） | knowledgeStore |
| | `/chat` | AI 对话（SSE 流式） | chatStore |
| **🧠 学习** | `/review` | 间隔复习（SM-2 调度） | - |
| | `/flashcards` | 闪卡（3D 翻转动画） | - |
| | `/writing` | AI 写作助手 | - |
| | `/daily-review` | 每日回顾报告 | - |
| **⚙️ 管理** | `/users` | 用户管理 | - |
| | `/operation-logs` | 操作日志列表 | - |
| | `/token-usage` | Token 用量可视化 | - |
| | `/backups` | 数据备份管理 | - |
| | `/system` | 系统监控 | - |
| | `/settings` | 模型配置 + API Token + 系统配置 | - |

（登录页 `/login` 独立，无侧边栏）

**主题系统**: 浅色/深色双主题，spec 色值驱动，CSS 变量 + `data-theme` 属性切换。NaiveUI 通过 `theme-overrides` 统一覆盖。localStorage 持久化。

---

## 5. 数据库设计

### 5.1 实体关系概览

```
api_tokens ──→ users (user_id)
knowledge 1──N knowledge_relation (knowledge_id / related_id)
knowledge 1──N auto_process_log (knowledge_id)
knowledge 1──N review_schedule (knowledge_id)
knowledge 1──N flash_card (knowledge_id)
chat_session 1──N chat_message (session_id)
```

注：用户上下文（userId）通过 AuthFilter 从 Session/Token 提取并设置到请求上下文，
不作为外键存储在业务表中。`api_tokens` 是唯一包含 `user_id` 外键的表。

### 5.2 表结构

#### 5.2.1 users — 用户表
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGSERIAL PK | 主键 |
| username | VARCHAR(50) UNIQUE NOT NULL | 用户名 |
| password_hash | VARCHAR(255) NOT NULL | bcrypt 哈希 |
| display_name | VARCHAR(100) | 显示名 |
| enabled | BOOLEAN DEFAULT TRUE | 是否启用 |
| role | VARCHAR(20) DEFAULT 'USER' | USER / ADMIN |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

#### 5.2.2 api_tokens — API Token 表
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGSERIAL PK | 主键 |
| user_id | BIGINT FK → users | 用户 ID |
| name | VARCHAR(100) | Token 名称 |
| token | VARCHAR(64) UNIQUE NOT NULL | UUID Token |
| last_used_at | TIMESTAMP | 最后使用时间 |
| expires_at | TIMESTAMP | 过期时间 |
| created_at | TIMESTAMP | 创建时间 |

#### 5.2.3 knowledge — 知识表
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGSERIAL PK | 主键 |
| title | VARCHAR(500) | 用户标题 |
| ai_title | VARCHAR(500) | AI 生成标题 |
| content | TEXT | 内容（Markdown） |
| content_type | VARCHAR(20) DEFAULT 'TEXT' | TEXT/URL/PDF |
| source_url | VARCHAR(2048) | 来源 URL |
| tags | JSONB DEFAULT '[]' | AI 生成标签数组（JsonbStringTypeHandler） |
| user_tags | JSONB DEFAULT '[]' | 用户标签数组（JsonbStringTypeHandler） |
| summary | TEXT | AI 生成摘要 |
| embedding | VARCHAR | 向量嵌入（Java 侧为 String，DB 侧为 vector(1536)） |
| metadata | JSONB DEFAULT '{}' | 元数据（JsonbStringTypeHandler） |
| auto_process_status | VARCHAR(20) DEFAULT 'PENDING' | PENDING/TITLE_TAG_DONE/RELATION_DONE/COMPLETED |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

索引：
- `idx_knowledge_user_id` ON user_id（实际无 user_id 列，此为文档遗留索引参考）
- `idx_knowledge_auto_process_status` ON auto_process_status
- `idx_knowledge_user_tags` GIN ON user_tags
- `idx_knowledge_tags` GIN ON tags
- `idx_knowledge_embedding` HNSW ON embedding (vector_cosine_ops)

#### 5.2.4 chat_session — 会话表
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGSERIAL PK | 主键 |
| title | VARCHAR(500) | 会话标题 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

#### 5.2.5 chat_message — 消息表
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGSERIAL PK | 主键 |
| session_id | BIGINT FK → chat_session | 会话 ID |
| role | VARCHAR(20) | USER/ASSISTANT/SYSTEM |
| content | TEXT | 消息内容 |
| metadata | JSONB DEFAULT '{}' | 元数据（JsonbStringTypeHandler） |
| sources | JSONB DEFAULT '[]' | 引用来源（JsonbStringTypeHandler） |
| created_at | TIMESTAMP | 创建时间 |

#### 5.2.6 model_config — 模型配置表
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGSERIAL PK | 主键 |
| provider | VARCHAR(20) | openai/deepseek/alibaba/siliconflow/ollama |
| model_name | VARCHAR(100) | 模型名 |
| model_type | VARCHAR(20) DEFAULT 'CHAT' | CHAT/EMBEDDING |
| api_key | VARCHAR(500) | API Key |
| base_url | VARCHAR(500) | API 地址 |
| is_primary | BOOLEAN DEFAULT FALSE | 是否为主模型 |
| is_enabled | BOOLEAN DEFAULT TRUE | 是否启用 |
| priority | INTEGER DEFAULT 0 | 排序优先级 |
| metadata | JSONB DEFAULT '{}' | 元数据（JsonbStringTypeHandler） |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

#### 5.2.7 review_schedule — 复习调度表
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGSERIAL PK | 主键 |
| knowledge_id | BIGINT FK → knowledge | 知识 ID |
| ease_factor | DECIMAL(5,2) DEFAULT 2.50 | SM-2 EF 值 |
| interval_days | INTEGER DEFAULT 0 | 复习间隔（天） |
| review_count | INTEGER DEFAULT 0 | 重复次数 |
| next_review_at | TIMESTAMP DEFAULT NOW() | 下次复习日期 |
| last_review_at | TIMESTAMP | 上次复习时间 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

#### 5.2.8 flash_card — 闪卡表
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGSERIAL PK | 主键 |
| knowledge_id | BIGINT FK → knowledge | 关联知识 |
| question | TEXT | 正面问题 |
| answer | TEXT | 背面答案 |
| difficulty | VARCHAR(20) DEFAULT 'MEDIUM' | EASY/MEDIUM/HARD |
| source_type | VARCHAR(20) DEFAULT 'AUTO' | AUTO/MANUAL |
| created_at | TIMESTAMP | 创建时间 |

#### 5.2.9 daily_review — 每日回顾表
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGSERIAL PK | 主键 |
| report_date | DATE | 回顾日期 |
| total_count | INTEGER DEFAULT 0 | 知识总数 |
| summary | TEXT | 回顾摘要 |
| key_insights | TEXT DEFAULT '[]' | 关键洞察（JSON 数组，JsonbStringTypeHandler） |
| recommendations | TEXT DEFAULT '[]' | 建议（JSON 数组，JsonbStringTypeHandler） |
| category_breakdown | TEXT DEFAULT '{}' | 分类统计（JSON 对象，JsonbStringTypeHandler） |
| created_at | TIMESTAMP | 创建时间 |

#### 5.2.10 knowledge_relation — 知识关联表
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGSERIAL PK | 主键 |
| knowledge_id | BIGINT FK → knowledge | 知识 A |
| related_id | BIGINT FK → knowledge | 知识 B |
| relation_type | VARCHAR(20) | COMPLEMENT/CONTRAST/EXTENSION/REFERENCE |
| score | DECIMAL(5,4) | 置信度 0.00~1.00 |
| source | VARCHAR(20) | VECTOR/TAG/LLM |
| created_at | TIMESTAMP | 创建时间 |

#### 5.2.11 auto_process_log — 自动处理日志表
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGSERIAL PK | 主键 |
| knowledge_id | BIGINT FK → knowledge | 知识 ID |
| round | VARCHAR(20) | R1_TITLE_TAG/R2_RELATION/R3_AGGREGATION |
| status | VARCHAR(20) | SUCCESS/FAILED |
| result_summary | TEXT | 结果摘要 |
| llm_tokens | INTEGER DEFAULT 0 | LLM Token 用量 |
| llm_duration_ms | INTEGER DEFAULT 0 | LLM 耗时（毫秒） |
| error_message | TEXT | 错误信息 |
| started_at | TIMESTAMP | 开始时间 |
| completed_at | TIMESTAMP | 完成时间 |
| created_at | TIMESTAMP | 创建时间 |

#### 5.2.12 token_usage — Token 用量表
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGSERIAL PK | 主键 |
| model_id | BIGINT | 模型配置 ID |
| provider | VARCHAR(50) | 提供商 |
| model_name | VARCHAR(100) | 模型名 |
| model_type | VARCHAR(20) DEFAULT 'CHAT' | CHAT/EMBEDDING |
| prompt_tokens | INTEGER | 输入 Tokens |
| completion_tokens | INTEGER | 输出 Tokens |
| total_tokens | INTEGER | 总 Tokens |
| cost | DECIMAL(10,6) DEFAULT 0 | 估算费用 |
| request_source | VARCHAR(50) DEFAULT 'CHAT' | CHAT/AUTO/AGENT/... |
| request_id | VARCHAR(100) | 请求 ID |
| created_at | TIMESTAMP | 创建时间 |

#### 5.2.13 system_config — 系统配置表
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGSERIAL PK | 主键 |
| config_key | VARCHAR(255) UNIQUE NOT NULL | 配置键 |
| config_value | TEXT | 配置值 |
| description | VARCHAR(500) | 描述 |
| value_type | VARCHAR(20) DEFAULT 'string' | string/int/bool/cron/prompt |
| updated_at | TIMESTAMP | 更新时间 |

#### 5.2.14 operation_log — 操作日志表
| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGSERIAL PK | 主键 |
| module | VARCHAR(50) | 模块名 |
| action | VARCHAR(100) | 操作 |
| action_type | VARCHAR(20) | CREATE/UPDATE/DELETE/OTHER |
| entity_id | VARCHAR(64) | 实体 ID |
| summary | VARCHAR(500) | 操作摘要 |
| detail | TEXT | 详情 JSON（JsonbStringTypeHandler） |
| before_snapshot | TEXT | 操作前快照 JSON（select=false，JsonbStringTypeHandler） |
| after_snapshot | TEXT | 操作后快照 JSON（select=false，JsonbStringTypeHandler） |
| operator | VARCHAR(100) | 操作人用户名 |
| operator_id | BIGINT | 操作人 ID |
| ip_address | VARCHAR(45) | 客户端 IP |
| result | VARCHAR(20) DEFAULT 'SUCCESS' | SUCCESS/FAILED |
| error_message | TEXT | 错误信息 |
| duration_ms | BIGINT DEFAULT 0 | 耗时（毫秒） |
| remark | VARCHAR(500) | 备注 |
| created_at | TIMESTAMP | 创建时间 |

---

## 6. API 设计

### 6.1 API 风格

- **基础路径**: `/api/v1/`
- **认证**: Session Cookie（浏览器）+ Bearer Token（API 客户端），通过 `AuthorizationFilter` 统一处理
- **响应格式**:
  ```json
  {
    "status": "success" | "error",
    "message": "...",
    "data": { ... }
  }
  ```
- **分页**: page/pageSize 参数，返回 total/list
- **错误码**: HTTP 状态码 + 统一异常处理

### 6.2 认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /auth/login | 用户登录 |
| POST | /auth/logout | 退出登录 |
| GET | /auth/me | 获取当前用户 |
| PUT | /auth/password | 修改密码 |
| GET | /auth/tokens | Token 列表 |
| POST | /auth/tokens | 创建 Token |
| DELETE | /auth/tokens/{id} | 删除 Token |

### 6.3 知识库接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /knowledge | 知识列表（分页 + 搜索） |
| GET | /knowledge/{id} | 知识详情 |
| POST | /knowledge | 创建知识 |
| PUT | /knowledge/{id} | 更新知识 |
| DELETE | /knowledge/{id} | 删除知识 |
| GET | /knowledge/search | 搜索（关键词） |
| GET | /knowledge/search/hyde | HyDE 搜索 |
| GET | /knowledge/search/rewrite | 查询改写搜索 |
| GET | /knowledge/{id}/related | 关联知识列表 |
| POST | /knowledge/{id}/reprocess | 重新 AI 处理 |
| GET | /knowledge/{id}/process-logs | AI 处理日志 |
| POST | /knowledge/parse-url | URL 解析 |
| POST | /knowledge/parse-pdf | PDF 解析 |
| GET | /knowledge/export/json | 导出 JSON |
| GET | /knowledge/export/markdown | 导出 Markdown |
| GET | /knowledge/export/csv | 导出 CSV |
| POST | /knowledge/import/preview | 导入预览 |
| POST | /knowledge/import | 导入 |
| POST | /knowledge/batch/delete | 批量删除 |
| POST | /knowledge/batch/tag | 批量打标签 |
| POST | /knowledge/batch/ai-tag | AI 批量打标签 |
| POST | /knowledge/batch/export | 批量导出 |
| GET | /knowledge/tags | 标签列表 |

### 6.4 对话接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /chat/sessions | 会话列表 |
| POST | /chat/sessions | 创建会话 |
| GET | /chat/sessions/{id}/messages | 消息列表 |
| POST | /chat/sessions/{id}/messages | 发送消息 |
| POST | /chat/sessions/{id}/messages/stream | 发送消息（SSE 流式） |

### 6.5 模型配置接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /models | 模型列表 |
| POST | /models | 创建配置 |
| PATCH | /models/{id}/primary | 设为主模型 |
| PATCH | /models/{id}/priority | 调整优先级 |
| DELETE | /models/{id} | 删除配置 |
| POST | /models/fetch | 拉取提供商模型列表 |
| POST | /models/{id}/test | 测试连接 |

### 6.6 复习接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /review/due | 到期复习项 |
| GET | /review/due-count | 到期复习数量 |
| POST | /review/{knowledgeId}/schedule | 为知识创建复习 |
| POST | /review/{knowledgeId}/perform | 执行复习（质量评分） |

### 6.7 闪卡接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /flashcards | 闪卡列表 |
| GET | /flashcards/knowledge/{knowledgeId} | 按知识查闪卡 |
| POST | /flashcards/generate/{knowledgeId} | AI 生成闪卡 |
| DELETE | /flashcards/{id} | 删除闪卡 |

### 6.8 其他接口

| 模块 | 路径前缀 | 接口数 | 说明 |
|------|----------|--------|------|
| DailyReview | /daily-review | 4 | 最新/按日期/近期/生成回顾报告 |
| Writing | /writing | 1 | AI 文章生成 |
| TokenUsage | /token-usage | 3 | 每日/按来源/总计查询 |
| Backup | /backup | 3 | 创建/列表/下载备份 |
| OperationLog | /operation-logs | 2 | 列表/详情查询 |
| User | /users | 2 | 用户列表/启用禁用 |
| System | /system | 3 | 健康/信息/指标 |
| SystemConfig | /system-config | 12 | 配置 CRUD + 模块导航 + 定时任务 + 还原 |

### 6.9 认证与安全

- **Session Cookie**: 浏览器登录后，服务端通过 `JSESSIONID` Cookie 识别用户
- **Bearer Token**: API 客户端通过 `Authorization: Bearer <token>` 头认证
- **AuthFilter**: 检查每个请求的认证状态，提取 `userId` 设置到请求上下文
- **密码**: bcrypt 哈希存储
- **API Key 加密**: 模型配置中的 `api_key` 加密存储

### 6.10 API 文档

- Knife4j UI: `/api/doc.html`
- Swagger UI: `/api/swagger-ui/index.html`
- OpenAPI JSON: `/api/v3/api-docs`
- 所有接口自动生成 OpenAPI 3.0 文档，支持在线调试

---

## 7. AI 处理流水线

### 7.1 R1 — 标题 + 标签 + 摘要 + 嵌入（同步 + @Async）
- **触发条件**: 知识创建完成 或 手动 reprocess
- **处理流程**:
  1. 截取内容前 3000 字符送 AI 生成标题
  2. AI 生成标签（返回 JSON 数组）
  3. AI 生成摘要
  4. 调用 EmbeddingModel 生成向量嵌入
- **失败处理**: 写入 auto_process_log，status = FAILED

### 7.2 R2 — 关联发现（定时任务，每 5 分钟）
- **处理对象**: status = TITLE_TAG_DONE 的知识
- **三种策略**:
  1. **语义相似度**: 两两向量余弦相似度 > 阈值（0.7）
  2. **标签重叠**: 计算标签 Jaccard 相似度 > 阈值（0.3）
  3. **LLM 分析**: 对高价值知识调用 LLM 分析深层关联
- **存储**: knowledge_relation 表

### 7.3 R3 — 聚合统计（定时任务，每 30 分钟）
- **处理对象**: status = RELATION_DONE 的知识
- **处理内容**: 重建标签云、刷新统计数据

---

## 8. 前端设计

### 8.1 技术基础

| 项 | 选型 | 版本 | 备注 |
|----|------|------|------|
| 框架 | Vue 3 | 3.4+ | Composition API + `<script setup>` |
| 构建 | Vite | 5.x | 开发服务器 + 生产构建 |
| UI 库 | Naive UI | 2.44+ | 仅 override 主题变量，不改源码 |
| 状态管理 | Pinia | 2.x | knowledge/chat/auth/theme 等 store |
| 路由 | Vue Router 4 | 4.x | `createWebHistory()` 模式 |
| 编辑器 | Markdown (marked) | 18.x | 纯编辑/编辑+预览双栏可切换 |
| 图标 | Naive UI 内置 + @vicons/ionicons5 | - | 仅使用线性线条图标 |
| 字体 | Inter | - | 中英文通用，代码块同字体 |

### 8.2 整体布局原则

**Memos 风格 — 输入置顶优先**。四个响应式断点：

| 断点 | 范围 | 布局 | 侧边栏 | 输入模块 |
|------|------|------|--------|----------|
| xl | ≥1440px | 左右双栏（侧边栏 240px + 内容区） | 固定 n-layout-sider，可折叠 | 置顶 + 支持编辑/预览双栏 |
| lg | 1024~1439px | 单栏通铺 | 汉堡图标 → n-drawer 抽屉 | 置顶通栏 |
| md | 768~1023px | 单栏，左右留白缩减 | 汉堡图标 → n-drawer 抽屉 | 通栏，强制纯编辑，预览走弹窗 |
| sm | <768px | 单栏垂直流式，0 左右分栏 | 汉堡图标 → 抽屉（80vw，max 320px） | 全满通栏，弹窗预览 |

所有页面禁止横向滚动条，弹性自适应布局，不写 `width` 固定值。

### 8.3 色彩体系

**主色**: `#2563eb`，浅主色 `#eff6ff`
**成功色**: `#10b981`，**警告色**: `#f97316`，**危险色**: `#ef4444`

**浅色模式**:
| 角色 | 色值 |
|------|------|
| 页面背景 | `#ffffff` |
| 卡片底色 | `#f8fafc` |
| 正文文字 | `#1e293b` |
| 次要文字 | `#64748b` |
| 占位文字 | `#94a3b8` |
| 分割边框 | `#e2e8f0` |

**深色模式**:
| 角色 | 色值 |
|------|------|
| 页面背景 | `#0f172a` |
| 卡片底色 | `#1e293b` |
| 正文文字 | `#f1f5f9` |
| 次要文字 | `#94a3b8` |
| 分割边框 | `#334155` |

**实现**: 保留 NaiveUI 内置 `darkTheme`，通过 `theme-overrides` 将 `common` 部分的 `bodyColor`、`cardColor`、`textColor` 等全部 override 为 spec 色值。深色模式传入 `darkTheme`，浅色模式传入 `null`。CSS 变量层同步定义供自定义组件使用。

### 8.4 圆角体系

| 控件 | 圆角 |
|------|------|
| 小型控件（按钮、tag） | 4px |
| 输入框 | 8px |
| 内容卡片 | 12px |
| 弹窗/抽屉 | 16px |

### 8.5 间距体系

基础单位 4px，全局仅使用：4 / 8 / 12 / 16 / 24 / 32 / 48px。
移动端（<768px）所有间距缩小至原尺寸 75%。

### 8.6 阴影规范

统一软阴影：`0 2px 12px rgba(0,0,0,0.06)`
深色模式透明度调整为 0.12
移动端弱化阴影透明度

### 8.7 字体与字号

**字体**: 中文使用系统无衬线（苹方/思源黑体），英文和代码使用 Inter。

**PC 字号层级**:
| 角色 | 字号 |
|------|------|
| 标题 | 24px |
| 副标题 | 18px |
| 正文 | 15px |
| 辅助小字 | 13px |
| 提示文字 | 12px |

**行高**: 正文 1.6，代码块 1.4
**响应式缩放**: 平板字号缩小 10%，手机缩小 15%；移动端行高统一 1.5

### 8.8 动效规范

所有过渡统一：`0.2s ease`
覆盖场景：hover、展开、弹窗、抽屉、侧边栏伸缩
**禁止**: 闪烁、旋转、发光、长时动画
**移动端**: 移除 hover 交互，改为点击触发反馈
**禁用方案**: 全局支持 `prefers-reduced-motion`

### 8.9 NaiveUI 组件视觉约束

- **Button**: 仅 primary / default / text 三类，统一圆角，hover 柔和变色；移动端最小点击区域 40×40px
- **Input**: 细边框，聚焦仅主色浅边框，去除原生外轮廓；移动端加高适配触控
- **Tag**: 小圆角色块，支持点击筛选/关闭删除，多状态区分配色
- **Scrollbar**: 全局细款透明样式，仅内容区域滚动；页面禁止横向滚动条
- **Modal**: 居中弹出，半透黑遮罩，圆角 16px；手机弹窗左右预留 16px 边距
- **Icon**: 仅使用 NaiveUI 线性线条图标，禁止填充/粗线条混用
- **Card**: 固定 12px 圆角，轻量化软阴影，无外边框；移动端内边距缩小
- **Popover**: PC hover 唤起，移动端 click 唤起

### 8.10 布局架构详解

#### 8.10.1 xl 大屏 PC（≥1440px）— 左右双栏

```
[ n-layout has-sider min-height 100vh ]
├── n-layout-sider (fixed, 240px, collapsible → icon-only)
│   ├── Logo (高度 64px)
│   ├── 菜单分组（三级：笔记/学习/管理）
│   ├── ─── 分隔线 ───
│   └── 底部固定栏（主题切换 + 设置）
└── n-layout-content (flex:1)
    ├── [置顶输入模块] — 常驻页面顶部
    │   ├── n-card（仅底部微弱阴影）
    │   │   ├── textarea（自动高度，无边框，透明底色，自动聚焦）
    │   │   └── 横向快捷工具栏（标签插入、图片上传[含TODO]、代码块、分割线、发布）
    │   │       支持切换双栏：左编辑 + 右实时 Markdown 预览
    ├── [信息流] — 垂直滚动内容卡片
    └── [右侧悬浮筛选] — 搜索 + 时间筛选 + 标签筛选
```

#### 8.10.2 lg 小屏 PC/横屏平板（1024~1439px）

- 无固定侧边栏，左侧汉堡图标唤起 n-drawer（遮罩点击关闭）
- 主内容通栏铺满，保留「顶部置顶输入 + 下方信息流」结构
- 右侧悬浮筛选面板默认收起，点击图标展开弹窗

#### 8.10.3 md 竖屏平板（768~1023px）

- 顶部通栏导航条（60px）：汉堡菜单 + 搜索 + 主题切换
- 置顶输入框通栏，工具栏多余按钮收拢至下拉菜单
- 信息流卡片自适应，左右留白缩减
- 编辑预览双栏模式关闭，仅纯编辑，预览通过弹窗

#### 8.10.4 sm 手机（<768px）

- 纯垂直流式，无左右分栏
- 顶部通栏导航：汉堡 + 搜索 + 主题切换
- 输入模块全屏加高置顶，工具栏全部收纳下拉弹窗
- 信息流卡片满宽，简化次要信息
- 分类/标签/归档全部由左侧抽屉承载（80vw, max 320px）

### 8.11 核心输入模块规范

1. 全程常驻页面顶部，不弹窗、不折叠、不后置
2. 输入主体无边框，底色贴合页面背景
3. 自动高度伸缩，多行无滚动条
4. 工具栏仅保留高频操作，不堆砌
5. 双栏编辑预览仅 xl 大屏可用，其余断点强制单编辑
6. 完整适配深浅双主题
7. **图片上传按钮保留占位，标记 TODO**，后端文件上传接口待后续实现

### 8.12 编辑模式

- **知识库使用原地编辑（inline-edit）**：点击卡片直接编辑该条内容，不弹出独立编辑弹窗
- 创建新内容：通过顶部输入模块直接创建，出现在信息流顶部
- 编辑内容：点击卡片上的编辑按钮，卡片就地切换为编辑态；保存后切回展示态

### 8.13 内容卡片规范

1. 轻量化 n-card，12px 圆角 + 软阴影，无粗边框
2. 卡片顶部：创建时间 + 多色标签组
3. 卡片主体：Markdown 渲染（marked + DOMPurify），图片自适应，代码块深色圆角
4. 卡片底部：编辑、收藏、复制、删除（极简操作栏）
5. 置顶内容左侧加主色竖标记条
6. 空状态使用 n-empty 组件，引导用户使用顶部输入
7. 卡片垂直间距 16px，左右自适应留白

### 8.14 侧边栏/抽屉规范

1. 扁平单层结构，禁用多层嵌套
2. 分组：笔记 / 学习 / 管理，组间分隔线
3. 选中：浅主色底色 + 左侧主色竖标识条；hover 浅灰底色；移动端仅点击反馈
4. 抽屉底部铺满操作按钮
5. 标签按使用量/字母排序（**移除拖拽排序**，因后端缺少 sort_order 支持）

### 8.15 全局响应式约束

| 组件 | 大屏 | 平板 | 手机 |
|------|------|------|------|
| n-message | 居中 | 居中 | 通栏宽度 |
| n-skeleton | 自适应 | 自适应 | 自适应 |
| n-breadcrumb | 完整路径 | 省略中间 | 省略中间 |
| n-modal | 固定居中宽度 | 固定居中宽度 | 全屏去边距 |
| n-popover | hover 唤起 | click 唤起 | click 唤起 |
| 底部操作面板 | 自定义 | 自定义 | 使用 `placement="bottom"` 抽屉 |

### 8.16 硬性禁止规则

1. 禁止传统三栏布局（文件夹+列表+大编辑器）
2. 禁止输入模块在弹窗或页面下方
3. 禁止高饱和渐变、发光特效、厚重黑色阴影
4. 禁止旋转、闪烁、复杂装饰动画；移动端删减全部非必要动效
5. 禁止多字体混用
6. 禁止大面积装饰背景图和多余装饰元素
7. 禁止粗重分割边框（靠底色和留白区分层级）
8. **全设备禁止横向滚动条**
9. 深浅主题切换后所有组件文字/边框对比度达标
10. 不修改 NaiveUI 底层组件源码，仅通过主题变量 + 自定义 class 调整

### 8.17 迭代计划

1. **阶段一（当前）**：重构 KnowledgeView 为 Memos 风格 + 更新全局布局/主题/配色
2. **阶段二**：按新规则逐一重构其余 13 个视图

---

## 9. 部署方案

### 9.1 容器架构

```
┌──────────────────────────────────────────────┐
│              Docker Compose                   │
│                                               │
│  ┌─────────┐  ┌─────────┐  ┌──────────────┐  │
│  │  db      │  │ backend  │  │  frontend     │  │
│  │ pgvector │←─│ JDK 21  │←─│  Nginx        │  │
│  │ :5432    │  │ :8080   │  │  :3000        │  │
│  └─────────┘  └─────────┘  └──────────────┘  │
│                                               │
│  Network: mindvault (bridge)                  │
└──────────────────────────────────────────────┘
```

### 9.2 容器详情

| 容器 | 镜像 | 端口 | 健康检查 | 依赖 |
|------|------|------|----------|------|
| db | pgvector/pgvector:pg16 | 5432 | pg_isready | - |
| backend | 多阶段 Maven 构建 | 8080 | GET /health | db |
| frontend | node build → nginx | 3000 | wget :80 | backend |

### 9.3 环境变量

| 变量 | 说明 |
|------|------|
| SPRING_DATASOURCE_URL | 数据库 JDBC URL |
| SPRING_DATASOURCE_USERNAME | 数据库用户名 |
| SPRING_DATASOURCE_PASSWORD | 数据库密码 |
| MINDVAULT_ADMIN_USERNAME | 初始 Admin 用户名 |
| MINDVAULT_ADMIN_PASSWORD | 初始 Admin 密码 |
| MINDVAULT_AUTH_ENABLED | 是否启用认证 |
| MINDVAULT_TEST_API_KEY | 测试 API Key |

---

## 10. 安全设计

- **认证层**: Session Cookie + Bearer Token 双重机制
- **密码存储**: bcrypt 哈希
- **API Key**: 模型配置加密存储
- **SQL 注入防御**: MyBatis-Plus 参数化查询
- **XSS 防御**: 前端 Naive UI 自带 XSS 防护
- **Audit Trail**: 所有 CRUD 操作记录到 operation_log
- **敏感词过滤**: KeywordBlockingService 在聊天中过滤敏感词
- **CORS**: 跨域配置（前端开发时允许 localhost:3000）
- **速率限制**: 内置速率限制（连接数、请求频率）

---

## 11. 监控与可观测性

### 11.1 自定义端点

| 端点 | 路径 | 说明 |
|------|------|------|
| 健康检查 | GET /api/v1/system/health | 含数据库 + 磁盘检查 |
| 系统信息 | GET /api/v1/system/info | JVM + 内存 + 线程 |
| 指标 | GET /api/v1/system/metrics | 应用指标 |
| Actuator 健康 | GET /api/v1/actuator/health | Spring Boot 标准 |
| Actuator 信息 | GET /api/v1/actuator/info | - |
| Actuator 指标 | GET /api/v1/actuator/metrics | - |
| Prometheus | GET /api/v1/actuator/prometheus | Prometheus 格式 |

### 11.2 业务指标

| 指标名 | 类型 | 说明 |
|--------|------|------|
| mindvault.api.calls | Counter | API 调用次数 |
| mindvault.api.errors | Counter | API 错误次数 |
| mindvault.llm.calls | Counter | LLM 调用次数 |
| mindvault.llm.duration | Timer | LLM 调用耗时 |
| mindvault.llm.tokens.input | Counter | 输入 Token 数 |
| mindvault.llm.tokens.output | Counter | 输出 Token 数 |
| mindvault.circuitbreaker.open | Gauge | 熔断器状态 |
| mindvault.backup.count | Gauge | 备份数量 |
| mindvault.connections.active | Gauge | 活跃连接数 |

### 11.3 追踪

- 每个请求生成 `X-Trace-Id` 头
- 通过 SLF4J MDC 传播 Trace ID
- 支持日志聚合（按 Trace ID 串联请求链路）
