# Dossier — Product Requirements Document (PRD)

## 1. Background and Goals

**Product positioning**: An AI-powered personal portfolio assistant for freelancers and small teams (owners), enabling potential clients to learn about the owner's skills, background, and portfolio at any time through a conversational interface.

**Core value**:
- For clients: a 24/7 intelligent consultation entry point that replaces passive static personal pages
- For owners: a zero-code knowledge base — the AI automatically distills key information

---

## 2. System Architecture Overview

The system consists of three independent web entry points sharing a single backend:

```
Client Portal  /{username}/chat
  └── Public-facing, accessible to potential clients; Gemini-style chat interface; supports multiple owners

Owner Admin Console  /admin
  └── Used only by the owner; log in to manage knowledge base, personal info, and AI configuration; JWT auth

Super Admin Panel  /admin-panel
  └── System-level operations; create/delete owner accounts; fixed token auth; not exposed publicly
```

---

## 3. Client Portal

### 3.1 Feature List

| Feature | Description | Priority |
|---------|-------------|----------|
| Chat interface | Multi-turn conversation with streaming output | P0 |
| SSO login / guest mode | Supports third-party SSO login or direct guest access | P0 |
| Dynamic suggestions | Display initial suggestions on the home screen; generate new follow-up suggestions after each AI reply based on conversation content | P0 |
| Owner profile display | Show avatar, name, and tagline at the top or side of the page | P0 |
| Conversation history restore | Logged-in users see previous chat history and the latest suggestions from the last session on return | P0 |
| Contact info nudge | AI guides clients to request contact info at an appropriate moment | P1 |
| Multi-language support | Detect browser language; match UI and AI replies to user's language | P2 |
| Mobile responsiveness | Responsive layout | P0 |

### 3.2 Login and Identity

**SSO login**:
- Supports major third-party providers (e.g., Google, GitHub) to reduce sign-up friction
- Binds user identity after login and persists chat history

**Guest mode**:
- No login required — enter the chat directly
- Session data is stored locally in the browser (localStorage) and cleared on close
- The page may prompt guests to log in at an appropriate moment (e.g., after several conversation rounds) to save their history

### 3.3 Dynamic Suggestions Mechanism

```
First page load
  └── Display initial suggestions configured by the owner in the admin console (3–6 items)

User asks a question → AI finishes replying
  └── AI generates 2–4 follow-up suggestions based on the current exchange
  └── Shown below the AI reply; users can click to ask a follow-up question

User returns after logging in
  └── Restore conversation history
  └── Display follow-up suggestions from the last reply in the previous session, for easy continuation
```

**Suggestion display format**: small cards/chips; clicking one sends the suggestion as a message without affecting manual input.

### 3.4 UI Design Requirements

Reference Gemini (gemini.google.com) visual style:
- Dark / light theme toggle
- Centered wide chat area
- Clean conversation layout: user messages in right-aligned bubbles; AI replies displayed full-width with Markdown rendering
- Fixed input bar at the bottom with a send button and a reserved attachment slot (P2)
- On the home screen before any input: show a branded welcome message + initial suggestion cards
- Login entry in the top-right corner (show avatar/nickname when logged in)

### 3.5 AI Behavior Guidelines

- Only answer questions relevant to the owner; politely decline off-topic requests
- Replies **must be strictly based on knowledge base content** — never use general knowledge or training data to fill gaps; never fabricate information
- For general greetings or small talk (e.g., "hello", "thank you"), respond warmly without refusing
- If the knowledge base has no answer, politely inform the user and suggest contacting the owner directly for more information
- At the end of each reply, output 2–4 follow-up suggestions as a structured field (not mixed into the reply body)
- Tone: professional, friendly, concise
- Anti-hallucination mechanism: the system prompt always injects a "behavior rules" constraint block; when RAG returns empty, explicitly state "there is currently no relevant knowledge base content" to prevent the AI from answering freely

---

## 4. Owner Admin Console

### 4.1 Feature List

| Feature | Description | Priority | Status |
|---------|-------------|----------|--------|
| Owner account login | Username/password login; default password 888888; supports username and password changes | P0 | ✅ Live |
| Profile settings | Configure avatar, name, tagline, contact info, and other display fields | P0 | ✅ Live |
| AI assistant custom instructions | Set the AI assistant's tone and style; sandboxed injection into the system prompt | P0 | ✅ Live |
| File upload to knowledge base | Supports PDF, Word, TXT, Markdown; auto-extracts content | P0 | ✅ Live |
| Knowledge entry management | Manually create/view/edit/delete knowledge entries (TEXT / FAQ / STRUCTURED types) | P0 | ✅ Live |
| Initial suggestion management | Configure home-screen guide questions; supports enable/disable and sort order | P0 | ✅ Live |
| Image upload | Upload portfolio images; AI extracts descriptions (with optional text annotations) | P1 | ⬜ Planned |
| Conversation history | View past client–AI conversations (for improving the knowledge base) | P2 | ⬜ Planned |
| Knowledge base statistics | Overview of entry count, covered topics, etc. | P2 | ⬜ Planned |

**Super Admin Panel** (separate entry at `/admin-panel`): hardcoded token auth; creates/deletes owner accounts; for system administrators only.

### 4.2 Knowledge Base Entry Flow (Core)

```
Owner uploads a file / enters text
        ↓
AI extracts key information (skills, projects, experience, achievements, etc.)
        ↓
AI displays extracted results; owner confirms or edits
        ↓
Stored in the vector database (with semantic embeddings)
```

**Supported content types**:
- Skills and tech stack (with proficiency level)
- Work / project experience (dates, company/client, role, outcomes)
- Portfolio (project description, tech used, links, screenshots)
- Education background and certifications
- Services offered and pricing range
- Personal strengths and differentiators

### 4.3 UI Design Requirements

- Left navigation: Knowledge Base, Profile Settings, Suggestion Config, Conversation History
- Main area: conversational entry interface (similar to the client portal, but with a file upload entry)
- Knowledge base list: card-based display with type filtering and search
- Clean and functional — no need for elaborate styling

---

## 5. Interaction Flow Design

### 5.1 Client Portal Flows

#### 5.1.1 First-Time Guest Visit

```
Visit /{ownerUsername}/chat
  └── Load home screen: show owner avatar, name, tagline
        └── Below: welcome message + initial suggestion cards (3–6)
              ├── Click a suggestion card → trigger question → enter chat flow (see 5.1.4)
              └── Type a question manually → enter chat flow (see 5.1.4)
```

#### 5.1.2 Logged-in User Visit

```
Visit /{ownerUsername}/chat
  └── Detect logged-in identity (cookie/token valid)
        └── Has previous conversation?
              ├── Yes → Restore message history
              │         └── Show dynamic suggestions from the end of the last session
              │               ├── Click a suggestion → follow-up → chat flow (see 5.1.4)
              │               └── Type manually → chat flow (see 5.1.4)
              └── No → Same as first-time guest home screen (see 5.1.1)
```

#### 5.1.3 Guest-to-Login Conversion

```
Conversation in progress (as guest)
  └── Conversion trigger (e.g., after the 3rd exchange, AI inserts a login nudge)
        └── Show "Log in to save your conversation" banner (dismissible, not mandatory)
              ├── Click Login → launch SSO login modal
              │     └── Authorized → bind identity, migrate local session to server
              │           └── Page resumes conversation seamlessly
              └── Dismiss → continue as guest; no repeat prompt for this session
```

#### 5.1.4 Chat Flow (General)

```
User sends a message (click suggestion card or type + send)
  └── Input disabled; loading state shown
        └── Backend RAG retrieval + AI generation (streaming output)
              └── AI reply area renders text incrementally (full-width)
                    └── Streaming output ends
                          ├── Render complete AI reply
                          ├── Show dynamic suggestion cards below the reply (2–4, blue chip style)
                          └── Input re-enabled
```

### 5.2 Admin Console Flows

#### 5.2.1 Login Flow

```
Visit admin URL
  └── Not logged in → redirect to login page
        └── Enter username + password → click Login
              ├── Auth failed → show error, stay on login page
              └── Auth succeeded → redirect to Knowledge Base home
```

#### 5.2.2 Add Text to Knowledge Base

```
Go to Knowledge Base page (left nav)
  └── Main area shows the entry dialog interface
        └── Owner types content (e.g., pastes a resume section, describes a project)
              └── Send → AI parses and shows extracted results
                    ├── Results shown as structured cards (type, title, content summary)
                    │     ├── Owner clicks "Save" → written to knowledge base; card switches to saved state
                    │     ├── Owner clicks "Edit" → inline editing; confirm to save
                    │     └── Owner clicks "Discard" → entry not saved; continue entering
                    └── After saving, the owner can continue entering new content
```

#### 5.2.3 Upload File to Knowledge Base

```
Click "Upload File" button in the entry interface
  └── Select a file (PDF / Word / TXT)
        └── Uploading → progress bar shown
              └── Upload complete → AI processes in the background
                    └── Processing done → show extracted entry list (same as last half of 5.2.2)
                          └── Owner confirms entries individually / in bulk / discards in bulk
```

#### 5.2.4 Knowledge Entry Management

```
Go to Knowledge Base page
  ├── "Add Knowledge Entry" button at the top; click to expand the form (type, title, content)
  └── Below: list of saved knowledge entries (paginated, 10 per page)
        ├── Filter by type (skill / project / experience / education / other)
        ├── Keyword search
        ├── Pagination controls at the bottom (shown when > 10 entries)
        └── Click an entry card
              ├── Expand to view full content
              └── Click "Delete" → confirmation dialog → remove on confirm
```

#### 5.2.5 Profile Configuration

```
Click "Profile Settings" in the left nav
  └── Show configuration form: avatar upload, name, tagline, contact info
        └── Edit and click "Save"
              ├── Save succeeded → top banner shows "Updated"; changes take effect on the client portal immediately
              └── Save failed → show error message; form retains edited state
```

#### 5.2.6 Suggestion Configuration

```
Click "Suggestion Config" in the left nav
  └── Show the current list of enabled initial suggestions (drag to reorder)
        ├── Click "Add" → modal to enter suggestion text → confirm to add
        ├── Click "Edit" → inline text editing → save
        ├── Click "Delete" → confirmation → remove
        └── Toggle enable/disable → takes effect on client portal home screen immediately
```

---

## 6. AI Capabilities

### 6.1 RAG (Retrieval-Augmented Generation)

- Client asks a question → semantic retrieval of relevant knowledge entries → context assembled → answer generated
- Retrieval strategy: vector similarity + keyword hybrid search

### 6.2 Knowledge Extraction (Admin Console)

- Parse uploaded documents, identify and structure key information
- Detect intent (e.g., "this is a project experience section") and categorize for storage
- Owner can confirm or edit extracted results

### 6.3 Multi-Provider Compatibility

- Abstract AI service layer; switch providers via config file without changing business code
- Supported providers:

| Provider | Description | Status |
|----------|-------------|--------|
| Google Gemini | Cloud model; requires `GOOGLE_AI_API_KEY`; used on non-GCP environments | ✅ Default provider |
| Vertex AI Gemini | GCP cloud model; uses Application Default Credentials (ADC); auto-activated on Cloud Run / GKE / GCE | ✅ Auto on GCP |
| Claude (Anthropic) | High-quality cloud model; requires `ANTHROPIC_API_KEY` | ✅ Integrated |
| Mock | Local simulation; no API key needed; ideal for development/debugging | ✅ Default dev mode |
| Ollama (local model) | Runs on local machine or intranet; no API key; data stays on-premise | ✅ Optional, non-default |

- Configuration: set `ai.provider: google / claude / ollama` in `application.yml`; cloud providers support mock mode via `ai.mock: true`
- When `ai.provider=google` and the backend is deployed on GCP (Cloud Run, GKE, GCE, App Engine), Vertex AI is activated automatically via ADC — no `GOOGLE_AI_API_KEY` is needed. On non-GCP environments, Google AI Studio is used as before.

---

## 7. Data Model (Conceptual)

| Entity | Key Fields |
|--------|-----------|
| Owner | Avatar, name, tagline, contact info, configuration |
| ClientUser | SSO provider, third-party ID, nickname, avatar, created at |
| KnowledgeEntry | Type, title, content text, vector embedding, source document, created at |
| Document | Filename, type, size, processing status, linked knowledge entries |
| Conversation | User ID (nullable = guest), source (client/admin), message list, created at |
| Message | Conversation ID, role (user/assistant), body, linked dynamic suggestions, created at |
| PromptSuggestion | Display text, sort order, enabled status (configured in admin console) |
| DynamicPrompt | Linked message ID, suggestion text, generated at |

---

## 8. Non-Functional Requirements

| Dimension | Requirement |
|-----------|-------------|
| Performance | AI replies support streaming output; time-to-first-token < 2s |
| Security | All admin endpoints require authentication; API keys not exposed in the frontend |
| Deployment | Docker Compose one-command startup for all services |
| Scalability | Knowledge base supports multiple owners (SaaS extension reserved; single-user in MVP) |
| Availability | Client portal accessible without login |

---

## 9. Tech Stack (Reference)

> Suggestions below; final choices confirmed in the technical design document

| Layer | Recommendation |
|-------|---------------|
| Frontend | React + Next.js (TypeScript) |
| Backend | Java Spring Boot |
| AI integration | Spring AI (multi-provider switching) |
| Vector storage | pgvector (PostgreSQL extension) or Milvus |
| File storage | Local mount / MinIO |
| Deployment | Docker Compose |

---

## 10. MVP Scope (Phase 1)

**Included**:
- Client portal: chat, quick suggestions, owner profile display
- Admin console: login, text/file knowledge base entry, entry management, profile configuration
- AI: RAG Q&A + knowledge extraction
- Deployment: Docker Compose

**Excluded**:
- Multi-language support
- Conversation history viewer
- Image content understanding
- Multi-user / SaaS

---

## 11. Open Questions

- [x] Should the product support multiple owner accounts (SaaS mode) or single-user? → **Multiple owners supported**, isolated by URL `/{username}/chat`; super admin panel manages all accounts
- [ ] Is custom domain support needed for each owner (each owner has their own domain)?
- [ ] Should the AI be allowed to display images in replies (e.g., portfolio screenshots)?
- [ ] Do owners need the ability to review and intervene in AI conversations (view history and provide corrections)?
