# MindVault — Agent Guide

## Workflow Preferences
This project follows an iterative delivery pattern:

1. **Goal-first**: Start with a clear target, no upfront planning docs.
2. **Ship fast, then polish**: First working version ASAP → user tests → iterate on feedback.
3. **Batch similar fixes**: When one issue is found, proactively fix all sibling occurrences.
4. **Verify tests**: Run full test suite (backend + frontend) after every change batch. Don't break green.
5. **Deploy on Docker**: After building, redeploy via `docker compose up -d --build` so changes can be verified in the real environment.
6. **User acceptance**: Ask the user to verify changes in the browser. Don't mark work done without confirmation.
7. **Sync docs**: Update AGENTS.md to reflect current state; verify todo list accuracy.
8. **Commit once**: One atomic commit per feature/batch, with a clear summary of changes, only after user confirms and docs are accurate.
9. **Check leftovers**: After commit, review pending todos so both sides know what's next.

## Quick Start
```bash
# Backend (JDK 21, Maven)
cd backend && mvn clean package -DskipTests && java -jar target/*.jar

# Frontend (Node 20)
cd frontend && npm install && npm run dev

# Full stack via Docker
cd docker && docker compose up -d --build
```

## Commands
| Task | Command |
|------|---------|
| Backend tests (295 total) | `cd backend && mvn test` |
| Single test class | `cd backend && mvn test -Dtest=KnowledgeControllerTest` |
| Frontend tests (85 total) | `cd frontend && npx vitest run` |
| Build backend jar | `cd backend && mvn clean package -DskipTests` |
| Build frontend | `cd frontend && npm run build` |
| Docker rebuild+deploy | `cd docker && docker compose up -d --build` |
| Login as admin (Docker) | `curl -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"mindvault123"}'` |
| Create API token | `curl -X POST localhost:8080/api/v1/auth/tokens -H 'Authorization: Bearer <session>' -H 'Content-Type: application/json' -d '{"name":"ext","expireDays":365}'` |
| List API tokens | `curl localhost:8080/api/v1/auth/tokens -H 'Authorization: Bearer <token>'` |
| Delete API token | `curl -X DELETE localhost:8080/api/v1/auth/tokens/{id} -H 'Authorization: Bearer <token>'` |

## Architecture
- **Backend**: Spring Boot 3.4.3, Spring AI 2.0.0, JDK 21 (virtual threads), MyBatis-Plus (NOT JPA)
- **Frontend**: Vue 3 + Pinia + Vue Router 4, Tailwind CSS, Vite, `marked` for markdown rendering, TypeScript (gradual migration in progress)
- **DB**: PostgreSQL 16 with pgvector extension (Docker image `pgvector/pgvector:pg16`)
- **Proxy**: Nginx (frontend container) proxies `/api/` → `http://backend:8080`
- **Ports**: Backend `:8080`, Frontend `:3000`, DB `:5432`

## Package Map (backend)
| Package | Responsibility |
|---------|---------------|
| `ai` | Spring AI wrapper: `AiModelFactory` (builds ChatModel/EmbeddingModel from ModelConfig), `PromptRegistry` (11 prompt templates via SystemConfig overrides) |
| `knowledge` | CRUD + tags + search + export/import + separate user/AI fields, `updateAiFields()`, `reprocessKnowledge()`, `displayTitle()` |
| `chat` | Chat sessions, messages, SSE streaming |
| `agent` | LLM calling with failover, tool execution |
| `auth` | User auth, login, API tokens, session manager, auth filter, admin initializer, user management (admin) |
| `auto` | Auto-processing pipeline (R1: aiTitle/aiTags/summary/embedding via @Async; R3: AggregationService for tag cloud + stats) |
| `model` | Model config CRUD (OpenAI/DeepSeek/Alibaba/Ollama) |
| `content` | URL (Jsoup) and PDF (PDFBox) parsing → markdown |
| `review` | SM-2 spaced repetition scheduling |
| `relation` | Round 2 association discovery (semantic + tag + LLM) |
| `flashcard` | Flashcard management |
| `dailyreview` | Daily review report generation (uses LLM) |
| `backup` | DB backup/restore via pg_dump |
| `tokenusage` | Token usage tracking |
| `operationlog` | Audit log |
| `scheduler` | Scheduled tasks: AutoProcessScheduler (R2 @5min, R3 @30min) |
| `common` | Global exception handler, filters, health endpoint, metrics, actuator, AOP operation log, custom type handlers |
| `annotation` | @OperationLog custom annotation |
| `aspect` | OperationLogAspect — auto-logging around @OperationLog methods |

## Key Conventions
- **Controller tests**: `@WebMvcTest` + `@MockBean` pattern, no real DB. Example: `KnowledgeControllerTest.java`
- **Integration tests**: Use `application-test.yml` with H2 in PostgreSQL mode. Requires `MINDVAULT_TEST_API_KEY` env var for model API tests (auto-skipped if missing).
- **Secrets**: `application-test.yml` and `application-local.yml` are gitignored. Template at `application-test.yml.example`.
- **MyBatis-Plus**: Entity fields use `@TableField`, mappers extend `BaseMapper`. Custom SQL via `@Select` on mapper interface.
- **JSONB columns**: Stored as String in Java, use `@TableField(typeHandler = JsonbStringTypeHandler.class)` on the field. Custom handler at `common.handler.JsonbStringTypeHandler` — wraps value in PGobject with type `jsonb` for write, reads as raw String from ResultSet.
- **TIMESTAMPTZ** → `TIMESTAMP`: All timestamp columns use `TIMESTAMP` (no timezone) to match Java `LocalDateTime`.
- **Lombok**: Version pinned to 1.18.38 via `<lombok.version>` property. Spring Boot 3.2.5's default 1.18.32 is incompatible with JDK 21.0.11+.
- **AI Auto-Processing fields**: Knowledge entity uses `title` (user title), `aiTitle` (AI-generated title), `tags` (AI tags), `userTags` (user tags), `autoProcessStatus` (PENDING/TITLE_TAG_DONE/RELATION_DONE/COMPLETED). Use `displayTitle()` to resolve `aiTitle || title`. Frontend shows AI title as primary, user title as secondary, merged tags from `ai_tags` + `user_tags`.
- **Knowledge relations**: Stored in `knowledge_relation` table (knowledge_id, related_id, relation_type, score, source), managed by `RelationService`.
- **Auto-process logs**: Stored in `auto_process_log` table (knowledge_id, round, status, result_summary, llm_tokens, ...), written by `AutoProcessService` and `RelationService`.

## AI Auto-Processing Pipeline (R1/R2/R3)
The pipeline processes each knowledge entry in three automated rounds:

| Round | Stage | Service | What it does | Trigger |
|-------|-------|---------|--------------|---------|
| R1 | Title & Tags | `AutoProcessService` | Generates `aiTitle`, `aiTags`, summary, embedding vector via LLM; sets status → `TITLE_TAG_DONE` | `@Async` on create / `reprocessKnowledge()` |
| R2 | Relation Discovery | `RelationService` | Discovers associations using semantic similarity + tag overlap + LLM analysis; writes to `knowledge_relation`; sets status → `RELATION_DONE` | `AutoProcessScheduler` every 5 minutes |
| R3 | Aggregation | `AggregationService` | Rebuilds tag cloud, refreshes stats, completes processing; sets status → `COMPLETED` | `AutoProcessScheduler` every 30 minutes |

## Spring AI 2.0 Conventions
- **AiModelFactory** (`ai.client`): Builds `ChatModel` / `EmbeddingModel` dynamically from `ModelConfig` entities. OpenAI/DeepSeek/Alibaba → `OpenAiChatModel` (all OpenAI-compatible API), Ollama → `OllamaChatModel`. Supports overloaded `buildChatModel(ModelConfig, Double temperature)`.
- **PromptRegistry** (`ai.prompt`): 11 prompt templates as enum constants. Each template resolved via `SystemConfig` overrides with `config.getPrompt(key, defaultTemplate)`.
- **LlmFailoverService**: Uses `ChatModel.call(new Prompt(new UserMessage(text), options))` instead of RestClient. Failover/retry/metrics preserved.
- **AgentService**: Uses `ChatModel.call(prompt)` / `ChatModel.stream(prompt).toStream().forEach(...)` directly (not via LlmFailoverService, needs multi-message arrays). Don't pass `ChatOptions` in the `Prompt` — set all options on the model builder via `AiModelFactory`.
- **Embedding**: Uses `EmbeddingModel.embed(String)` returning `float[]`. Convert to JSON string via `StringJoiner` + for-loop (no `Arrays.stream(float[])` in Java).
- **OllamaApi**: Use `OllamaApi.builder().baseUrl(url).build()` (not `new OllamaApi(url)`).
- **MyBatis-Plus 3.5.9**: `PaginationInnerInterceptor` removed — use auto-configuration instead.

## Testing Notes
- `ModelApiIntegrationTest` runs real API calls against `agnes-2.0-flash` (~29s for 5 tests). Skipped when env var absent.
- Frontend tests use `happy-dom` environment, `@vue/test-utils`, and `vitest`.
- Frontend test count: 85 tests across 19 files.
- Naive UI hooks (`useDialog`, `useMessage`, `useNotification`) are auto-imported by `unplugin-auto-import` in dev/build but NOT in vitest. The setup file (`__tests__/setup.js`) provides them on `globalThis` for views that use them without explicit import. Views that explicitly `import { useDialog } from 'naive-ui'` (e.g. FlashCardView) need a `vi.mock('naive-ui', ...)` in their test file.
- Test data SQL (10 knowledge entries) in init script — rerun manually if DB is reset.
- Auth is disabled in tests via `mindvault.auth.enabled=false` in `src/test/resources/application.properties`.

## Feature Progress

### Backend (API — 70 endpoints)
| Module | Backend | Controller Test | Service Test | Coverage |
|--------|---------|:-:|:-:|:-:|
| 知识库 Knowledge | CRUD + 搜索 + 导入导出 + 标签 + URL/PDF 解析 + AI自动处理(R1) | ✅ | ✅ | 54% |
| 聊天 Chat | 会话 + 消息 + SSE 流式 | ✅ | ✅ | 49% |
| 模型配置 Model Config | CRUD + 主模型 + 测试连接 + 拉取列表 | ✅ | ✅ | 36% |
| 复习 Review | SM-2 调度 + 到期查询 | ✅ | ✅ | 99% |
| 闪卡 FlashCard | CRUD + 自动生成 | ✅ | ✅ | 25% |
| 每日回顾 DailyReview | LLM 报告生成 + 定时任务 | ✅ | ✅ | 33% |
| 写作助手 Writing | AI 文章生成 | ✅ | ✅ | 76% |
| 操作日志 OperationLog | 审计日志查询 | ✅ | ✅ | 31% |
| Token 用量 TokenUsage | 用量记录 + 日结汇总 | ✅ | ✅ | 89% |
| 数据备份 Backup | 备份 + 列表 + 下载 + 清理 | ✅ | ✅ | 85% |
| 关联发现 Relation | R2 关联发现（语义 + 标签 + LLM） | ❌¹ | ✅ | 54% |
| 自动处理 AutoProcess | R1 自动标注 + R3 聚合统计 + 定时调度 | ❌¹ | ✅ | 58% |
| Agent | LLM failover + 熔断 + 工具调用 | ❌² | ✅ | 57% |
| 内容解析 Content | Jsoup 网页 + PDFBox PDF | ❌¹ | ✅ | 68% |
| 认证 Auth | 登录 + Token 管理 + 密码修改 | ✅ | ✅ | — |
| 系统配置 SystemConfig | 动态 KV 配置 + 定时任务管理 | ✅ | ✅ | 34% |
| 用户管理 User | 列表 + 启用/禁用 | ✅ | ✅ | 36% |
| **Total** | **18 模块 / 70 接口** | **13/18** | **17/18** | **53%** |

### Frontend (14 routes)
| Route | View | Responsive | Tests | Design Polish |
|-------|------|:-:|:-:|:-:|
| `/login` | 登录页面 — 品牌英雄式布局 + 渐变图标 + 背景光晕 | ✅ | ✅ | ✅ v0.10 |
| `/` | 知识库列表 + 搜索 + 导入导出 + AI标题 + 合并标签 | ✅ | ✅ | ✅ v0.10 |
| `/chat` | AI 对话 — 头部渐变 + 快捷建议 + 清空按钮 | ✅ | ✅ | ✅ v0.10 |
| `/review` | 间隔复习 — 进度条 + 圆点指示器 + 分色质量按钮 | ✅ | ✅ | ✅ v0.10 |
| `/flashcards` | 闪卡展示 — CSS 3D 翻转动画 + 难度色标 | ✅ | ✅ | ✅ v0.10 |
| `/writing` | AI 写作 — 纸质感文章卡片 + 琥珀色主题 | ✅ | ✅ | ✅ v0.10 |
| `/daily-review` | 每日复盘 — 分区报告卡片 + 左边框色标 | ✅ | ✅ | ✅ v0.10 |
| `/token-usage` | Token 用量 — 柱状图可视化 + 统计卡片 | ✅ | ✅ | ✅ v0.10 |
| `/operation-logs` | 操作日志 | ✅ | ✅ | ✅ v0.10 |
| `/backups` | 数据备份 — 时间线布局 | ✅ | ✅ | ✅ v0.10 |
| `/system` | 系统监控 — 图标统计卡 + 内存/磁盘进度条 | ✅ | ✅ | ✅ v0.10 |
| `/users` | 用户管理 — 首字母头像 + 角色/状态标签 | ✅ | ✅ | ✅ v0.10 |
| `/settings` | 模型配置 + Token 管理 | ✅ | ✅ | ✅ v0.10 |
| `/system-config` | 系统配置 + 定时任务管理 | ✅ | ✅ | ✅ v0.10 |

¹ 功能通过 `KnowledgeController` 或调度器暴露，无独立 Controller。测试覆盖在 `KnowledgeControllerTest` 中。  
² Agent 为纯服务层模块，无 REST 接口，暂无 Controller 测试。

### API 文档
- Knife4j UI: `http://localhost:3000/api/doc.html`
- Swagger UI: `http://localhost:3000/api/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:3000/api/v3/api-docs`

### Observability Endpoints
- Health: `GET /api/v1/system/health` (custom, with DB + disk checks)
- Info: `GET /api/v1/system/info` (JVM + memory + threads)
- Metrics: `GET /api/v1/system/metrics`
- Actuator: `GET /api/v1/actuator/health`, `/api/v1/actuator/info`, `/api/v1/actuator/metrics`, `/api/v1/actuator/prometheus`
- Prometheus metrics: `mindvault.api.calls`, `mindvault.api.errors`, `mindvault.llm.calls`, `mindvault.llm.duration`, `mindvault.llm.tokens.*`, `mindvault.circuitbreaker.open`, `mindvault.backup.count`, `mindvault.connections.active`
- Trace ID: Every request gets `X-Trace-Id` header, propagated via SLF4J MDC

## Frontend Design System (v0.10 — Multi-Theme Glassmorphism)
- **3 themes × 2 modes** = 6 color combinations, switchable via sidebar footer
  - 🌄 **Amber Earth** (default): Terracotta `#D4856A` + Olive `#8B9C68`, warm cream/dark brown background
  - 🌿 **Lavender Calm**: Warm purple `#A855F7` + Emerald `#34D399`; Soft UI evolution, neumorphism-glass fusion
  - 💎 **Violet Glass**: Violet `#8B5CF6` + Cyan `#06B6D4`; original dark glassmorphism
- **Theme mechanism**: `<html data-theme="X">` + `.light` class; all colors via CSS custom properties; no hardcoded colors in components
- **Naive UI overrides**: Dynamic per-theme+per-mode `themeColors()` function in `App.vue`
- **CSS architecture**: 6 variable blocks in `style.css` (3 themes × 2 modes); shared `.glass`, `.card`, `.tag-pill` components use `var(--color-*)`
- **Persistence**: Theme + mode saved to `localStorage`
- **Fonts**: Outfit (headings), Inter (body), JetBrains Mono (mono)
- **Key effects**: `backdrop-filter: blur(20px)`, animated background glows per-theme, `--gradient-brand` for brand elements

### To Do
- （无待办 — 全部完成）

## Docker Deployment
- Three containers: `db` (pgvector), `backend` (JDK 21), `frontend` (Nginx)
- Docker Compose file: `docker/docker-compose.yml`
- Dockerfiles: `Dockerfile.backend` (multi-stage Maven build), `Dockerfile.frontend` (node build → nginx)
- v0.4 migration: `docker/migration-v0.4.sql` (creates knowledge_relation, auto_process_log tables; adds new columns to knowledge)
- v0.5 migration: `docker/migration-v0.5.sql` (creates system_config table with ~120 default config items for prompts/crons/thresholds/defaults)
- Networks: `mindvault` bridge
- Health checks: DB (`pg_isready`), Backend (`GET /api/v1/system/health`), Frontend (`wget http://localhost:80/`)
- Admin user auto-created on first boot via env vars: `MINDVAULT_ADMIN_USERNAME` / `MINDVAULT_ADMIN_PASSWORD`
