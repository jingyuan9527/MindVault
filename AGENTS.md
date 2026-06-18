# MindVault — Agent Guide

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
| Backend tests (188 total) | `cd backend && mvn test` |
| Single test class | `cd backend && mvn test -Dtest=KnowledgeControllerTest` |
| Frontend tests (53 total) | `cd frontend && npx vitest run` |
| Build backend jar | `cd backend && mvn clean package -DskipTests` |
| Build frontend | `cd frontend && npm run build` |
| Docker rebuild+deploy | `cd docker && docker compose up -d --build` |
| Restart backend with new jar | `docker compose exec backend rm -f /app/app.jar && docker cp ../backend/target/*.jar docker-backend-1:/app/app.jar && docker compose restart backend` |

## Architecture
- **Backend**: Spring Boot 3.2.5, JDK 21 (virtual threads), MyBatis-Plus (NOT JPA), Flyway migrations
- **Frontend**: Vue 3 + Pinia + Vue Router 4, Tailwind CSS, Vite, `marked` for markdown rendering
- **DB**: PostgreSQL 16 with pgvector extension (Docker image `pgvector/pgvector:pg16`)
- **Proxy**: Nginx (frontend container) proxies `/api/` → `http://backend:8080`
- **Ports**: Backend `:8080`, Frontend `:3000`, DB `:5432`

## Package Map (backend)
| Package | Responsibility |
|---------|---------------|
| `knowledge` | CRUD + tags + search + export/import |
| `chat` | Chat sessions, messages, SSE streaming |
| `agent` | LLM calling with failover, tool execution |
| `model` | Model config CRUD (OpenAI/DeepSeek/Alibaba/Ollama) |
| `content` | URL (Jsoup) and PDF (PDFBox) parsing → markdown |
| `review` | SM-2 spaced repetition scheduling |
| `flashcard` | Flashcard management |
| `dailyreview` | Daily review report generation (uses LLM) |
| `backup` | DB backup/restore via pg_dump |
| `tokenusage` | Token usage tracking |
| `operationlog` | Audit log |
| `common` | Global exception handler, filters, health endpoint |

## Key Conventions
- **Controller tests**: `@WebMvcTest` + `@MockBean` pattern, no real DB. Example: `KnowledgeControllerTest.java`
- **Integration tests**: Use `application-test.yml` with H2 in PostgreSQL mode. Requires `MINDVAULT_TEST_API_KEY` env var for model API tests (auto-skipped if missing).
- **Secrets**: `application-test.yml` and `application-local.yml` are gitignored. Template at `application-test.yml.example`.
- **MyBatis-Plus**: Entity fields use `@TableField`, mappers extend `BaseMapper`. Custom SQL via `@Select` on mapper interface.
- **JSONB columns**: Stored as String in Java, need `::jsonb` cast in raw SQL inserts. Future: add `@TableField(typeHandler = JacksonTypeHandler.class)`.
- **Flyway**: Production migrations in `db/migration/`. Do NOT modify existing migrations.
- **TIMESTAMPTZ** → `TIMESTAMP`: All timestamp columns use `TIMESTAMP` (no timezone) to match Java `LocalDateTime`.

## Testing Notes
- `ModelApiIntegrationTest` runs real API calls against `agnes-2.0-flash` (~29s for 5 tests). Skipped when env var absent.
- Frontend tests use `happy-dom` environment, `@vue/test-utils`, and `vitest`.
- Test data SQL (10 knowledge entries) in init script — rerun manually if DB is reset.

## Feature Progress

### Backend (API — 51 endpoints)
| Module | Backend | Controller Test | Service Test | Coverage |
|--------|---------|:-:|:-:|:-:|
| 知识库 Knowledge | CRUD + 搜索 + 导入导出 + 标签 + URL/PDF 解析 | ✅ | ✅ | 54% |
| 聊天 Chat | 会话 + 消息 + SSE 流式 | ✅ | ✅ | 49% |
| 模型配置 Model Config | CRUD + 主模型 + 测试连接 + 拉取列表 | ✅ | ✅ | 36% |
| 复习 Review | SM-2 调度 + 到期查询 | ✅ | ✅ | 99% |
| 闪卡 FlashCard | CRUD + 自动生成 | ✅ | ✅ | 25% |
| 每日回顾 DailyReview | LLM 报告生成 + 定时任务 | ✅ | ✅ | 33% |
| 写作助手 Writing | AI 文章生成 | ✅ | ✅ | 76% |
| 操作日志 OperationLog | 审计日志查询 | ✅ | ✅ | 31% |
| Token 用量 TokenUsage | 用量记录 + 日结汇总 | ✅ | ✅ | 89% |
| 数据备份 Backup | 备份 + 列表 + 下载 + 清理 | ✅ | ✅ | 85% |
| Agent | LLM failover + 熔断 + 工具调用 | ❌ | ✅ | 3% |
| 内容解析 Content | Jsoup 网页 + PDFBox PDF | ❌ | ✅ | 13% |
| **Total** | **12 模块 / 51 接口** | **10/12** | **12/12** | **43%** |

### Frontend (7 routes)
| Route | View | Responsive | Tests |
|-------|------|:-:|:-:|
| `/` | 知识库列表 + 搜索 + 导入导出 | ✅ | ✅ |
| `/review` | 复习计划 + 执行 SM-2 | ✅ | ❌ |
| `/flashcards` | 闪卡展示 | ✅ | ❌ |
| `/writing` | AI 写作 | ✅ | ❌ |
| `/daily-review` | 每日回顾报告 | ✅ | ❌ |
| `/operation-logs` | 操作日志 | ✅ | ❌ |
| `/settings` | 模型配置 + 导出 | ✅ | ❌ |

### API 文档
- Knife4j UI: `http://localhost:3000/api/doc.html`
- Swagger UI: `http://localhost:3000/api/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:3000/api/v3/api-docs`

### To Do
- [ ] Agent 集成测试（WireMock）— agent 3% → ~60%
- [ ] Content 集成测试（WireMock）— content 13% → ~70%
- [ ] Frontend 7 个 View 的 Vitest 测试（当前只有 3 个组件/View 有测试）
- [ ] `image` 图床模块（可选）
- [ ] `search` 搜索日志/热词（可选）

## Docker Deployment
- Three containers: `db` (pgvector), `backend` (JDK 21), `frontend` (Nginx)
- Docker Compose file: `docker/docker-compose.yml`
- Dockerfiles: `Dockerfile.backend` (multi-stage Maven build), `Dockerfile.frontend` (node build → nginx)
- Networks: `mindvault` bridge
- Health checks: DB (`pg_isready`), Backend (`GET /api/v1/system/health`)