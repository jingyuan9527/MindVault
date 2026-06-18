<p align="center">
  <h1 align="center">MindVault · 知忆</h1>
  <p align="center"><em>你的 AI 增强第二大脑 — 自托管个人知识库，搭载大语言模型智能</em></p>
</p>

---

## 概述

MindVault（知忆）是一个自托管的个人知识库系统，深度融合 LLM AI 能力。它将知识管理、智能检索（RAG）、AI 对话、间隔重复复习和 AI 内容生成整合于同一个平台。

无论是收集网页书签、处理 PDF 文档，还是记录日常笔记，知忆都能帮你存储、组织、检索和复习知识——并配备一位理解你整个知识库的 AI 助手。

---

## 功能特性

### 知识库
- **增删改查 + 标签** — 使用嵌套标签归档条目
- **混合搜索** — 全文检索 + 向量语义搜索（pgvector）
- **URL 解析** — 自动从网页提取 Markdown 内容（Jsoup）
- **PDF 解析** — 从 PDF 文档提取文本（PDFBox）
- **导入/导出** — 支持 JSON、Markdown、CSV 格式

### AI 对话（RAG）
- **上下文感知聊天** — AI 基于你的知识库回答问题
- **SSE 流式输出** — 逐 token 实时响应
- **多模型支持** — OpenAI、DeepSeek、阿里千问、Ollama

### 间隔重复（SM-2）
- **复习调度** — 基于算法的到期项计算
- **知识卡片** — 从知识条目自动生成
- **进度追踪** — 追踪长期记忆保持率

### AI 内容生成
- **每日复盘** — AI 生成的每日学习总结报告
- **写作助手** — 根据主题/提示生成文章

### 可观测性与管理
- **Token 用量追踪** — 监控各供应商的 LLM 费用
- **操作审计日志** — AOP 自动化日志记录
- **数据库备份/恢复** — pg_dump 备份 + 保留策略
- **Prometheus 指标** — API 调用、LLM 用量、熔断器状态、连接池
- **系统端点** — 健康检查、JVM 信息、自定义指标

---

## 技术栈

| 层 | 技术 | 版本 |
|-------|-----------|---------|
| **后端** | Java（虚拟线程）, Spring Boot | JDK 21, 3.2.5 |
| **ORM** | MyBatis-Plus | 3.5.7 |
| **数据库** | PostgreSQL 16 + pgvector | 16 |
| **迁移** | 手动 SQL | — |
| **API 文档** | Knife4j（SpringDoc OpenAPI 3） | 4.5.0 |
| **前端** | Vue 3（组合式 API）, Pinia, Vue Router 4 | ^3.4 |
| **样式** | Tailwind CSS | ^3.4 |
| **构建** | Maven（后端）, Vite（前端） | — |
| **容器化** | Docker Compose（3 容器） | — |

---

## 快速开始

### 环境要求

- JDK 21（后端开发）
- Node.js 20（前端开发）
- Docker & Docker Compose（全栈部署）

### 开发模式

```bash
# 后端
cd backend && mvn clean package -DskipTests && java -jar target/*.jar

# 前端（另开终端）
cd frontend && npm install && npm run dev
```

前端开发服务器运行在 `http://localhost:5173`（自动代理 `/api` 到 `:8080`）。

### 生产部署（Docker）

```bash
cd docker && docker compose up -d --build
```

启动三个容器：
| 容器 | 端口 | 用途 |
|-----------|------|---------|
| `db` | `:5432` | PostgreSQL 16 + pgvector |
| `backend` | `:8080` | Spring Boot API 服务器（JDK 21） |
| `frontend` | `:3000` | Nginx 提供 SPA + 代理 `/api/` 到后端 |

访问 **`http://localhost:3000`** 使用应用。

---

## API 文档

启动后可通过以下地址查看 API 文档：

- **Knife4j UI**: `http://localhost:3000/api/doc.html`
- **Swagger UI**: `http://localhost:3000/api/swagger-ui/index.html`
- **OpenAPI JSON**: `http://localhost:3000/api/v3/api-docs`

---

## 项目结构

```
mind-vault/
├── backend/                          # Spring Boot 后端（JDK 21）
│   ├── src/main/java/com/mindvault/
│   │   ├── agent/                    # LLM 编排、故障转移、工具调用
│   │   ├── backup/                   # pg_dump 备份/恢复
│   │   ├── chat/                     # 聊天会话 + SSE 流式
│   │   ├── common/                   # 异常处理、配置、过滤器、指标
│   │   │   ├── annotation/           # @OperationLog 注解
│   │   │   └── aspect/               # OperationLogAspect 切面
│   │   ├── content/                  # URL（Jsoup）和 PDF（PDFBox）解析
│   │   ├── dailyreview/              # AI 生成的每日复盘报告
│   │   ├── flashcard/                # 知识卡片增删改查 + 自动生成
│   │   ├── knowledge/                # 核心知识 CRUD、搜索、标签、导出
│   │   ├── model/                    # LLM 模型配置 CRUD
│   │   ├── operationlog/             # 审计日志
│   │   ├── review/                   # SM-2 间隔重复
│   │   ├── tokenusage/               # Token 用量追踪
│   │   └── writing/                  # AI 写作助手
│   ├── src/main/resources/
│   │   ├── application.yml           # 主配置文件
│   │   ├── db/migration/             # 数据库初始化脚本
│   │   └── logback-spring.xml        # 日志配置
│   └── pom.xml
├── frontend/                         # Vue 3 单页应用
│   ├── src/
│   │   ├── api/                      # Axios API 封装
│   │   ├── components/               # 可复用 Vue 组件
│   │   ├── stores/                   # Pinia 状态管理
│   │   ├── views/                    # 页面视图
│   │   └── router/                   # Vue Router 配置
│   ├── nginx.conf                    # Nginx 生产配置
│   ├── vite.config.js
│   ├── tailwind.config.js
│   └── package.json
├── docker/
│   └── docker-compose.yml            # 全栈编排
├── Dockerfile.backend                # 多阶段后端构建
├── Dockerfile.frontend               # 多阶段前端构建
├── AGENTS.md                         # 开发指南
└── ROADMAP.md                        # 功能进度
```

---

## 测试

```bash
# 后端（189 个测试）
cd backend && mvn test

# 指定测试类
cd backend && mvn test -Dtest=KnowledgeControllerTest

# 前端（53 个测试）
cd frontend && npx vitest run
```

---

## 许可证

MIT