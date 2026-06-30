# MindVault — Agent Guide

## 三条铁律

1. **没有完成标准，不写代码** — 开工前必须有明确的 Done Criteria（测试用例 / 手工验收清单 / 肉眼确认的行为判定），有清晰的 done/not-done 分界线。
2. **先写验证，再写实现** — 优先 TDD：80%+ 业务逻辑通过设计分离做到可低成本自动化测试，确难测的场景以手工验收清单兜底，但不逃避可测性设计。
3. **一个 Commit 一个功能点，小到可独立回滚** — 不混入不同用户可见功能的代码，同一功能点内的内部重构属于该功能 commit 的一部分。

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
| Backend tests (326 total) | `cd backend && mvn test` |
| Single test class | `cd backend && mvn test -Dtest=KnowledgeControllerTest` |
| Frontend tests (120 total) | `cd frontend && npx vitest run` |
| Build backend jar | `cd backend && mvn clean package -DskipTests` |
| Build frontend | `cd frontend && npm run build` |
| Docker rebuild+deploy | `cd docker && docker compose up -d --build` |
| Login as admin (Docker) | `curl -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin"}'` |
| Create API token | `curl -X POST localhost:8080/api/v1/auth/tokens -H 'Authorization: Bearer <session>' -H 'Content-Type: application/json' -d '{"name":"ext","expireDays":365}'` |
| List API tokens | `curl localhost:8080/api/v1/auth/tokens -H 'Authorization: Bearer <token>'` |
| Delete API token | `curl -X DELETE localhost:8080/api/v1/auth/tokens/{id} -H 'Authorization: Bearer <token>'` |

## Architecture
- **Backend**: Spring Boot 3.4.3, Spring AI 2.0.0, JDK 21 (virtual threads), MyBatis-Plus (NOT JPA)
- **Frontend**: Vue 3 + Pinia + Vue Router 4, Tailwind CSS, Vite, `marked` for markdown rendering, TypeScript (gradual migration in progress)
- **DB**: PostgreSQL 16 with pgvector extension (Docker image `pgvector/pgvector:pg16`)
- **Proxy**: Nginx (frontend container) proxies `/api/` → `http://backend:8080`
- **Ports**: Backend `:8080`, Frontend `:3000`, DB `:5432`

## SOLID 设计决策（2024-06-26 烧烤共识）

以下为基于 SOLID 原则的 8 项重构共识，后续开发应遵循。标注 ✅ 为已完成，🔄 为部分完成，❌ 为未完成：

### 1. 包归属 ✅
| 当前 | 目标 | 状态 |
|------|------|:----:|
| `AutoProcessService` 在 `content` 包 | → `auto` 包 | ✅ |
| `KnowledgeAssociationService` 在 `knowledge` 包 | 保持 | ✅ |
| `KeywordBlockingService` 在 `chat` 包 | 保持 | ✅ |
| R1/R2/R3 + 调度器横跨 `auto/relation/scheduler` | → 统一到 `auto` 包，按子包 `r1/`, `r2/`, `r3/` 拆分 | ✅ |

### 2. 单一职责（SRP） ✅
- `KnowledgeService`（849 行）拆出 2 个新服务：
  - **`ImportExportService`** — JSON/Markdown/CSV 导入导出、格式转义、预览
  - **`TagService`** — 标签 CRUD、批量打标签、AI 批量打标签、标签合并
  - `KnowledgeService` 保留 CRUD + 搜索 + 编排

### 3. 依赖倒置 + 接口隔离（DIP + ISP） 🔄
- **所有 Service 抽取接口**：部分业务模块仍有 `XxxService` 接口，部分尚待抽取
- Controller 和 Service 间依赖接口而非具体类

### 4. 开闭原则（OCP）— 策略模式 ✅
- **搜索**：`SearchStrategy` 接口 → `HybridSearchStrategy`, `KeywordSearchStrategy`, `VectorSearchStrategy` 等实现，`SearchService` 按条件自动选择
- **导出**：`ExportFormatStrategy` 接口 → `JsonExportStrategy`, `MarkdownExportStrategy`, `CsvExportStrategy` 实现

### 5. 包结构统一 🔄
- 大部分模块已有 5 子包骨架（含空目录），部分实体和 dto 目录尚待填充

### 6. 强类型配置绑定 ✅
- ✅ 14 个模块全部抽取 `@ConfigurationProperties` 类，替换 `SystemConfigService` 字符串 key 模式
- ✅ 各模块配置类：`ReviewProperties`, `RelationProperties`, `FlashCardProperties`, `DailyReviewProperties`,
  `WritingProperties`, `AgentProperties`, `SearchToolProperties`, `ChatProperties`, `TokenUsageProperties`,
  `AssociationProperties`, `SearchProperties`, `ImportExportProperties`, `KnowledgeProperties`,
  `AutoThresholdProperties` (已有 `AutoProcessProperties`)
- ✅ 所有 Service 注入强类型 properties 类，仅 PromptRegistry 和 SystemConfig UI 保留 `SystemConfigService`
  （BackupServiceImpl 的 `task.backup.enabled` 为已知例外，待后续抽取）

### 7. 调度器重写 ✅
- ✅ 移除 `AutoProcessScheduler` 的 `volatile` 手动节流
- ✅ 改用 `@Scheduled(fixedRateString = "${mindvault.auto-process.round2.fixed-delay-ms:300000}")` + `AutoProcessProperties` 强类型配置
- ✅ 频率变更需重启生效（而非运行时热更新）

### 8. 循环依赖解耦 ✅
- `KnowledgeService` ↔ `AutoProcessService` 的 `@Lazy` 循环依赖通过拆分职责消除
- 新增 `AutoProcessOrchestrator`（在 `auto` 包）作为 R1 的单一协调入口

## Package Map (backend)
| Package | Responsibility |
|---------|---------------|
| `ai` | Spring AI wrapper: `AiModelFactory` (builds ChatModel/EmbeddingModel from ModelConfig), `PromptRegistry` (11 prompt templates via SystemConfig overrides) |
| `knowledge` | CRUD + tags + search + export/import + separate user/AI fields, `updateAiFields()`, `reprocessKnowledge()`, `displayTitle()` |
| `chat` | Chat sessions, messages, SSE streaming |
| `agent` | LLM calling with failover, tool execution |
| `auth` | User auth, login, API tokens, session manager, auth filter, admin initializer, user management (admin) |
| `auto` | Auto-processing pipeline (R1: aiTitle/aiTags/summary/embedding via @Async; R2: RelationService under auto/r2/; R3: AggregationService under auto/r3/; scheduler under auto/scheduler/) |
| `model` | Model config CRUD (OpenAI/DeepSeek/Alibaba/SiliconFlow/Ollama) |
| `content` | URL (Jsoup) and PDF (PDFBox) parsing → markdown |
| `review` | SM-2 spaced repetition scheduling |
| `flashcard` | Flashcard management |
| `dailyreview` | Daily review report generation (uses LLM) |
| `backup` | DB backup/restore via pg_dump |
| `tokenusage` | Token usage tracking |
| `operationlog` | Audit log |
| `common` | Global exception handler, filters, health endpoint, metrics, actuator, AOP operation log, custom type handlers |
| `annotation` | @OperationLog custom annotation |
| `aspect` | OperationLogAspect — auto-logging around @OperationLog methods |

## Key Conventions
- **Commit messages**: 使用中文，清晰描述本次改动的内容和目的。
- **Backend code comments**: 完整注释 — 类职责、方法逻辑、复杂分支、边界条件均需注释说明。
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
The pipeline processes each knowledge entry in three automated rounds. Scheduler lives in `auto/scheduler/`.

| Round | Stage | Service | What it does | Trigger |
|-------|-------|---------|--------------|---------|
| R1 | Title & Tags | `AutoProcessService` | Generates `aiTitle`, `aiTags`, summary, embedding vector via LLM; sets status → `TITLE_TAG_DONE` | `@Async` on create / `reprocessKnowledge()` |
| R2 | Relation Discovery | `RelationService` | Discovers associations using semantic similarity + tag overlap + LLM analysis; writes to `knowledge_relation`; sets status → `RELATION_DONE` | `AutoProcessScheduler` via `AutoProcessProperties` (default 5 min) |
| R3 | Aggregation | `AggregationService` | Rebuilds tag cloud, refreshes stats, completes processing; sets status → `COMPLETED` | `AutoProcessScheduler` via `AutoProcessProperties` (default 30 min) |

## Spring AI 2.0 Conventions
- **AiModelFactory** (`ai.client`): Builds `ChatModel` / `EmbeddingModel` dynamically from `ModelConfig` entities. OpenAI/DeepSeek/Alibaba/SiliconFlow → `OpenAiChatModel` (all OpenAI-compatible API), Ollama → `OllamaChatModel`. Supports overloaded `buildChatModel(ModelConfig, Double temperature)`. Empty/blank `baseUrl` strings are treated as null (falls back to provider default).
- **PromptRegistry** (`ai.prompt`): 11 prompt templates as enum constants. Each template resolved via `SystemConfig` overrides with `config.getPrompt(key, defaultTemplate)`.
- **LlmFailoverService**: Uses `ChatModel.call(new Prompt(new UserMessage(text), options))` instead of RestClient. Failover/retry/metrics preserved.
- **AgentService**: Uses `ChatModel.call(prompt)` / `ChatModel.stream(prompt).toStream().forEach(...)` directly (not via LlmFailoverService, needs multi-message arrays). Don't pass `ChatOptions` in the `Prompt` — set all options on the model builder via `AiModelFactory`.
- **Embedding**: Uses `EmbeddingModel.embed(String)` returning `float[]`. Convert to JSON string via `StringJoiner` + for-loop (no `Arrays.stream(float[])` in Java).
- **OllamaApi**: Use `OllamaApi.builder().baseUrl(url).build()` (not `new OllamaApi(url)`).
- **MyBatis-Plus 3.5.9**: `PaginationInnerInterceptor` removed entirely — implement manual pagination (count + LIMIT/OFFSET) in service layer instead. `selectPage` does NOT auto-execute count query. Remove empty `MybatisPlusInterceptor` bean if no other inner interceptors are needed.

## Testing Notes
- `ModelApiIntegrationTest` runs real API calls against `agnes-2.0-flash` (~29s for 5 tests). Skipped when env var absent.
- Frontend tests use `happy-dom` environment, `@vue/test-utils`, and `vitest`.
- Frontend test count: 145 tests across 21 files.
- Naive UI hooks (`useDialog`, `useMessage`, `useNotification`) are auto-imported by `unplugin-auto-import` in dev/build but NOT in vitest. The setup file (`__tests__/setup.js`) provides them on `globalThis` for views that use them without explicit import. Views that explicitly `import { useDialog } from 'naive-ui'` (e.g. FlashCardView) need a `vi.mock('naive-ui', ...)` in their test file.
- Test data SQL (10 knowledge entries) in init script — rerun manually if DB is reset.
- Auth is disabled in tests via `mindvault.auth.enabled=false` in `src/test/resources/application.properties`.

## Known Issues
- **pgvector 类型不匹配**: `knowledge.embedding` 列为 `vector` 类型，但 MyBatis 写入时用 `varchar` 表达式，导致 R1 嵌入向量存储失败（`ERROR: column "embedding" is of type vector but expression is of type character varying`）。影响：语义检索退化为关键字搜索，相似度分缺失。前端 `SearchResultItem` 对缺失 similarity 优雅降级（隐藏分数）。修复需为 `vector` 类型注册自定义 MyBatis TypeHandler。
- **前端健康检查误报**: `docker-compose.yml` 中 frontend healthcheck 用 `wget http://localhost:80/`，Alpine 里 `localhost` 解析到 IPv6 `::1`，而 nginx 只监听 IPv4 `0.0.0.0:80`，导致容器状态恒为 `unhealthy`。实际服务正常（HTTP 200）。修复：改用 `127.0.0.1`。

## Feature Progress

### Backend (API — 80 endpoints)
| Module | Backend | Controller Test | Service Test | Coverage |
|--------|---------|:-:|:-:|:-:|
| 知识库 Knowledge | CRUD + 搜索（3 种 + offset 分页）+ 导入导出 + 标签 + URL/PDF 解析 + AI自动处理(R1) | ✅ | ✅ | 54% |
| 聊天 Chat | 会话 + 消息 + SSE 流式 | ✅ | ✅ | 49% |
| 模型配置 Model Config | CRUD + 主模型 + 优先级 + 测试连接 + 拉取列表 | ✅ | ✅ | 36% |
| 复习 Review | SM-2 调度 + 到期查询 + 执行复习 | ✅ | ✅ | 99% |
| 闪卡 FlashCard | 列表查询 + AI 生成 | ✅ | ✅ | 25% |
| 每日回顾 DailyReview | LLM 报告生成 + 定时任务 | ✅ | ✅ | 33% |
| 写作助手 Writing | AI 文章生成 | ✅ | ✅ | 76% |
| 操作日志 OperationLog | 审计日志查询 | ✅ | ✅ | 31% |
| Token 用量 TokenUsage | 用量记录 + 日结汇总 | ✅ | ✅ | 89% |
| 数据备份 Backup | 备份 + 列表 + 下载 | ✅ | ✅ | 85% |
| 关联发现 Relation | R2 关联发现（语义 + 标签 + LLM）（在 auto/r2/ 下） | ❌¹ | ✅ | 54% |
| 自动处理 AutoProcess | R1 自动标注 + R2 关联 + R3 聚合统计 + 定时调度（在 auto/ 下） | ❌¹ | ✅ | 58% |
| Agent | LLM failover + 熔断 + 工具调用 | ❌² | ✅ | 57% |
| 内容解析 Content | Jsoup 网页 + PDFBox PDF | ❌¹ | ✅ | 68% |
| 系统监控 System | 健康/信息/指标端点 | ✅ | ✅ | — |
| 系统配置 SystemConfig | 模块导航 + 内联编辑 + 定时任务 + 默认值还原 + 审计溯源 | ✅ | ✅ | 34% |
| 用户管理 User | 列表 + 启用/禁用 | ✅ | ✅ | 36% |
| 认证 Auth | 登录 + Token 管理 + 密码修改 | ✅ | ✅ | — |
| **Total** | **18 模块 / 80 接口** | **14/18** | **17/18** | **53%** |

### Frontend (14 routes)
| Route | View | Responsive | Tests | Design Polish |
|-------|------|:-:|:-:|:-:|
| `/login` | 登录页面 — 品牌英雄式布局 + 渐变图标 + 背景光晕 | ✅ | ✅ | ✅ v0.10 |
| `/` | 知识库列表 + 语义检索（自动切换浏览/检索） + 右侧抽屉（预览+关联图遍历+返回栈） + 导入导出 + AI标题 + 合并标签 | ✅ | ✅ | ✅ v0.10 |
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
| `/settings` | 模型配置 + 嵌入模型 + Token 管理 | ✅ | ✅ | ✅ v0.10 |
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

### To Do — 知识库页面布局优化（2026-06-30 拷问定稿）

**痛点**: A 信息密度低（一屏看不到几条）+ B 发现效率差（关联笔记零露出、标签藏下拉）+ ~~检索缺失（前端只走 list 端点 keyword 过滤，后端 3 套语义检索未接入）~~（✅ todo #1+#2 已接入语义检索 + offset 分页）。

**布局骨架**: header（搜索+深度开关+密度+排序+新建）→ 标签 pill 横条 → 列表/卡片 feed → 右侧抽屉。

**关键决策**:
- 检索形态: 单一界面自动切换（空=浏览走 list 端点，有词=检索走 hybridSearch；深度开关走 rewrite+rerank）
- 检索结果: 密集列表行（标题+命中 snippet 高亮+相似度分+标签），与浏览态卡片/列表形态区分作为模式信号
- 密度: 切换器（紧凑列表默认 / 卡片），复用 `NoteListItem.vue` 骨架
- 发现: 右侧抽屉（预览+关联+轻操作），点关联=图遍历（返回栈回溯），列表同步滚动高亮当前笔记；长文编辑走 `NoteEditorModal`（从抽屉唤起，并存）
- 标签: header 下方 pill 横条（替代 n-select 下拉），浏览态筛选/检索态收紧搜索
- 分页: 浏览态"加载更多"追加（真分页）；检索态 offset 追加封顶 top 60 后提示精炼
- 新建: header"+新建"按钮 + N/Cmd+N 快捷键，砍 FAB
- 移动端: 分级降级（手机 header 只留搜索+新建图标，余下收溢出菜单，强制列表，抽屉全屏；平板≥768px 恢复）

**原子 todo（每条一个 commit，带完成标准）**:
1. [high] ✅ 后端: 检索接口加 offset（hybridSearch/searchWithRewrite/hydeSearch）— offset 用例证明第二 tier 不重复
2. [high] ✅ 前端: 接入语义检索（自动切换端点 + 检索结果行 snippet+相似度分）— 相似度分依赖 embedding 存储（pgvector 类型不匹配为已知遗留 bug，组件对缺失 similarity 优雅降级）
3. [medium] ✅ 前端: 密度切换（紧凑列表默认/卡片，复用 NoteListItem）
4. [high] ✅ 前端: 右侧抽屉（预览+关联+图遍历返回栈+列表高亮同步）— 新增 NoteDrawer.vue，点击笔记开抽屉而非编辑器，点关联=图遍历（getById 拉全文），返回栈回溯，列表选中高亮
5. [medium] ✅ 前端: 抽屉/modal 并存（完整编辑入口+保存回流）
6. [medium] ✅ 前端: header 重设计（条件渲染：检索态隐藏排序、深度开关仅有词时出现）
7. [medium] ✅ 前端: 标签 pill 横条（替代下拉，浏览筛选/检索收紧搜索）
8. [medium] ✅ 前端: 加载更多分页（浏览真分页追加/检索 offset 追加封顶 60）
9. [low] 前端: 新建入口+快捷键（砍 FAB，N/Cmd+N，空状态文案）
10. [low] 前端: 移动端响应式（分级降级，溢出菜单，强制列表，全屏抽屉）

## Docker Deployment
- Three containers: `db` (pgvector), `backend` (JDK 21), `frontend` (Nginx)
- Docker Compose file: `docker/docker-compose.yml`
- Dockerfiles: `Dockerfile.backend` (multi-stage Maven build), `Dockerfile.frontend` (node build → nginx)
- v0.4 migration: `docker/migration-v0.4.sql` (creates knowledge_relation, auto_process_log tables; adds new columns to knowledge)
- v0.5 migration: `docker/migration-v0.5.sql` (creates system_config table with ~120 default config items for prompts/crons/thresholds/defaults)
- v0.6 migration: `docker/migration-v0.6-operationlog.sql` (operation_log partition + archive; entity_id → VARCHAR(64))
- Networks: `mindvault` bridge
- Health checks: DB (`pg_isready`), Backend (`GET /api/v1/system/health`), Frontend (`wget http://localhost:80/`)
- Admin user auto-created on first boot via env vars: `MINDVAULT_ADMIN_USERNAME` / `MINDVAULT_ADMIN_PASSWORD`
