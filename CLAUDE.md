# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Dossier is an AI Personal Portfolio Assistant that allows visitors to learn about an owner's skills and resume through chat. It supports multiple owners, using URL-based usernames for data isolation.

**Three Subsystems**:
- **Client Portal** `/{username}/chat`: No login required; visitors chat with the AI assistant to learn about the owner.
- **Owner Admin Console** `/admin`: Owners log in to manage their knowledge base, documents, personal info, custom AI instructions, and initial prompts; secured via JWT.
- **Super Admin Panel** `/admin-panel`: Create/delete owner accounts; secured via a static token (`X-Super-Admin-Token`), not exposed to the public.

## Common Commands

### Backend (Spring Boot)

```bash
cd backend

# Start (requires local PostgreSQL, port 8080)
./mvnw spring-boot:run

# Compile
./mvnw compile

# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=ChatServiceTest

# Run a specific test method
./mvnw test -Dtest=ChatServiceTest#testHandleStream
```

### Frontend (Next.js)

```bash
cd frontend

# Development server (port 3000)
npm run dev

# Build
npm run build

# Lint
npm run lint
```

### Docker (Full Environment)

```bash
# Start in Mock mode (default, no API keys required)
docker compose up -d

# Using Google Gemini (default provider, recommended)
AI_MOCK=false GOOGLE_AI_API_KEY=<key> docker compose up -d

# Using Claude API
AI_PROVIDER=claude AI_MOCK=false ANTHROPIC_API_KEY=<key> docker compose up -d

# Using local Ollama (requires uncommenting the ollama service in docker-compose.yml)
AI_PROVIDER=ollama docker compose up -d

# View backend logs
docker compose logs -f backend
```

For local development, start PostgreSQL via Docker first, then run the backend separately:
```bash
docker compose up postgres -d
cd backend && ./mvnw spring-boot:run
```

## Architecture

### Backend Package Structure

```
com.dossier.backend
├── ai/
│   ├── provider/                # AI Provider Abstraction Layer
│   │   ├── AiChatProvider       # Core Interface: streamChat() + generateSuggestions()
│   │   ├── GoogleChatProvider   # Google AI Studio (non-GCP)
│   │   ├── VertexAiChatProvider # Vertex AI Gemini (auto-activated on GCP)
│   │   ├── GcpEnvironmentDetector # Detects GCP environment via metadata server
│   │   ├── ClaudeChatProvider
│   │   ├── OllamaChatProvider
│   │   └── MockChatProvider
│   └── EmbeddingService         # Vectorization (Used in Phase 3)
├── chat/
│   ├── ChatController      # POST /api/owners/{username}/chat/stream
│   ├── ChatService         # Core streaming logic (@Async)
│   ├── PromptAssembler     # Builds System Prompt
│   ├── SseEventBuilder     # SSE event formatting
│   └── tool/
│       └── SuggestFollowupsTool  # Non-bean per-request tool to capture dynamic suggestions
├── client/
│   └── ClientController    # GET /api/owners/{username}/profile|suggestions (public)
├── admin/                  # Owner Admin interfaces (JWT Auth)
│   ├── auth/               # Login, JWT issuance
│   ├── owner/              # Personal info & custom AI instructions
│   ├── knowledge/          # Knowledge base CRUD
│   ├── document/           # File upload (max 50MB) & processing
│   └── suggestion/         # Initial prompt CRUD
├── superadmin/
│   ├── SuperAdminController  # GET/POST/DELETE /api/super-admin/owners
│   └── SuperAdminService
├── conversation/           # Conversation & Message persistence
├── knowledge/
│   ├── RagService          # Phase 2: stub; Phase 3: vector retrieval integration
│   └── KnowledgeService
├── owner/                  # Owner Entity & Repository
├── document/               # File storage (UPLOAD_DIR, Phase 3)
└── config/
    ├── AiConfig            # Conditional registration of AiChatProvider Beans
    ├── AsyncConfig         # sseTaskExecutor thread pool
    ├── CorsConfig
    ├── SecurityConfig
    ├── JwtConfig / JwtAuthenticationFilter  # JWT authentication
    ├── DataInitializer     # Seeds admin account from env vars on Docker startup
    └── VectorType          # Custom Hibernate type for pgvector
├── common/
│   ├── exception/          # BusinessException, ResourceNotFoundException, GlobalExceptionHandler
│   └── response/           # ApiResponse (Unified response wrapper)
```

### AI Provider Switching Logic

`AiConfig` conditionally registers a unique `AiChatProvider` Bean:

| `ai.provider` | `ai.mock` | Active Provider |
|---|---|---|
| `google` (default) | `true` | MockChatProvider |
| `google` | `false`, not on GCP | GoogleChatProvider (requires GOOGLE_AI_API_KEY) |
| `google` | `false`, on GCP | VertexAiChatProvider (ADC auto-auth, no key needed) |
| `claude` | `true` | MockChatProvider |
| `claude` | `false` | ClaudeChatProvider (requires ANTHROPIC_API_KEY) |
| `ollama` | Any | OllamaChatProvider (Always live) |

**GCP Auto-Detection**: `GcpEnvironmentDetector` requests the GCP metadata server (`169.254.169.254`) at startup. If reachable, it activates `VertexAiChatProvider`, using the `GOOGLE_CLOUD_PROJECT` env var for the project ID.

**AI_MOCK Defaults**: `application.yml` defaults to `AI_MOCK=false`, but `docker-compose.yml` overrides this to `AI_MOCK=true` (Docker runs in Mock mode by default).

**Deployment Note**: The default provider is `google`. **No need** to install or start Ollama unless specifically using `AI_PROVIDER=ollama`.

### Core Streaming Chat Flow

`ChatService.handleStream()` (annotated with `@Async("sseTaskExecutor")`) executes in order:
1. Create or reuse conversation (`conversationId=null` indicates guest mode).
2. Save user message to DB.
3. `RagService.retrieve()` — Returns empty list in Phase 2.
4. Build Spring AI message list (SystemMessage + History + Current Message).
5. `PromptAssembler.assemble()`: Dynamically builds System Prompt containing Owner info and RAG context.
6. Create per-request `SuggestFollowupsTool` instance (not a Spring Bean to avoid state sharing).
7. `AiChatProvider.streamChat()`: Subscribe to the stream.
8. Post-stream: Check `SuggestFollowupsTool.getCapturedSuggestions()`; if empty (small models lacking Function Calling), use `generateSuggestions()` fallback.
9. Save assistant message + suggestions, push `done` SSE event.

### Frontend Architecture

**Routing Structure**:
- `app/[ownerUsername]/chat/page.tsx` → Client chat page (dynamic route, isolated by username).
- `app/admin/` → Owner management (login / profile / knowledge / documents); `admin/layout.tsx` handles JWT guard.
- `app/admin-panel/page.tsx` → Super Admin panel (static token validation, manage owner accounts).

**Core Hook**: `hooks/useChatStream.ts`
- Uses `fetch + ReadableStream` instead of `EventSource` (Reason: `EventSource` only supports GET; cannot send JSON bodies).
- Three SSE event types: `token` (append text), `done` (finish and save), `error`.
- **Guest Mode**: Chat history stored in `localStorage` (persisted via `lib/storage.ts`, isolated per owner, 20-message limit); each request sends the full `history` array.

**API & Utils Layer**:
- `lib/api.ts`: Public client interfaces (owner profile, initial prompts).
- `lib/admin-api.ts`: Owner admin interfaces (automatic JWT Bearer Token injection).
- `lib/error-utils.ts`: Maps technical error codes to user-friendly messages.
- SSE streaming is handled directly within `useChatStream` to avoid Next.js API Route buffering.

**Next.js Build**: Uses `output: 'standalone'` for smaller Docker images. `NEXT_PUBLIC_API_URL` and `BACKEND_INTERNAL_URL` are injected as build args during `docker compose build`.

**Nginx**: Proxies `/api/` requests directly to the backend (with `proxy_buffering off` for SSE), while other traffic goes to Next.js.

### Database

- PostgreSQL 16 + pgvector; migrations managed via Flyway (`backend/src/main/resources/db/migration/`).
- `V1`: Tables; `V2`: Owner seeds; `V3`: Prompt seeds; `V4`: Embedding dimensions; `V5`: Auth fields; `V6`: Custom AI instructions.
- `dynamic_suggestions` table: Stores per-message follow-up suggestions.
- `prompt_suggestions` table: Stores owner-defined initial prompts for the landing screen.

### Docker Seed Accounts

`DataInitializer` creates a default admin account on container startup using env vars:
- `ADMIN_USERNAME` (Default: `admin`)
- `ADMIN_PASSWORD_HASH` (Bcrypt hash pre-configured in `docker-compose.yml`)

Super Admin uses two tokens passed via `X-Super-Admin-Token` (No defaults, must be provided in env):
- `SUPER_ADMIN_PASSWORD`: Read-only/Creation access (list + create).
- `SUPER_ADMIN_FULL_ACCESS_PASSWORD`: Full access (list + create + delete).

The frontend calls `/api/super-admin/capabilities` to determine if the "Delete" button should be displayed based on the current token.

### Phase Status

`Phase 2`/`Phase 3` markers in comments indicate current progress:
- **Phase 2 (Current)**: SSE streaming chat is functional; RAG is a stub (empty list).
- **Phase 3 (Planned)**: Integration of real vector retrieval via `EmbeddingService` and `KnowledgeRepository`.