# Dossier

> AI-powered personal portfolio assistant — let potential clients learn about your skills, background, and work through a chat interface.

## Overview

Dossier is an AI business card system designed for freelancers and small teams. It consists of three web sub-systems:

- **Client Portal** `/{username}/chat`: A Gemini-style chat interface where visitors can talk to the AI without logging in to learn about the owner.
- **Admin Console** `/admin`: Owners log in to maintain their knowledge base (text/file entries), configure personal info, and customize the AI assistant's instructions and initial suggestions.
- **Super Admin Panel** `/admin-panel`: System-level operations for creating and deleting Owner accounts (authenticated via a fixed token; not exposed publicly).

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 21 + Spring Boot 3.4 + Spring AI 1.0 |
| Frontend | Next.js + React + TypeScript + Tailwind CSS |
| Database | PostgreSQL 16 + pgvector |
| AI | Multi-provider abstraction (Google Gemini / Claude / Mock) |
| Deployment | Docker Compose + Nginx |

## Quick Start

### Prerequisites

- Docker & Docker Compose

### One-command start (Mock mode)

```bash
docker compose up -d
```

Starts in **Mock mode** by default — no API key required. Service runs at `http://localhost:3000`.

### Use Google Gemini API (recommended)

```bash
AI_MOCK=false GOOGLE_AI_API_KEY=<your-key> docker compose up -d
```

Default model is `gemini-2.0-flash`; override with `GOOGLE_AI_MODEL`.

### Use Claude API

```bash
AI_PROVIDER=claude AI_MOCK=false ANTHROPIC_API_KEY=<your-key> docker compose up -d
```

### Use local Ollama model (optional)

```bash
AI_PROVIDER=ollama docker compose up -d
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_PASSWORD` | `postgres` | PostgreSQL password |
| `AI_PROVIDER` | `google` | AI provider: `google` / `claude` / `ollama` |
| `AI_MOCK` | `false` | Mock mode toggle for cloud providers (ignored when `AI_PROVIDER=ollama`) |
| `GOOGLE_AI_API_KEY` | `placeholder` | Google AI Studio API key (not required on GCP) |
| `GOOGLE_AI_MODEL` | `gemini-2.5-flash-lite` | Google / Vertex AI Gemini model name |
| `ANTHROPIC_API_KEY` | `placeholder` | Claude API key |
| `AI_OLLAMA_BASE_URL` | `http://ollama:11434` | Ollama service URL (optional) |
| `AI_OLLAMA_MODEL` | `qwen2.5:1.5b` | Ollama model name (optional) |
| `VERTEX_AI_LOCATION` | `us-central1` | Vertex AI API region (GCP deployments only) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | CORS allowed origins |

## Project Structure

```
dossier/
├── backend/          # Spring Boot backend
│   └── src/main/java/com/dossier/backend/
│       ├── owner/        # Owner info & prompt suggestions
│       ├── conversation/ # Conversations, messages & dynamic suggestions
│       ├── knowledge/    # Knowledge base & RAG retrieval
│       ├── document/     # File management
│       └── common/       # Unified responses & exception handling
├── frontend/         # Next.js frontend
│   ├── app/          # Page routes
│   └── components/   # UI components (chat interface, etc.)
├── docs/
│   └── db-design.md  # Database schema design (ER diagram)
├── docker-compose.yml
└── nginx.conf        # Reverse proxy (SSE long-connection support)
```

## Main API Endpoints

**Client Portal (public, no authentication required)**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/owners/{username}/profile` | Get owner's public profile |
| `GET` | `/api/owners/{username}/suggestions` | Get initial home-screen suggestions |
| `POST` | `/api/owners/{username}/chat/stream` | SSE streaming chat |

**Admin Console (JWT authentication)**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/admin/auth/login` | Log in and get token |
| `GET/PUT` | `/api/admin/owner/profile` | Get/update profile and AI custom instructions |
| `PUT` | `/api/admin/owner/username` | Change username |
| `PUT` | `/api/admin/owner/password` | Change password |
| `GET/POST/PUT/DELETE` | `/api/admin/knowledge` | Knowledge base entry CRUD |
| `GET/POST/DELETE` | `/api/admin/documents` | Document upload, processing, and deletion |
| `GET/POST/PUT/DELETE` | `/api/admin/suggestions` | Initial suggestions CRUD |

**Super Admin Panel (`X-Super-Admin-Token` authentication)**

| Method | Path | Description |
|--------|------|-------------|
| `GET/POST` | `/api/super-admin/owners` | List/create owner accounts |
| `DELETE` | `/api/super-admin/owners/{id}` | Delete an owner account |

## AI Provider Configuration

The backend uses the Spring AI abstraction layer with runtime provider switching:

- **Mock mode** (`AI_MOCK=true`): Returns fixed mock data with no external dependencies — ideal for development and debugging.
- **Google Gemini** (default, non-GCP): Calls the Google AI Studio API; requires `GOOGLE_AI_API_KEY`. Use `AI_MOCK=true` to skip actual calls.
- **Vertex AI Gemini** (auto on GCP): When deployed to Cloud Run, GKE, or GCE, the backend automatically switches to Vertex AI and uses Application Default Credentials (ADC) — no `GOOGLE_AI_API_KEY` needed. Keep `AI_PROVIDER=google` (the default); switching is transparent.
- **Claude**: Calls the Anthropic API; requires `AI_PROVIDER=claude` and `ANTHROPIC_API_KEY`. Use `AI_MOCK=true` to skip actual calls.
- **Ollama** (optional): Calls a local model; requires `AI_PROVIDER=ollama`. `AI_MOCK` has no effect on this provider — requests are always real.

Small models that do not support Tool Use (e.g., some Ollama models) automatically fall back to text-parsing mode to generate dynamic suggestions.

## Database

Database migrations are managed by Flyway. The pgvector extension provides vector retrieval support (planned for Phase 3).

See [docs/db-design.md](docs/db-design.md) for the full schema.

## Local Development

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Requires a local PostgreSQL instance (or start one via Docker):

```bash
docker compose up postgres -d
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend defaults to `http://localhost:3000` and proxies API requests to `http://localhost:8080`.
