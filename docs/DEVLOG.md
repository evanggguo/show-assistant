# Dossier — Development Log

---

## 2026-04-10 | Backend Initialization (Phase 2 Complete)

### What Was Done

Completed the initial Spring Boot backend setup. The goal was to wire up the core flow described in TDD Section 4: **streaming chat + Tool Use dynamic suggestions** (Phase 2, no RAG).

#### Generated File Overview

| Category | Files | Notes |
|----------|-------|-------|
| Configuration | 4 | SecurityConfig, CorsConfig, AsyncConfig, AiConfig |
| Chat core | 5 | ChatController, ChatService, PromptAssembler, SseEventBuilder, SuggestFollowupsTool |
| Conversation module | 7 | Conversation/Message/DynamicSuggestion entities + Repository + Service + Controller |
| Owner module | 6 | Owner/PromptSuggestion entities + Repository + Service + Controller + DTO |
| Knowledge module | 5 | KnowledgeEntry, KnowledgeType, KnowledgeRepository, KnowledgeService, RagService |
| Document module | 3 | Document, DocumentStatus, DocumentRepository |
| AI service layer | 2 | EmbeddingService (interface), SpringAiEmbeddingService |
| Common module | 4 | ApiResponse, BusinessException, ResourceNotFoundException, GlobalExceptionHandler |
| DB migrations | 3 | V1 schema, V2 seed owner, V3 seed suggestions |
| Config files | 2 | application.yml, pom.xml |
| **Total** | **43 Java + config** | `mvn compile` BUILD SUCCESS (2.7s) |

---

### Key Design Implementations (mapped to TDD)

**TDD 4.3 — Streaming Chat SSE Protocol**
- Three event types: `token` (character-by-character), `done` (with messageId + suggestions), `error`
- Frontend uses `fetch + ReadableStream` instead of `EventSource` (reason: EventSource only supports GET)
- Spring Boot side uses `SseEmitter` (not WebFlux), backed by a dedicated thread pool (`sseTaskExecutor`)

**TDD 4.4 — Dynamic Suggestions via Tool Use**
- `suggest_followups` tool defined using Spring AI `@Tool` annotation
- `SuggestFollowupsTool` is **not a Spring Bean** — a new instance is created per request to avoid shared state
- Tool parameter name is `suggestions` (matches TDD JSON Schema design)
- Registered and subscribed via `chatClient.prompt().tools(suggestTool).stream().content()`

**TDD 4.2 — RAG (Phase 2 stub)**
- `RagService.retrieve()` returns `Collections.emptyList()` directly
- `PromptAssembler` omits the "reference material" section when RAG context is empty

**TDD 5 — Database**
- Flyway manages DDL; `ddl-auto: validate` (JPA validates only, no auto-migration)
- `knowledge_entries.embedding` is a `vector(1536)` column with an HNSW index created in V1 migration
- JPA entity marks `embedding` as `@Transient` for Phase 2 (replaced with full vector mapping in Phase 3)

---

### Problems Solved During Development

**1. Spring AI 1.0.0 artifact ID change**
- Old name: `spring-ai-anthropic-spring-boot-starter`
- Correct name: `spring-ai-starter-model-anthropic`
- Reason: Spring AI 1.0.0 unified its naming conventions

**2. Maven Central unreachable**
- Symptom: connection timeout, DNS resolved to `198.18.0.169` (reserved range, blocked)
- Fix: created `~/.m2/settings.xml` and configured Aliyun Maven mirror
  ```xml
  <mirror>
    <id>aliyun-central</id>
    <mirrorOf>central</mirrorOf>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
  ```

**3. `spring-boot-starter-security` not in local cache**
- The dependency was absent from the local Maven repository and the Aliyun mirror was not fully synced
- Fix: temporarily commented out the dependency for Phase 2; `SecurityConfig` replaced with an empty placeholder
- To restore: uncomment in `pom.xml` and restore the full `SecurityConfig` implementation once the network is stable

**4. `KnowledgeEntry.embedding` type mapping**
- `@JdbcTypeCode(SqlTypes.VECTOR)` + pgvector Hibernate integration is unnecessary in Phase 2
- If pgvector is not properly registered with Hibernate, the application may fail to start
- Fix: used `@Transient` in Phase 2 to bypass the issue; full vector mapping will replace it in Phase 3

**5. `SuggestFollowupsTool` parameter name mismatch**
- Initial generated code used `questions`, conflicting with the `suggestions` parameter name defined in TDD 4.4.2 JSON Schema
- Spring AI derives the JSON Schema from parameter names, so the model would receive an incorrect field name
- Fix: renamed parameter to `suggestions`

---

---

## 2026-04-10 | Frontend Initialization (Phase 2 Complete)

### What Was Done

| File | Description |
|------|-------------|
| `hooks/useChatStream.ts` | Core SSE hook — fetch + ReadableStream parses token/done/error events |
| `lib/types.ts` | All TypeScript types (Message, OwnerProfile, ChatRequest, etc.) |
| `lib/api.ts` | fetchOwnerProfile / fetchInitialSuggestions REST wrappers |
| `lib/storage.ts` | localStorage guest history management, max 20 entries, SSR-safe |
| `components/chat/ChatPage.tsx` | Main container: home-screen ↔ chat toggle, API data loading, error display |
| `components/chat/MessageList.tsx` | Message list, auto-scrolls to bottom on new messages |
| `components/chat/MessageBubble.tsx` | User: right-aligned grey bubble; assistant: left-aligned white bubble + Markdown rendering |
| `components/chat/StreamingBubble.tsx` | Streaming bubble with animate-pulse cursor at the end |
| `components/chat/SuggestionCards.tsx` | Horizontally scrollable suggestion cards; click to send |
| `components/chat/ChatInput.tsx` | Textarea: Enter to send / Shift+Enter for newline; disabled while streaming |
| `components/OwnerProfile.tsx` | Two modes: hero (full home-screen) and compact (inline header during chat) |

### Problems Solved During Development

| Problem | Cause | Fix |
|---------|-------|-----|
| SSE endpoint path wrong `/api/chat` | Generated code was missing the `/stream` suffix | Corrected to `/api/chat/stream` |
| `prose` class not applying | Tailwind v4 requires explicit registration of the typography plugin | Installed `@tailwindcss/typography`; added `@plugin` directive in globals.css |
| Google Fonts build failure | No network access to download the Geist font | Changed `layout.tsx` to use a system font stack (-apple-system, etc.) |

### How to Start

```bash
cd frontend && npm run dev   # Dev mode, accessible at http://localhost:3000/chat
```

The backend must be running concurrently on `http://localhost:8080`.

---

### Current Phase Status

| Phase | Status | Notes |
|-------|--------|-------|
| Phase 1 — Infrastructure | ✅ Done | Backend skeleton, Flyway migrations, config classes |
| Phase 2 — Streaming Chat Core | ✅ Done | Backend SSE + Tool Use; frontend useChatStream + Chat UI |
| Phase 3 — RAG Integration | 🔲 Pending | Replace RagService stub with real vector retrieval + pgvector |
| Phase 4 — Home-screen Polish | 🔲 Pending | SSO login, conversation history restore, theme switching |
| Phase 5 — Admin Console | 🔲 Pending | Knowledge base entry, file upload, Security auth |

---

### How to Start

```bash
# Start PostgreSQL (pgvector image)
docker run -d --name dossier-pg \
  -e POSTGRES_DB=dossier \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  pgvector/pgvector:pg16

# Start the backend
export ANTHROPIC_API_KEY=sk-ant-xxx
cd backend && mvn spring-boot:run
```

**Verify the core SSE endpoint:**
```bash
curl -N -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello, tell me about your background"}' \
  --no-buffer
```

Expected output:
```
event: token
data: {"text":"Hello"}

event: token
data: {"text":"! I"}

...

event: done
data: {"messageId":1,"suggestions":["What technologies does he specialize in?","Any notable projects?","How can I get in touch?"]}
```
