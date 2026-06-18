<p align="center">
  <h1 align="center">MindVault</h1>
  <p align="center"><em>Your AI-Augmented Second Brain — A self-hosted personal knowledge base with LLM-powered intelligence</em></p>
</p>

---

## Overview

MindVault is a self-hosted personal knowledge base system enhanced with LLM AI capabilities. It combines knowledge management, intelligent retrieval (RAG), AI chat, spaced repetition review, and AI-powered content generation into a single integrated platform.

Whether you're collecting web bookmarks, processing PDF documents, or writing daily notes, MindVault helps you store, organize, retrieve, and review your knowledge — with an AI assistant that understands your entire knowledge base.

---

## Features

### Knowledge Base
- **CRUD + tagging** — Organize entries with nested tags
- **Hybrid search** — Full-text + vector (semantic) search via pgvector
- **URL parsing** — Auto-extract markdown from web pages (Jsoup)
- **PDF parsing** — Extract text content from PDF documents (PDFBox)
- **Import/Export** — JSON, Markdown, CSV formats

### AI Chat (RAG)
- **Context-aware chat** — AI answers grounded in your knowledge base
- **SSE streaming** — Real-time token-by-token response
- **Multi-model support** — OpenAI, DeepSeek, Alibaba Qwen, Ollama

### Spaced Repetition (SM-2)
- **Review scheduling** — Algorithm-based due item calculation
- **Flashcards** — Auto-generated from knowledge entries
- **Progress tracking** — Track retention over time

### AI Content Generation
- **Daily Review** — AI-generated daily study summaries
- **Writing Assistant** — Article generation from topics/prompts

### Observability & Management
- **Token usage tracking** — Monitor per-provider LLM costs
- **Operation audit log** — AOP-based automatic logging
- **Database backup/restore** — pg_dump with retention policy
- **Prometheus metrics** — API calls, LLM usage, circuit breaker state, connection pool
- **System endpoints** — Health check, JVM info, custom metrics

---

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| **Backend** | Java (Virtual Threads), Spring Boot | JDK 21, 3.2.5 |
| **ORM** | MyBatis-Plus | 3.5.7 |
| **Database** | PostgreSQL 16 + pgvector | 16 |
| **Migrations** | Manual SQL | — |
| **API Docs** | Knife4j (SpringDoc OpenAPI 3) | 4.5.0 |
| **Frontend** | Vue 3 (Composition API), Pinia, Vue Router 4 | ^3.4 |
| **Styling** | Tailwind CSS | ^3.4 |
| **Build** | Maven (backend), Vite (frontend) | — |
| **Container** | Docker Compose (3 containers) | — |

---

## Quick Start

### Prerequisites

- JDK 21 (backend dev)
- Node.js 20 (frontend dev)
- Docker & Docker Compose (full-stack deployment)

### Development

```bash
# Backend
cd backend && mvn clean package -DskipTests && java -jar target/*.jar

# Frontend (in another terminal)
cd frontend && npm install && npm run dev
```

Frontend dev server at `http://localhost:5173` (proxies `/api` to `:8080`).

### Production (Docker)

```bash
cd docker && docker compose up -d --build
```

Three containers are started:
| Container | Port | Purpose |
|-----------|------|---------|
| `db` | `:5432` | PostgreSQL 16 + pgvector |
| `backend` | `:8080` | Spring Boot API server (JDK 21) |
| `frontend` | `:3000` | Nginx serving SPA + proxying `/api/` to backend |

Access the application at **`http://localhost:3000`**.

---

## API Documentation

Once running, API docs are available at:

- **Knife4j UI**: `http://localhost:3000/api/doc.html`
- **Swagger UI**: `http://localhost:3000/api/swagger-ui/index.html`
- **OpenAPI JSON**: `http://localhost:3000/api/v3/api-docs`

---

## Project Structure

```
mind-vault/
├── backend/                          # Spring Boot backend (JDK 21)
│   ├── src/main/java/com/mindvault/
│   │   ├── agent/                    # LLM orchestration, failover, tools
│   │   ├── backup/                   # pg_dump backup/restore
│   │   ├── chat/                     # Chat sessions + SSE streaming
│   │   ├── common/                   # Exception handler, config, filter, metrics
│   │   │   ├── annotation/           # @OperationLog annotation
│   │   │   └── aspect/               # OperationLogAspect
│   │   ├── content/                  # URL (Jsoup) and PDF (PDFBox) parsing
│   │   ├── dailyreview/              # AI-generated daily review reports
│   │   ├── flashcard/                # Flashcard CRUD + auto-generation
│   │   ├── knowledge/                # Core knowledge CRUD, search, tags, export
│   │   ├── model/                    # LLM model config CRUD
│   │   ├── operationlog/             # Audit log
│   │   ├── review/                   # SM-2 spaced repetition
│   │   ├── tokenusage/               # Token usage tracking
│   │   └── writing/                  # AI writing assistant
│   ├── src/main/resources/
│   │   ├── application.yml           # Main config
│   │   ├── db/migration/             # Database initialization scripts
│   │   └── logback-spring.xml        # Logging config
│   └── pom.xml
├── frontend/                         # Vue 3 SPA
│   ├── src/
│   │   ├── api/                      # Axios API wrappers
│   │   ├── components/               # Reusable Vue components
│   │   ├── stores/                   # Pinia stores
│   │   ├── views/                    # Page views
│   │   └── router/                   # Vue Router config
│   ├── nginx.conf                    # Nginx production config
│   ├── vite.config.js
│   ├── tailwind.config.js
│   └── package.json
├── docker/
│   └── docker-compose.yml            # Full-stack orchestration
├── Dockerfile.backend                # Multi-stage backend build
├── Dockerfile.frontend               # Multi-stage frontend build
├── AGENTS.md                         # Development guide
└── ROADMAP.md                        # Feature progress
```

---

## Testing

```bash
# Backend (189 tests)
cd backend && mvn test

# Specific test class
cd backend && mvn test -Dtest=KnowledgeControllerTest

# Frontend (53 tests)
cd frontend && npx vitest run
```

---

## License

MIT