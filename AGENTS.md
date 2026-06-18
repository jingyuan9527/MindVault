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
| Backend tests (134 total) | `cd backend && mvn test` |
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

## Docker Deployment
- Three containers: `db` (pgvector), `backend` (JDK 21), `frontend` (Nginx)
- Docker Compose file: `docker/docker-compose.yml`
- Dockerfiles: `Dockerfile.backend` (multi-stage Maven build), `Dockerfile.frontend` (node build → nginx)
- Networks: `mindvault` bridge
- Health checks: DB (`pg_isready`), Backend (`GET /api/v1/system/health`)