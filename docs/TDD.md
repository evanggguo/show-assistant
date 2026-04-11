# Show Assistant — 技术设计文档（TDD）

---

## 1. 文档说明

本文档基于 PRD，细化系统的技术实现方案。**重点**在第 4 节——动态提示词与流式对话，这是产品最核心、最复杂的部分，也是 MVP 阶段优先跑通的目标。

---

## 2. 技术选型确认

| 层次 | 选型 | 说明 |
|------|------|------|
| 前端 | Next.js 15 (App Router, TypeScript) | SSR + 客户端组件混用，天然支持 SSE |
| 后端 | Java 21 + Spring Boot 3 | 熟悉栈；Spring AI 原生支持 Claude 流式 + Tool Use |
| AI 接入 | Spring AI + 多提供商抽象 | 默认本地 Ollama；可配置切换 Claude / OpenAI |
| 向量存储 | PostgreSQL + pgvector | 减少组件数，MVP 阶段足够；pgvector 支持 HNSW 索引 |
| 关系存储 | PostgreSQL（同库） | 对话记录、用户、知识条目共用一个 PG 实例 |
| 文件存储 | 本地挂载（MinIO 预留接口） | Docker Volume 挂载，接口层抽象便于后续迁移 |
| 部署 | Docker Compose | 一键启动：next、spring-boot、postgres |

---

## 3. 系统架构

### 3.1 模块划分

```
show-assistant/
├── frontend/          # Next.js，客户端 + 管理端两套页面
│   ├── app/
│   │   ├── (client)/  # 客户端路由组
│   │   └── admin/     # 管理端路由组
│   └── components/
│       ├── chat/      # 对话相关组件（核心）
│       └── admin/     # 管理端组件
│
└── backend/           # Spring Boot
    ├── chat/          # 对话模块（流式 + RAG + 动态提示词）
    ├── knowledge/     # 知识库模块（录入、检索）
    ├── conversation/  # 会话持久化模块
    ├── owner/         # 拥有者信息模块
    └── ai/            # AI 服务抽象层
        ├── provider/  # 多模型提供商实现
        │   ├── AiChatProvider        # 接口（流式对话抽象）
        │   ├── OllamaChatProvider    # 本地 Ollama（默认，真实实现）
        │   ├── ClaudeChatProvider    # Anthropic Claude（Mock，待接入）
        │   └── OpenAiChatProvider    # OpenAI GPT（Mock，待接入）
        ├── ChatService
        ├── EmbeddingService
        └── ExtractionService
```

### 3.2 部署结构

```
                    Browser
                       │
          ┌────────────┴────────────┐
          ▼                         ▼
   Next.js (3000)            Next.js (3000)
   /  (Client Portal)        /admin (Admin Console)
          │                         │
          └────────────┬────────────┘
                       ▼
             Spring Boot (8080)
             ┌─────────────────┐
             │   REST + SSE    │
             └────────┬────────┘
                      ▼
             PostgreSQL (5432)
             ├── 关系表（conversations, messages, …）
             └── pgvector 扩展（knowledge_entries.embedding）
```

---

## 4. 核心功能详细设计：动态提示词与流式对话

> 这是系统最核心的部分。本节从整体数据流开始，逐层拆解每个子问题的设计决策。

### 4.1 整体数据流

```
用户发送消息
     │
     ▼
[Frontend] POST /api/chat/stream
  携带：{ conversationId?, message, history[] }
     │
     ▼
[Backend: ChatController]
  1. 加载/创建会话
  2. 保存 user message 到数据库
  3. 触发 RAG 检索
  4. 组装 Prompt（系统提示 + 检索上下文 + 对话历史）
  5. 调用 Claude API（流式 + Tool Use）
  6. 以 SSE 向前端推送 token
  7. 流结束时，捕获 Tool Use 结果（动态提示词）
  8. 保存 assistant message + 动态提示词到数据库
  9. 推送 SSE done 事件（含提示词）
     │
     ▼
[Frontend: useChatStream hook]
  - 实时渲染 token 到消息气泡
  - 接收 done 事件，展示动态提示词卡片
```

### 4.2 RAG 检索管道

#### 4.2.1 知识嵌入（写入时）

```
拥有者录入内容（文字 / 文件）
     │
     ▼
AI 提取结构化知识条目（ExtractionService）
每条 KnowledgeEntry：{ type, title, content }
     │
     ▼
EmbeddingService.embed(content)
  → 调用 Claude / OpenAI Embedding API
  → 返回 float[] 向量（1536 维 或 1024 维）
     │
     ▼
INSERT INTO knowledge_entries (content, embedding, type, …)
  使用 pgvector 存储向量列
```

#### 4.2.2 语义检索（对话时）

```
用户问题 q
     │
     ▼
EmbeddingService.embed(q) → queryVector
     │
     ▼
SELECT content, type, title
  FROM knowledge_entries
  ORDER BY embedding <=> queryVector   -- pgvector 余弦距离
  LIMIT 8
     │
     ▼
返回 top-K 相关知识片段，注入 Prompt
```

**检索增强策略**（MVP 后可迭代）：
- MVP：纯向量相似度检索，`<=>` 余弦距离
- 迭代 1：混合检索，加入 PostgreSQL 全文搜索（`tsvector`），结果 RRF 融合
- 迭代 2：按 `type` 加权，提问技能类问题时技能条目权重更高

### 4.3 流式对话协议（SSE）

SSE（Server-Sent Events）是本系统前后端实时通信的唯一协议。选择 SSE 而非 WebSocket 的原因：对话是单向推送（服务端→客户端），SSE 实现更简单，且 Spring Boot / Next.js 均原生支持。

#### 4.3.1 SSE 事件类型定义

服务端向客户端推送以下三种事件，均为 `text/event-stream` 格式：

```
# 1. 文本 token（流式逐字输出）
event: token
data: {"text": "你好"}

event: token
data: {"text": "，我"}

event: token
data: {"text": "可以帮你..."}

# 2. 流结束（携带动态提示词 + 消息 ID）
event: done
data: {
  "messageId": "msg_abc123",
  "suggestions": [
    "他擅长哪些技术栈？",
    "有没有相关的项目案例？",
    "如何联系他？"
  ]
}

# 3. 错误
event: error
data: {"code": "RATE_LIMITED", "message": "请稍后再试"}
```

**设计要点**：
- `token` 事件高频（每个词片段一条），`data` 只含增量文本，不含历史
- `done` 事件在 Claude 流完全结束后发送一次，携带完整的结构化提示词
- 前端收到 `done` 后关闭 EventSource 连接，展示提示词卡片
- `messageId` 由后端生成并返回，前端用于后续操作（如点击提示词时关联上下文）

#### 4.3.2 服务端处理流程（Spring Boot）

```java
// ChatController.java（伪代码）
@GetMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamChat(@RequestBody ChatRequest req) {
    SseEmitter emitter = new SseEmitter(180_000L); // 3 分钟超时

    executor.submit(() -> {
        // 1. RAG 检索
        List<String> context = ragService.retrieve(req.getMessage());

        // 2. 组装 Prompt
        Prompt prompt = promptBuilder.build(req, context);

        // 3. 调用 Claude 流式 API（含 Tool Use）
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
                    // 捕获动态提示词，暂存
                    suggestions = input.get("suggestions");
                }
            }

            @Override
            public void onComplete(String fullText) {
                // 4. 持久化
                Message msg = conversationService.saveAssistantMessage(
                    req.getConversationId(), fullText, suggestions);

                // 5. 发送 done 事件
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

#### 4.3.3 客户端处理逻辑（Next.js）

```typescript
// hooks/useChatStream.ts（伪代码）
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

    // 手动解析 SSE 流（fetch + ReadableStream，兼容性优于 EventSource）
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
          // 通知父组件将 streamingText 转为正式消息
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

**为什么用 `fetch + ReadableStream` 而非 `EventSource`**：
- `EventSource` 只支持 GET 请求，无法携带 JSON Body（会话信息、消息内容）
- `fetch` 方案完全控制请求格式，同时可读取 SSE 事件流

### 4.4 动态提示词生成策略

#### 4.4.1 方案选型对比

生成动态提示词（每轮 2~4 条追问建议）有以下几种实现方式：

| 方案 | 原理 | 优点 | 缺点 |
|------|------|------|------|
| **A. 流后独立请求** | 主流结束后，单独发一条 AI 请求生成建议 | 实现简单 | 额外延迟 ~1s；两次调用 |
| **B. 流内分隔符** | 要求模型在回复末尾追加 `---SUGGESTIONS---\n[...]` | 单次调用 | 模型输出不稳定，解析脆弱 |
| **C. Tool Use（推荐）** | 定义 `suggest_followups` 工具，模型在回复末尾强制调用 | 结构化可靠；单次调用；延迟低 | 需理解 Claude Tool Use API |

**选择方案 C（Tool Use）**，原因：Claude 的 Tool Use 在流式模式下完全支持，且 tool call 的结构化输出比自由文本解析可靠得多。

#### 4.4.2 Tool Use 方案详细设计

**工具定义**（发送给 Claude 的 tools 参数）：

```json
{
  "name": "suggest_followups",
  "description": "在每次回答结束后，生成 2~4 条用户可能感兴趣的追问建议。建议应基于本轮对话内容，自然引导用户深入了解。",
  "input_schema": {
    "type": "object",
    "properties": {
      "suggestions": {
        "type": "array",
        "items": { "type": "string" },
        "minItems": 2,
        "maxItems": 4,
        "description": "追问建议列表，每条不超过 20 字"
      }
    },
    "required": ["suggestions"]
  }
}
```

**Tool Use 在流式响应中的位置**：

Claude 流式 API 的事件序列如下：

```
# 1. 正常文本 token（主回答）
{"type": "content_block_delta", "delta": {"type": "text_delta", "text": "..."}}

# 2. 文本结束，开始 tool call block
{"type": "content_block_stop"}
{"type": "content_block_start", "content_block": {"type": "tool_use", "name": "suggest_followups"}}

# 3. Tool call 参数（JSON 增量，需累积拼接）
{"type": "content_block_delta", "delta": {"type": "input_json_delta", "partial_json": "{\"suggestions\":"}}
{"type": "content_block_delta", "delta": {"type": "input_json_delta", "partial_json": "[\"他的主要技术栈？\","}}
...

# 4. Tool call 结束
{"type": "content_block_stop"}
{"type": "message_stop"}
```

后端 Spring AI 的 `StreamingChatClient` 会将以上事件封装，开发者通过回调分别处理文本 delta 和 tool call。

**tool_choice 设置**：

```json
"tool_choice": {"type": "auto"}
```

不强制要求每次都调用，模型在合适时调用（实际上在 System Prompt 中明确要求每次必须调用）。

#### 4.4.3 System Prompt 设计

```
你是 {ownerName} 的个人展示助理。你的职责是帮助潜在客户了解 {ownerName} 的技能、经历和作品集。

## 行为规范
- 只回答与 {ownerName} 相关的问题，对无关话题礼貌拒绝
- 回答基于下方提供的知识库内容，不编造信息
- 若知识库中无相关信息，引导客户直接联系 {ownerName}
- 语气：专业、友好、简洁

## 知识库上下文
以下是与用户问题相关的背景信息：

{retrievedContext}

## 重要：追问建议
每次回答结束后，你**必须**调用 suggest_followups 工具，生成 2~4 条自然的追问建议。
建议应基于本轮对话的具体内容，帮助用户深入了解感兴趣的方向。
```

### 4.5 多模型提供商抽象

#### 4.5.1 设计目标

PRD 6.3 要求 AI 服务层支持运行时切换 Claude、OpenAI、本地模型。设计原则：

- `ChatService` 只依赖 `AiChatProvider` 接口，不感知具体提供商
- 通过 `application.yml` 中的 `ai.provider` 配置项选择实现
- 当前阶段：**Ollama（本地）为真实实现**，Claude/OpenAI 为 Mock（返回模拟数据）
- 后续直接替换对应实现类，不改调用方代码

#### 4.5.2 接口定义

```java
// ai/provider/AiChatProvider.java
public interface AiChatProvider {

    /**
     * 流式对话，返回 token 流。
     * 约定：流结束时，最后一个元素以 "[DONE]" 标记；
     *       suggest_followups 的 JSON 通过 SuggestFollowupsTool 捕获，不混入流。
     *
     * @param messages  完整的对话历史（含 system prompt）
     * @param tools     注册的工具实例列表（Spring AI @Tool bean）
     * @return Flux<String> token 增量文本流
     */
    Flux<String> streamChat(List<Message> messages, Object... tools);

    /**
     * 提供商标识，用于日志和监控
     */
    String providerName();
}
```

#### 4.5.3 提供商实现概览

| 实现类 | 状态 | 说明 |
|--------|------|------|
| `OllamaChatProvider` | ✅ 真实实现 | 调用本地 Ollama HTTP API，默认模型 `qwen2.5:7b` |
| `ClaudeChatProvider` | 🔲 Mock | 返回固定模拟 token 流，用于无 API Key 环境测试 |
| `OpenAiChatProvider` | 🔲 Mock | 返回固定模拟 token 流，用于无 API Key 环境测试 |

#### 4.5.4 配置驱动选择

```yaml
# application.yml
ai:
  provider: ollama          # 可选: ollama | claude | openai
  ollama:
    base-url: http://localhost:11434
    model: qwen2.5:7b
  claude:
    api-key: ${ANTHROPIC_API_KEY:}
    model: claude-sonnet-4-6
  openai:
    api-key: ${OPENAI_API_KEY:}
    model: gpt-4o
```

Spring 通过 `@ConditionalOnProperty` + `@Primary` 在启动时装配唯一的 `AiChatProvider` Bean：

```java
@Bean
@ConditionalOnProperty(name = "ai.provider", havingValue = "ollama", matchIfMissing = true)
public AiChatProvider ollamaChatProvider(...) { ... }

@Bean
@ConditionalOnProperty(name = "ai.provider", havingValue = "claude")
public AiChatProvider claudeChatProvider(...) { ... }

@Bean
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
public AiChatProvider openAiChatProvider(...) { ... }
```

#### 4.5.5 Ollama 本地模型说明

- **运行方式**：独立进程，暴露 HTTP API（`http://localhost:11434`）
- **Spring AI 集成**：`spring-ai-starter-model-ollama` 提供 `OllamaChatModel` Bean
- **推荐模型**：
  - 中英双语对话：`qwen2.5:7b`（阿里通义，7B 参数，消费级 GPU / 16GB 内存 CPU 可运行）
  - 仅英文或低内存：`llama3.2:3b`
- **Tool Use 支持**：Ollama 0.3+ 支持 OpenAI 兼容的 function calling，Spring AI 的 `@Tool` 注解可直接使用

#### 4.5.6 Docker Compose 本地模型支持

在 `docker-compose.yml` 中按需启用 Ollama 服务（可选，也可使用宿主机 Ollama）：

```yaml
ollama:
  image: ollama/ollama:latest
  container_name: showassistant-ollama
  ports:
    - "11434:11434"
  volumes:
    - ollama_data:/root/.ollama
  # GPU 加速（可选，需要 NVIDIA Container Toolkit）
  # deploy:
  #   resources:
  #     reservations:
  #       devices:
  #         - driver: nvidia
  #           count: all
  #           capabilities: [gpu]
```

后端环境变量配置：

```yaml
AI_PROVIDER: ollama
AI_OLLAMA_BASE_URL: http://ollama:11434   # Docker 网络内访问
```

---

### 4.6 会话管理

#### 4.6.1 游客会话（localStorage）

```typescript
// 游客会话存储结构（localStorage key: "guest_conversation"）
interface GuestConversation {
  id: string;                  // 前端生成的临时 UUID
  messages: LocalMessage[];
  lastSuggestions: string[];   // 最近一次的动态提示词
  createdAt: string;
}
```

- 游客的 `conversationId` 以 `guest_` 前缀区分
- 发送消息时携带完整的本地 `history`（最多 20 条），后端无状态处理
- 后端**不持久化**游客消息（或可选持久化，但不与用户绑定）

#### 4.6.2 登录用户会话（服务端持久化）

- 登录后，`conversationId` 为服务端生成的真实 ID
- 每次发消息前，前端**不需要**携带 history，后端从数据库加载最近 N 条消息
- 加载历史时，同时返回 `lastSuggestions`（最后一条 assistant message 关联的动态提示词）

---

## 5. 数据库设计

### 5.1 关键表结构

```sql
-- 拥有者信息
CREATE TABLE owners (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    tagline     VARCHAR(200),
    avatar_url  TEXT,
    contact     JSONB,         -- { "email": "...", "wechat": "..." }
    config      JSONB,         -- 预留扩展配置
    created_at  TIMESTAMPTZ   DEFAULT NOW()
);

-- 客户用户（SSO）
CREATE TABLE client_users (
    id            BIGSERIAL PRIMARY KEY,
    sso_provider  VARCHAR(50)  NOT NULL,  -- 'google' | 'github'
    sso_id        VARCHAR(200) NOT NULL,
    nickname      VARCHAR(100),
    avatar_url    TEXT,
    created_at    TIMESTAMPTZ  DEFAULT NOW(),
    UNIQUE (sso_provider, sso_id)
);

-- 会话
CREATE TABLE conversations (
    id          BIGSERIAL PRIMARY KEY,
    owner_id    BIGINT       REFERENCES owners(id),
    user_id     BIGINT       REFERENCES client_users(id),  -- NULL = 游客
    source      VARCHAR(20)  NOT NULL,  -- 'client' | 'admin'
    created_at  TIMESTAMPTZ  DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  DEFAULT NOW()
);

-- 消息
CREATE TABLE messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT       NOT NULL REFERENCES conversations(id),
    role            VARCHAR(20)  NOT NULL,  -- 'user' | 'assistant'
    content         TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  DEFAULT NOW()
);

-- 动态提示词（关联到 assistant message）
CREATE TABLE dynamic_suggestions (
    id          BIGSERIAL PRIMARY KEY,
    message_id  BIGINT  NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    text        TEXT    NOT NULL,
    sort_order  INT     NOT NULL DEFAULT 0
);

-- 初始提示词（管理端配置）
CREATE TABLE prompt_suggestions (
    id          BIGSERIAL PRIMARY KEY,
    owner_id    BIGINT       NOT NULL REFERENCES owners(id),
    text        TEXT         NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE
);

-- 知识条目（含向量）
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

-- 向量检索索引（HNSW，近似最近邻，查询更快）
CREATE INDEX idx_knowledge_embedding ON knowledge_entries
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- 上传文档
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

## 6. API 接口设计

### 6.1 流式对话（核心接口）

```
POST /api/chat/stream
Content-Type: application/json
Accept: text/event-stream

Request Body:
{
  "conversationId": "123",       // 可选，游客无此字段
  "message": "他擅长哪些技术？",
  "history": [                   // 仅游客携带，登录用户留空
    { "role": "user", "content": "..." },
    { "role": "assistant", "content": "..." }
  ]
}

Response: text/event-stream（见 4.3.1 事件定义）
```

### 6.2 会话接口

```
# 获取会话历史（含最后一次动态提示词）
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
      "suggestions": ["追问1", "追问2"],   // 该消息关联的动态提示词
      "createdAt": "..."
    }
  ],
  "lastSuggestions": ["追问1", "追问2"]  // 最后一条 assistant message 的提示词（快捷访问）
}
```

### 6.3 初始提示词接口

```
# 获取客户端首屏初始提示词
GET /api/suggestions/initial

Response:
{
  "suggestions": ["他的主要项目有哪些？", "擅长哪些技术？", "如何合作？"]
}
```

### 6.4 拥有者简介接口

```
GET /api/owner/profile

Response:
{
  "name": "张三",
  "tagline": "全栈开发者 & 独立产品人",
  "avatarUrl": "https://..."
}
```

---

## 7. 前端核心组件设计

```
app/(client)/page.tsx                  # 客户端主页
  └── <ChatPage>
        ├── <OwnerProfile>             # 头像、姓名、tagline（首屏展示）
        ├── <MessageList>              # 消息列表
        │     ├── <MessageBubble>      # 单条消息（支持 Markdown）
        │     │     └── <SuggestionCards>  # 该消息下方的动态提示词卡片
        │     └── <StreamingBubble>   # 正在流式输出的气泡（实时更新）
        ├── <InitialSuggestions>       # 首屏未对话时展示的初始提示词
        └── <ChatInput>                # 底部输入框 + 发送按钮
```

**状态管理**：无需全局状态库（Zustand/Redux），使用 React Context + `useChatStream` Hook 管理对话状态。

---

## 8. MVP 最小跑通路径

按依赖顺序，最小化实现核心链路：

```
阶段 1：静态骨架（1~2 天）
  ✓ Next.js 项目初始化，客户端页面布局（参考 Gemini 风格）
  ✓ Spring Boot 项目初始化，数据库表创建（Flyway migration）

阶段 2：无 RAG 的流式对话（1~2 天）
  ✓ 后端：/api/chat/stream 接口，直接调用 Claude 流式 API
  ✓ 后端：Tool Use 接入，捕获 suggest_followups 结果
  ✓ 后端：SSE 推送 token + done 事件
  ✓ 前端：useChatStream Hook，实时渲染流式文本
  ✓ 前端：done 事件后展示动态提示词卡片
  → 目标：能对话 + 出提示词卡片，AI 使用通用知识（无知识库）

阶段 3：接入知识库（2~3 天）
  ✓ 后端：pgvector 配置，EmbeddingService 封装
  ✓ 后端：手动插入 2~3 条测试知识条目（跳过管理端录入）
  ✓ 后端：RAG 检索，注入 System Prompt
  → 目标：AI 能基于知识库内容回答，提示词与内容相关

阶段 4：拥有者简介 + 初始提示词（1 天）
  ✓ 数据库初始化拥有者数据（硬编码）
  ✓ /api/owner/profile 接口
  ✓ /api/suggestions/initial 接口
  ✓ 前端首屏展示 Profile + 初始提示词卡片
  → 目标：完整的首屏体验

阶段 5：管理端基础录入（后续）
  ✓ 文本录入知识库
  ✓ 知识条目列表管理
```

**阶段 2 是最关键的验证节点**，跑通后整个核心链路的技术风险即解除。

---

## 9. 关键技术风险与应对

| 风险 | 说明 | 应对 |
|------|------|------|
| Tool Use 在流式模式下的兼容性 | Spring AI 对 Claude 流式 Tool Use 的封装可能不完整 | 优先验证；必要时直接使用 Anthropic Java SDK 或 HTTP Client 手写 |
| SSE 连接在 Next.js 中间层丢失 | Next.js App Router 的 API Route 对长连接支持有限 | 前端直接请求后端（跨域配置），跳过 Next.js 中间层转发 |
| pgvector 向量维度与模型不匹配 | 不同 embedding 模型输出维度不同，建表时需确认 | 初始化时统一用 `text-embedding-3-small`（1536 维）或 Claude 的 embedding（实际确认维度） |
| 游客 history 过长 | 游客携带全量 history 可能导致 token 超限 | 截取最近 10 轮（20 条消息），超出部分丢弃最早的 |
