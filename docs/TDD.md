# Dossier — Technical Design Document (TDD)

---

## 1. Document Overview

This document elaborates on the technical implementation plan based on the PRD. The focus is **Section 4 — Dynamic Suggestions and Streaming Chat**, which is the most core and complex part of the product and the primary milestone for the MVP phase.

---

## 2. Technology Choices

| Layer | Choice | Notes |
|-------|--------|-------|
| Frontend | Next.js 15 (App Router, TypeScript) | SSR + client component mix; native SSE support |
| Backend | Java 21 + Spring Boot 3 | Familiar stack; Spring AI natively supports Claude streaming + Tool Use |
| AI integration | Spring AI + multi-provider abstraction | Default: local Ollama; configurable to Claude / OpenAI |
| Vector storage | PostgreSQL + pgvector | Fewer components; sufficient for MVP; pgvector supports HNSW indexing |
| Relational storage | PostgreSQL (same database) | Conversations, users, and knowledge entries share one PG instance |
| File storage | Local mount (MinIO interface reserved) | Docker Volume mount; abstracted interface for future migration |
| Deployment | Docker Compose | One-command startup: next, spring-boot, postgres |

---

## 3. System Architecture

### 3.1 Module Breakdown

```
dossier/
├── frontend/          # Next.js, three sub-systems
│   ├── app/
│   │   ├── [ownerUsername]/chat/  # Client portal (dynamic route, multi-owner)
│   │   ├── admin/                 # Owner admin console (login/profile/knowledge/documents)
│   │   └── admin-panel/           # Super admin panel (owner account management)
│   ├── components/
│   │   ├── admin/     # Admin console components (SuggestionManager, KnowledgeTable, DocumentUploader, etc.)
│   │   └── ...        # Shared components (OwnerProfile, ErrorAlert, etc.)
│   ├── hooks/
│   │   ├── useChatStream.ts   # SSE streaming chat
│   │   └── useAdminAuth.ts    # Admin console JWT auth state
│   └── lib/
│       ├── api.ts             # Client public API
│       └── admin-api.ts       # Admin console API (auto JWT injection)
│
└── backend/           # Spring Boot
    ├── chat/          # Chat module (streaming + RAG + dynamic suggestions)
    ├── client/        # Client public endpoints (/api/owners/{username}/...)
    ├── knowledge/     # Knowledge base module (entry, retrieval)
    ├── conversation/  # Conversation persistence module
    ├── owner/         # Owner info module (includes OwnerContextHolder)
    ├── document/      # Document upload and processing module
    ├── admin/         # Owner admin endpoints (auth/owner/knowledge/document/suggestion)
    ├── superadmin/    # Super admin module (owner account CRUD, fixed token auth)
    └── ai/            # AI service abstraction layer
        ├── provider/  # Multi-model provider implementations
        │   ├── AiChatProvider        # Interface (streaming chat abstraction)
        │   ├── GoogleChatProvider    # Google Gemini (default, real implementation)
        │   ├── ClaudeChatProvider    # Anthropic Claude (real implementation)
        │   ├── OllamaChatProvider    # Local Ollama (optional, real implementation)
        │   └── MockChatProvider      # Mock fallback (dev/testing)
        └── EmbeddingService
```

### 3.2 Deployment Architecture

```
                              Browser
                                 │
          ┌──────────────────────┼──────────────────────┐
          ▼                      ▼                      ▼
   Next.js (3000)          Next.js (3000)         Next.js (3000)
   /{username}/chat         /admin                 /admin-panel
   (Client Portal)         (Owner Admin Console)  (Super Admin Panel)
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 ▼
                       Nginx (reverse proxy, :80)
                       ├── /api/* → Spring Boot
                       └── /*    → Next.js
                                 ▼
                       Spring Boot (8080)
                       ┌─────────────────┐
                       │   REST + SSE    │
                       └────────┬────────┘
                                ▼
                       PostgreSQL (5432)
                       ├── Relational tables (owners, conversations, messages, …)
                       └── pgvector extension (knowledge_entries.embedding)
```

---

## 4. Core Feature Design: Dynamic Suggestions and Streaming Chat

> This is the most critical part of the system. This section starts from the overall data flow and breaks down each sub-problem and design decision.

### 4.1 Overall Data Flow

```
User sends a message
     │
     ▼
[Frontend] POST /api/owners/{username}/chat/stream
  Payload: { conversationId?, message, history[] }
     │
     ▼
[Backend: ChatController]
  1. Load or create a conversation
  2. Save user message to the database
  3. Trigger RAG retrieval
  4. Assemble prompt (system prompt + retrieved context + conversation history)
  5. Call AI API (streaming + Tool Use)
  6. Push tokens to frontend via SSE
  7. After stream ends, capture Tool Use result (dynamic suggestions)
  8. Save assistant message + dynamic suggestions to the database
  9. Push SSE done event (with suggestions)
     │
     ▼
[Frontend: useChatStream hook]
  - Render tokens in real time to the full-width AI reply area
  - Receive done event and display dynamic suggestion cards
```

### 4.2 RAG Retrieval Pipeline

#### 4.2.1 Knowledge Embedding (on write)

```
Owner enters content (text / file)
     │
     ▼
AI extracts structured knowledge entries (ExtractionService)
Each KnowledgeEntry: { type, title, content }
     │
     ▼
EmbeddingService.embed(content)
  → Calls Claude / OpenAI Embedding API
  → Returns float[] vector (1536 or 1024 dimensions)
     │
     ▼
INSERT INTO knowledge_entries (content, embedding, type, …)
  Uses pgvector to store the vector column
```

#### 4.2.2 Semantic Retrieval (during chat)

```
User question q
     │
     ▼
EmbeddingService.embed(q) → queryVector
     │
     ▼
SELECT content, type, title
  FROM knowledge_entries
  ORDER BY embedding <=> queryVector   -- pgvector cosine distance
  LIMIT 8
     │
     ▼
Return top-K relevant knowledge snippets, inject into prompt
```

**Retrieval enhancement strategy** (iterable after MVP):
- MVP: pure vector similarity retrieval, `<=>` cosine distance
- Iteration 1: hybrid retrieval with PostgreSQL full-text search (`tsvector`); results fused with RRF
- Iteration 2: type-weighted scoring — skill entries rank higher for skill-related questions

### 4.3 Streaming Chat Protocol (SSE)

SSE (Server-Sent Events) is the sole real-time communication protocol between frontend and backend. SSE is chosen over WebSocket because the conversation is unidirectional push (server → client), and SSE is simpler to implement with native support in both Spring Boot and Next.js.

#### 4.3.1 SSE Event Type Definitions

The server pushes three event types to the client, all in `text/event-stream` format:

```
# 1. Text token (streaming character-by-character output)
event: token
data: {"text": "Hello"}

event: token
data: {"text": ", I"}

event: token
data: {"text": " can help you..."}

# 2. Stream end (with dynamic suggestions + message ID)
event: done
data: {
  "messageId": "msg_abc123",
  "suggestions": [
    "What tech stack does he specialize in?",
    "Are there any related project examples?",
    "How can I contact him?"
  ]
}

# 3. Error
event: error
data: {"code": "RATE_LIMITED", "message": "Please try again later"}
```

**Design notes**:
- `token` events are high-frequency (one per text fragment); `data` contains only incremental text, not history
- `done` is sent once after the full stream ends, carrying the complete structured suggestions
- After receiving `done`, the frontend closes the EventSource connection and displays suggestion cards
- `messageId` is generated by the backend; the frontend uses it for subsequent operations (e.g., linking context when a suggestion is clicked)

#### 4.3.2 Server-Side Processing (Spring Boot)

```java
// ChatController.java (pseudo-code)
@GetMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamChat(@RequestBody ChatRequest req) {
    SseEmitter emitter = new SseEmitter(180_000L); // 3-minute timeout

    executor.submit(() -> {
        // 1. RAG retrieval
        List<String> context = ragService.retrieve(req.getMessage());

        // 2. Assemble prompt
        Prompt prompt = promptBuilder.build(req, context);

        // 3. Call AI streaming API (with Tool Use)
        claudeClient.streamWithTools(prompt, new StreamHandler() {
            @Override
            public void onToken(String token) {
                emitter.send(SseEmitter.event()
                    .name("token")
                    .data("{\"text\": \"" + escape(token) + "\"}"));
            }

            @Override
            public void onToolUse(String toolName, JsonNode input) {
                if ("suggest_followups".equals(toolName)) {
                    // Capture dynamic suggestions, hold temporarily
                    suggestions = input.get("suggestions");
                }
            }

            @Override
            public void onComplete(String fullText) {
                // 4. Persist
                Message msg = conversationService.saveAssistantMessage(
                    req.getConversationId(), fullText, suggestions);

                // 5. Send done event
                emitter.send(SseEmitter.event()
                    .name("done")
                    .data(buildDonePayload(msg.getId(), suggestions)));

                emitter.complete();
            }
        });
    });

    return emitter;
}
```

#### 4.3.3 Client-Side Processing (Next.js)

```typescript
// hooks/useChatStream.ts (pseudo-code)
export function useChatStream() {
  const [streamingText, setStreamingText] = useState('');
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [isStreaming, setIsStreaming] = useState(false);

  const sendMessage = useCallback(async (message: string, conversationId?: string) => {
    setIsStreaming(true);
    setStreamingText('');
    setSuggestions([]);

    const response = await fetch('/api/chat/stream', {
      method: 'POST',
      body: JSON.stringify({ message, conversationId }),
    });

    const reader = response.body!.getReader();
    const decoder = new TextDecoder();

    // Manually parse the SSE stream (fetch + ReadableStream; better compatibility than EventSource)
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      const chunk = decoder.decode(value);
      for (const line of parseSSELines(chunk)) {
        if (line.event === 'token') {
          setStreamingText(prev => prev + line.data.text);
        } else if (line.event === 'done') {
          setSuggestions(line.data.suggestions);
          setIsStreaming(false);
          // Notify parent to convert streamingText into a final message
          onStreamComplete?.(line.data.messageId);
        } else if (line.event === 'error') {
          handleError(line.data);
          setIsStreaming(false);
        }
      }
    }
  }, []);

  return { sendMessage, streamingText, suggestions, isStreaming };
}
```

**Why `fetch + ReadableStream` instead of `EventSource`**:
- `EventSource` only supports GET requests and cannot carry a JSON body (conversation state, message content)
- The `fetch` approach gives full control over the request format while still reading the SSE event stream

### 4.4 Dynamic Suggestion Generation Strategy

#### 4.4.1 Approach Comparison

There are several ways to generate dynamic suggestions (2–4 follow-up recommendations per turn):

| Approach | Mechanism | Pros | Cons |
|----------|-----------|------|------|
| **A. Separate request after stream** | Send a dedicated AI request after the main stream ends | Simple to implement | Extra ~1s latency; two API calls |
| **B. In-stream delimiter** | Ask the model to append `---SUGGESTIONS---\n[...]` at the end of its reply | Single call | Model output is inconsistent; fragile parsing |
| **C. Tool Use (recommended)** | Define a `suggest_followups` tool; model is required to call it at the end of each reply | Structured and reliable; single call; low latency | Requires understanding the Claude Tool Use API |

**Choosing Approach C (Tool Use)**: Claude's Tool Use is fully supported in streaming mode, and the structured output from a tool call is far more reliable than parsing free-form text.

#### 4.4.2 Tool Use Detailed Design

**Tool definition** (sent to the AI as the `tools` parameter):

```json
{
  "name": "suggest_followups",
  "description": "After completing each response, generate 2–4 follow-up questions the user might find interesting. Suggestions should be based on the current conversation and naturally guide the user to learn more.",
  "input_schema": {
    "type": "object",
    "properties": {
      "suggestions": {
        "type": "array",
        "items": { "type": "string" },
        "minItems": 2,
        "maxItems": 4,
        "description": "List of follow-up suggestions, each under 20 words"
      }
    },
    "required": ["suggestions"]
  }
}
```

**Position of Tool Use in the streaming response**:

The Claude streaming API event sequence is as follows:

```
# 1. Normal text tokens (main reply)
{"type": "content_block_delta", "delta": {"type": "text_delta", "text": "..."}}

# 2. Text block ends; tool call block begins
{"type": "content_block_stop"}
{"type": "content_block_start", "content_block": {"type": "tool_use", "name": "suggest_followups"}}

# 3. Tool call parameters (incremental JSON; must be accumulated and joined)
{"type": "content_block_delta", "delta": {"type": "input_json_delta", "partial_json": "{\"suggestions\":"}}
{"type": "content_block_delta", "delta": {"type": "input_json_delta", "partial_json": "[\"What is his main tech stack?\","}}
...

# 4. Tool call ends
{"type": "content_block_stop"}
{"type": "message_stop"}
```

Spring AI's `StreamingChatClient` wraps these events; developers handle text deltas and tool calls through separate callbacks.

**`tool_choice` setting**:

```json
"tool_choice": {"type": "auto"}
```

This does not force every call to invoke the tool — the model calls it when appropriate (the system prompt explicitly requires it on every turn).

#### 4.4.3 System Prompt Design (Anti-Hallucination)

`PromptAssembler.assemble()` dynamically builds a system prompt with the following four sections:

**Section 1: Identity**
```
You are {ownerName}'s AI personal assistant.
{ownerName}'s bio: {tagline}   ← omitted when tagline is blank
```

**Section 2: Rules (always injected — core anti-hallucination constraint)**
```
## Rules (MUST follow strictly)
1. You may only answer visitors' questions based on the "{ownerName}'s Knowledge Base" content provided below.
2. You must not use any information outside the knowledge base, including your general knowledge or training data.
3. Do not speculate, supplement, or fabricate anything not explicitly stated in the knowledge base.
4. For general greetings or small talk (e.g., "hello", "thank you"), respond warmly without refusing.
5. If you cannot find the answer to a visitor's question in the knowledge base, politely explain this and suggest the visitor contact {ownerName} directly.
6. Reply in the same language the visitor uses.
```

**Section 3: Knowledge Base (two branches — with or without content)**

With RAG content:
```
## {ownerName}'s Knowledge Base
The following is reference material about {ownerName}. All specific answers must be strictly based on this content:

1. **{title}**
{content}

2. ...
```

Without RAG content (empty knowledge base fallback — prevents AI from answering freely):
```
## {ownerName}'s Knowledge Base
There is currently no relevant knowledge base content for this question.
Please inform the visitor that you cannot provide this information at this time, and kindly suggest they contact {ownerName} directly for more details.
```

**Section 4: Tool Call Instruction (injected only when the provider supports Function Calling)**
```
## Important Instruction
After completing each response, you MUST call the `suggest_followups` tool
and provide 2–3 follow-up questions related to the current topic to help visitors learn more.
```

**Anti-hallucination design highlights:**
- The Rules section is always injected regardless of RAG state
- When RAG is empty, the absence of content is explicitly stated rather than silently omitting the knowledge base section — preventing the AI from interpreting "no constraints" as "free to answer"
- Rule 4 (allow greeting responses) prevents over-constraining the AI into refusing all non-knowledge-base questions

### 4.5 Multi-Provider AI Abstraction

#### 4.5.1 Design Goals

PRD 6.3 requires the AI service layer to support runtime switching between Claude, OpenAI, and local models. Design principles:

- `ChatService` depends only on the `AiChatProvider` interface — it is unaware of the specific provider
- The provider is selected via the `ai.provider` config key in `application.yml`
- **Local model (Ollama) always makes real calls**; `ai.mock` has no effect on it; the mock switch only applies to cloud providers
- Swapping providers in the future only requires replacing the implementation class, not the calling code

#### 4.5.2 Interface Definition

```java
// ai/provider/AiChatProvider.java
public interface AiChatProvider {

    /**
     * Streaming chat — returns a token stream.
     * Contract: suggest_followups JSON is captured via SuggestFollowupsTool, not mixed into the stream.
     *
     * @param messages  Complete conversation history (including system prompt)
     * @param tools     Registered tool instances (Spring AI @Tool beans)
     * @return Flux<String> incremental token text stream
     */
    Flux<String> streamChat(List<Message> messages, Object... tools);

    /**
     * Provider identifier — used for logging and monitoring.
     */
    String providerName();
}
```

#### 4.5.3 Provider Implementation Overview

| Implementation | Status | Notes |
|----------------|--------|-------|
| `GoogleChatProvider` | ✅ Real (non-GCP default) | Calls Google AI Studio Gemini API; requires `GOOGLE_AI_API_KEY`; used when `ai.provider=google` and not on GCP |
| `VertexAiChatProvider` | ✅ Real (GCP auto) | Calls Vertex AI Gemini via ADC; auto-activated when `GcpEnvironmentDetector.isRunningOnGcp()` is true; no API key required |
| `ClaudeChatProvider` | ✅ Real | Calls Anthropic Claude API; requires `ANTHROPIC_API_KEY` |
| `OllamaChatProvider` | ✅ Real (optional) | Calls local Ollama HTTP API; ignores `ai.mock`; commented out in docker-compose by default |
| `MockChatProvider` | ✅ Mock fallback | Activated when a cloud provider (google/claude) has `ai.mock=true`; returns mock data; no API key needed |

#### 4.5.4 Configuration-Driven Selection

Switching rules:

| `ai.provider` | `ai.mock` | Environment | Active Bean |
|---------------|-----------|-------------|-------------|
| `google` (default) | `false` (default) | GCP (Cloud Run / GKE / GCE) | `VertexAiChatProvider` (ADC) |
| `google` (default) | `false` (default) | non-GCP | `GoogleChatProvider` (API key) |
| `google` | `true` | any | `MockChatProvider` |
| `claude` | `false` | any | `ClaudeChatProvider` |
| `claude` | `true` | any | `MockChatProvider` |
| `ollama` | any (ignored) | any | `OllamaChatProvider` |

```yaml
# application.yml
ai:
  provider: ${AI_PROVIDER:google}   # options: google (default) | claude | ollama
  mock: ${AI_MOCK:false}            # only affects cloud providers; ollama ignores this
```

Spring wires up a single `AiChatProvider` Bean at startup using `@ConditionalOnProperty`:

```java
// Google Gemini (default)
@Configuration
@ConditionalOnProperty(name = "ai.provider", havingValue = "google", matchIfMissing = true)
static class GoogleProviderConfig {
    @Bean @ConditionalOnProperty(name = "ai.mock", havingValue = "false", matchIfMissing = true)
    public AiChatProvider googleAiChatProvider(...) { ... }

    @Bean @ConditionalOnProperty(name = "ai.mock", havingValue = "true")
    public AiChatProvider mockAiChatProvider() { ... }
}

// Claude cloud
@Configuration
@ConditionalOnProperty(name = "ai.provider", havingValue = "claude")
static class ClaudeProviderConfig {
    @Bean @ConditionalOnProperty(name = "ai.mock", havingValue = "false")
    public AiChatProvider claudeAiChatProvider(...) { ... }

    @Bean @ConditionalOnProperty(name = "ai.mock", havingValue = "true", matchIfMissing = true)
    public AiChatProvider mockAiChatProvider() { ... }
}

// Local Ollama: always makes real calls; ignores ai.mock
@Bean
@ConditionalOnProperty(name = "ai.provider", havingValue = "ollama")
public AiChatProvider ollamaAiChatProvider(...) { ... }
```

#### 4.5.5 Ollama Local Model Notes (Optional)

- **How it runs**: standalone process exposing an HTTP API (`http://localhost:11434`)
- **Spring AI integration**: `spring-ai-starter-model-ollama` provides the `OllamaChatModel` Bean
- **Recommended models**:
  - Bilingual (Chinese + English): `qwen2.5:7b` (Alibaba Qwen, 7B params, runs on consumer GPU / 16 GB CPU RAM)
  - English-only or low memory: `llama3.2:3b`
- **Tool Use support**: Ollama 0.3+ supports OpenAI-compatible function calling; Spring AI's `@Tool` annotation works directly
- **Note**: Ollama is not the default provider; Google Gemini is used by default. Enable Ollama only when local inference or on-premise data processing is required.

#### 4.5.6 Docker Compose Local Model Support

The Ollama service in `docker-compose.yml` is **commented out by default** and should only be uncommented when explicitly using `AI_PROVIDER=ollama`:

```yaml
# ─── Ollama local model (optional; only needed when AI_PROVIDER=ollama) ────
# ollama:
#   image: ollama/ollama:latest
#   container_name: dossier-ollama
#   volumes:
#     - ollama_data:/root/.ollama
```

When enabled, configure the backend environment:

```yaml
AI_PROVIDER: ollama
AI_OLLAMA_BASE_URL: http://ollama:11434   # Docker network internal address
```

#### 4.5.7 GCP Vertex AI Auto-Activation

When `ai.provider=google` and `ai.mock=false`, `AiConfig.GoogleProviderConfig` activates `VertexAiChatProvider` instead of `GoogleChatProvider` if the backend detects it is running on GCP. Detection is handled by `GcpEnvironmentDetector` using two checks in order:

1. **Primary**: `GOOGLE_CLOUD_PROJECT` environment variable — set automatically by all GCP runtimes (Cloud Run, GKE, GCE, App Engine). Zero network cost.
2. **Secondary**: HTTP probe to `http://metadata.google.internal/computeMetadata/v1/` with `Metadata-Flavor: Google` header, 500ms timeout. Only runs if env var is absent. Swallows all exceptions.

The detection result is cached after the first call (static volatile field). `VertexAiGeminiChatModel` is injected via `ObjectProvider` so that a missing or unconfigured Vertex AI bean causes a graceful fallback to Google AI Studio rather than a startup failure.

Required environment variables on GCP:

| Variable | Default | Description |
|----------|---------|-------------|
| `GOOGLE_CLOUD_PROJECT` | _(auto-set by GCP)_ | GCP project ID; also triggers GCP detection |
| `VERTEX_AI_LOCATION` | `us-central1` | Vertex AI API region |
| `GOOGLE_AI_MODEL` | `gemini-2.5-flash-lite` | Gemini model name (shared with Google AI Studio path) |

No `GOOGLE_AI_API_KEY` is required on GCP — Vertex AI authenticates via ADC.

---

### 4.6 Session Management

#### 4.6.1 Guest Session (localStorage)

```typescript
// Guest session storage structure (localStorage key: "guest_conversation")
interface GuestConversation {
  id: string;                  // Temporary UUID generated on the frontend
  messages: LocalMessage[];
  lastSuggestions: string[];   // Most recent dynamic suggestions
  createdAt: string;
}
```

- Guest `conversationId` values are prefixed with `guest_` for identification
- Each message send includes the full local `history` (up to 20 entries); the backend is stateless
- The backend **does not persist** guest messages (or may persist optionally without user binding)

#### 4.6.2 Logged-in User Session (Server-Side Persistence)

- After login, `conversationId` is a real server-generated ID
- Before sending each message, the frontend **does not need** to include history; the backend loads the most recent N messages from the database
- When loading history, `lastSuggestions` (the dynamic suggestions linked to the last assistant message) are returned at the same time

---

## 5. Database Design

### 5.1 Key Table Structures

```sql
-- Owner information
CREATE TABLE owners (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    tagline     VARCHAR(200),
    avatar_url  TEXT,
    contact     JSONB,         -- { "email": "...", "wechat": "..." }
    config      JSONB,         -- reserved for future config
    created_at  TIMESTAMPTZ   DEFAULT NOW()
);

-- Client users (SSO)
CREATE TABLE client_users (
    id            BIGSERIAL PRIMARY KEY,
    sso_provider  VARCHAR(50)  NOT NULL,  -- 'google' | 'github'
    sso_id        VARCHAR(200) NOT NULL,
    nickname      VARCHAR(100),
    avatar_url    TEXT,
    created_at    TIMESTAMPTZ  DEFAULT NOW(),
    UNIQUE (sso_provider, sso_id)
);

-- Conversations
CREATE TABLE conversations (
    id          BIGSERIAL PRIMARY KEY,
    owner_id    BIGINT       REFERENCES owners(id),
    user_id     BIGINT       REFERENCES client_users(id),  -- NULL = guest
    source      VARCHAR(20)  NOT NULL,  -- 'client' | 'admin'
    created_at  TIMESTAMPTZ  DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  DEFAULT NOW()
);

-- Messages
CREATE TABLE messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT       NOT NULL REFERENCES conversations(id),
    role            VARCHAR(20)  NOT NULL,  -- 'user' | 'assistant'
    content         TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  DEFAULT NOW()
);

-- Dynamic suggestions (linked to assistant messages)
CREATE TABLE dynamic_suggestions (
    id          BIGSERIAL PRIMARY KEY,
    message_id  BIGINT  NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    text        TEXT    NOT NULL,
    sort_order  INT     NOT NULL DEFAULT 0
);

-- Initial suggestions (configured in admin console)
CREATE TABLE prompt_suggestions (
    id          BIGSERIAL PRIMARY KEY,
    owner_id    BIGINT       NOT NULL REFERENCES owners(id),
    text        TEXT         NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Knowledge entries (with vector)
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE knowledge_entries (
    id          BIGSERIAL PRIMARY KEY,
    owner_id    BIGINT        NOT NULL REFERENCES owners(id),
    type        VARCHAR(50)   NOT NULL,  -- 'skill' | 'experience' | 'project' | 'education' | 'service' | 'other'
    title       VARCHAR(200),
    content     TEXT          NOT NULL,
    embedding   vector(1024),            -- text-embedding-3-small = 1536, claude embedding = 1024
    source_doc  BIGINT        REFERENCES documents(id),
    created_at  TIMESTAMPTZ   DEFAULT NOW()
);

-- Vector retrieval index (HNSW, approximate nearest neighbor, faster queries)
CREATE INDEX idx_knowledge_embedding ON knowledge_entries
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- Uploaded documents
CREATE TABLE documents (
    id          BIGSERIAL PRIMARY KEY,
    owner_id    BIGINT        NOT NULL REFERENCES owners(id),
    filename    VARCHAR(255)  NOT NULL,
    file_type   VARCHAR(50)   NOT NULL,
    file_size   BIGINT,
    file_path   TEXT          NOT NULL,
    status      VARCHAR(20)   NOT NULL DEFAULT 'pending',  -- 'pending' | 'processing' | 'done' | 'failed'
    created_at  TIMESTAMPTZ   DEFAULT NOW()
);
```

---

## 6. API Design

### 6.1 Streaming Chat (Core Endpoint)

```
POST /api/owners/{username}/chat/stream
Content-Type: application/json
Accept: text/event-stream

Request Body:
{
  "conversationId": "123",       // Optional; omitted for guests
  "message": "What technologies does he specialize in?",
  "history": [                   // Guests only; logged-in users leave this empty
    { "role": "user", "content": "..." },
    { "role": "assistant", "content": "..." }
  ]
}

Response: text/event-stream (see event definitions in 4.3.1)
```

### 6.2 Conversation Endpoints

```
# Get conversation history (including the last set of dynamic suggestions)
GET /api/conversations/{conversationId}

Response:
{
  "id": 123,
  "messages": [
    { "id": 1, "role": "user", "content": "...", "createdAt": "..." },
    {
      "id": 2,
      "role": "assistant",
      "content": "...",
      "suggestions": ["Follow-up 1", "Follow-up 2"],   // Dynamic suggestions for this message
      "createdAt": "..."
    }
  ],
  "lastSuggestions": ["Follow-up 1", "Follow-up 2"]  // Suggestions from the last assistant message (shortcut)
}
```

### 6.3 Initial Suggestions Endpoint

```
# Get initial home-screen suggestions for the client portal
GET /api/suggestions/initial

Response:
{
  "suggestions": ["What are his main projects?", "What technologies does he specialize in?", "How can we collaborate?"]
}
```

### 6.4 Owner Profile Endpoint

```
GET /api/owners/{username}/profile

Response:
{
  "name": "John Smith",
  "tagline": "Full-Stack Developer & Independent Product Builder",
  "avatarUrl": "https://..."
}

GET /api/owners/{username}/suggestions    — initial suggestion list
```

---

## 7. Frontend Core Component Design

```
app/[ownerUsername]/chat/page.tsx      # Client portal main page (dynamic route)
  └── <ChatPage>
        ├── <OwnerProfile>             # Avatar, name, tagline (home screen display)
        ├── <MessageList>              # Message list
        │     ├── <MessageBubble>      # Single message (supports Markdown)
        │     │     └── <SuggestionCards>  # Dynamic suggestion cards below this message
        │     └── <StreamingBubble>   # Full-width reply area during streaming (live updates)
        ├── <InitialSuggestions>       # Initial suggestions shown on the home screen before any chat
        └── <ChatInput>                # Bottom input bar + send button
```

**State management**: no global state library (Zustand/Redux) needed; use React Context + `useChatStream` hook to manage conversation state.

---

## 8. MVP Minimum Viable Path

Minimum implementation of the core flow, in dependency order:

```
Phase 1: Static skeleton (1–2 days)
  ✓ Next.js project initialized; client portal page layout (Gemini-inspired)
  ✓ Spring Boot project initialized; database tables created (Flyway migration)

Phase 2: Streaming chat without RAG (1–2 days)
  ✓ Backend: /api/chat/stream endpoint; direct call to AI streaming API
  ✓ Backend: Tool Use integration; capture suggest_followups result
  ✓ Backend: SSE push of token + done events
  ✓ Frontend: useChatStream hook; real-time streaming text rendering
  ✓ Frontend: display dynamic suggestion cards after done event
  → Goal: working chat + suggestion cards; AI uses general knowledge (no knowledge base)

Phase 3: Knowledge base integration (2–3 days)
  ✓ Backend: pgvector setup; EmbeddingService wrapper
  ✓ Backend: manually insert 2–3 test knowledge entries (skip admin console entry)
  ✓ Backend: RAG retrieval; inject into system prompt
  → Goal: AI answers based on knowledge base content; suggestions are topic-relevant

Phase 4: Owner profile + initial suggestions (1 day)
  ✓ Initialize owner data in the database (hardcoded)
  ✓ /api/owner/profile endpoint
  ✓ /api/suggestions/initial endpoint
  ✓ Frontend home screen shows profile + initial suggestion cards
  → Goal: complete home-screen experience

Phase 5: Admin console basic entry (follow-up)
  ✓ Text entry to knowledge base
  ✓ Knowledge entry list management
```

**Phase 2 is the most critical validation milestone** — once it passes, the core technical risks are resolved.

---

## 9. Key Technical Risks and Mitigations

| Risk | Description | Mitigation |
|------|-------------|------------|
| Tool Use compatibility in streaming mode | Spring AI's wrapping of Claude streaming Tool Use may be incomplete | Validate first; if needed, use the Anthropic Java SDK or a hand-written HTTP client |
| SSE connection lost through Next.js middleware | Next.js App Router API routes have limited support for long-lived connections | Frontend requests the backend directly (with CORS config), bypassing the Next.js middleware layer |
| pgvector dimension mismatch with model | Different embedding models produce different vector dimensions; the table must be created with the correct size | Standardize on `text-embedding-3-small` (1536 dims) or confirm the Claude embedding dimension before creating the table |
| Guest history too long | Sending full history from a guest may exceed the token limit | Truncate to the most recent 10 turns (20 messages); discard the oldest beyond that |
