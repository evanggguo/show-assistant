# Dossier — Deployment Guide

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Quick Deployment (Docker Compose)](#2-quick-deployment-docker-compose)
3. [Local Development Setup](#3-local-development-setup)
4. [Environment Variables](#4-environment-variables)
5. [Health Checks](#5-health-checks)
6. [Troubleshooting](#6-troubleshooting)

---

## 1. Prerequisites

| Tool | Minimum Version | Notes |
|------|-----------------|-------|
| Docker | 24.0+ | Container runtime |
| Docker Compose | 2.20+ | Bundled with Docker Desktop |
| Git | Any | For cloning the repository |

> Local development also requires: JDK 21, Maven 3.9+, Node.js 20+

---

## 2. Quick Deployment (Docker Compose)

### 2.1 Clone the Repository

```bash
git clone https://github.com/evanggguo/dossier.git
cd dossier
```

### 2.2 Create the Environment File

```bash
cp .env.example .env
# Edit .env and fill in real values
```

See [Section 4](#4-environment-variables) for the `.env` file reference.

### 2.3 Start All Services

```bash
docker compose up -d
```

The first run will build all images automatically — expect 3–5 minutes depending on network speed.

### 2.4 Verify Startup

```bash
docker compose ps
```

Once all three services show `healthy` or `running`, open:

- Frontend: <http://localhost:3000/chat>
- Backend health endpoint: <http://localhost:8080/actuator/health>

### 2.5 Stop Services

```bash
docker compose down          # Keep data volumes
docker compose down -v       # Also delete volumes (wipes the database)
```

### 2.6 Update Deployment

```bash
git pull
docker compose up -d --build
```

---

## 3. Local Development Setup

For local development, each service can be started independently with hot-reload.

### 3.1 Start PostgreSQL

```bash
docker run -d --name dossier-pg \
  -e POSTGRES_DB=dossier \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  pgvector/pgvector:pg16
```

### 3.2 Start the Backend

```bash
export ANTHROPIC_API_KEY=sk-ant-xxxx
cd backend
mvn spring-boot:run
```

The backend listens on `http://localhost:8080` by default.

### 3.3 Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend listens on `http://localhost:3000` by default.

---

## 4. Local Model (Ollama)

### 4.1 Install Ollama

```bash
# macOS / Linux
curl -fsSL https://ollama.com/install.sh | sh

# Windows: download the installer from https://ollama.com/download
```

### 4.2 Pull a Recommended Model

```bash
# Bilingual (Chinese + English), 7B parameters, runs on 16 GB RAM
ollama pull qwen2.5:7b

# Low-memory environment (8 GB), English only
ollama pull llama3.2:3b
```

### 4.3 Verify Ollama is Running

```bash
curl http://localhost:11434/api/tags
# Returns a list of installed models when healthy
```

Ollama listens on `http://localhost:11434` by default. When deploying with Docker Compose, the backend reaches it via `http://host.docker.internal:11434` (Mac/Windows) or the host IP (Linux).

### 4.4 Switch to a Cloud Provider

To use Claude, update `.env` with the provider and API key, and disable mock mode:

```dotenv
AI_PROVIDER=claude
AI_MOCK=false
ANTHROPIC_API_KEY=sk-ant-xxxx
```

> **Note**: `AI_MOCK` only affects cloud providers (Claude). When using local Ollama, this setting is ignored — requests are always real.

---

## 5. Environment Variables

Create a `.env` file in the project root (`.env` is git-ignored):

```dotenv
# ── AI Provider ──────────────────────────────────────────────────────────────
# AI_PROVIDER: ollama (default, local model) | google | claude
AI_PROVIDER=ollama

# Ollama service URL (used when AI_PROVIDER=ollama; always makes real calls, AI_MOCK has no effect)
# Accessing host Ollama from inside Docker Compose:
#   Mac/Windows: http://host.docker.internal:11434
#   Linux:       http://172.17.0.1:11434  (or actual host IP)
AI_OLLAMA_BASE_URL=http://host.docker.internal:11434
AI_OLLAMA_MODEL=qwen2.5:7b

# Claude configuration (fill in when AI_PROVIDER=claude)
# AI_MOCK=true (default): skips the Claude API and returns mock data
# AI_MOCK=false: makes real API calls; requires ANTHROPIC_API_KEY
AI_MOCK=true
ANTHROPIC_API_KEY=

# ── Database (optional, has defaults) ────────────────────────────────────────
DB_PASSWORD=postgres

# ── Frontend (optional, has defaults) ────────────────────────────────────────
# URL the frontend uses to reach the backend
# - Local Docker Compose: defaults to http://localhost:8080
# - Production server: set to the public IP or domain, e.g. https://api.example.com
NEXT_PUBLIC_API_URL=http://localhost:8080
```

> **Note**: `NEXT_PUBLIC_API_URL` is inlined into the frontend bundle at build time. After changing it, rebuild the frontend image: `docker compose up -d --build frontend`.

---

## 6. Health Checks

| Service | Check | Expected Response |
|---------|-------|-------------------|
| PostgreSQL | `pg_isready -U postgres -d dossier` | `accepting connections` |
| Backend | `http://localhost:8080/actuator/health` | `{"status":"UP"}` |
| Frontend | `http://localhost:3000/chat` | HTTP 200 |

**Verify the core SSE endpoint:**

```bash
curl -N -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello, tell me about yourself"}' \
  --no-buffer
```

Expected output:

```
event: token
data: {"text":"Hello"}

event: token
data: {"text":"!"}

...

event: done
data: {"messageId":1,"suggestions":["What technologies does he specialize in?","Any notable projects?","How can I get in touch?"]}
```

---

## 7. Troubleshooting

### Q: Blank frontend page or API requests failing

Check that `NEXT_PUBLIC_API_URL` is correct. If the frontend and backend are on different machines, set it to the backend's actual accessible address and make sure CORS is configured to allow the frontend origin.

### Q: Backend fails to start with `FlywayException: Validate failed`

The database schema is out of sync with Flyway migration scripts — typically caused by manually altering the schema. Fix by wiping and rebuilding:

```bash
# Drops the database and recreates it (data will be lost)
docker compose down -v
docker compose up -d
```

### Q: `docker compose up` hangs at the backend build stage (Maven dependency timeout)

Maven dependency downloads may time out on slow networks. You can mount your local `.m2` cache at build time:

```bash
docker build --build-arg MAVEN_OPTS="-Dmaven.repo.remote=https://maven.aliyun.com/repository/public" ./backend
```

Or run `mvn dependency:go-offline` locally first to pre-populate the cache, then build the Docker image.

### Q: Frontend image build fails (Next.js standalone mode)

Ensure `frontend/next.config.ts` contains `output: 'standalone'`. Without it, the `.next/standalone` directory is not generated, causing `COPY --from=builder /app/.next/standalone ./` to fail.

### Q: How to redeploy after changing backend code

```bash
docker compose up -d --build backend
```

This rebuilds and restarts only the backend service without affecting the database or frontend.
