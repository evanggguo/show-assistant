# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

Dossier 是 AI 个人展示助理，让访客通过聊天了解拥有者的技能和履历。当前 MVP 为单用户模式，`owner_id=1` 硬编码在后端。

**两个子系统**（管理端尚未开发）：
- **客户端（Client Portal）**：无需登录的 AI 聊天界面
- **管理端（Admin Console）**：计划中，用于管理知识库和配置

## 常用命令

### 后端（Spring Boot）

```bash
cd backend

# 启动（依赖本地 PostgreSQL，端口 8080）
./mvnw spring-boot:run

# 编译
./mvnw compile

# 运行全部测试
./mvnw test

# 运行单个测试类
./mvnw test -Dtest=ChatServiceTest

# 运行单个测试方法
./mvnw test -Dtest=ChatServiceTest#testHandleStream
```

### 前端（Next.js）

```bash
cd frontend

# 开发服务器（端口 3000）
npm run dev

# 构建
npm run build

# Lint
npm run lint
```

### Docker（完整环境）

```bash
# 默认 Mock 模式启动（无需任何 API Key）
docker compose up -d

# 切换 AI 提供商
AI_PROVIDER=ollama docker compose up -d
AI_PROVIDER=claude AI_MOCK=false ANTHROPIC_API_KEY=<key> docker compose up -d

# 查看后端日志
docker compose logs -f backend
```

本地开发时，先用 Docker 启动 PostgreSQL，再单独运行后端：
```bash
docker compose up postgres -d
cd backend && ./mvnw spring-boot:run
```

## 架构

### 后端包结构

```
com.dossier.backend
├── ai/
│   ├── provider/           # AI 提供商抽象层
│   │   ├── AiChatProvider  # 核心接口：streamChat() + generateSuggestions()
│   │   ├── OllamaChatProvider
│   │   ├── ClaudeChatProvider
│   │   └── MockChatProvider
│   └── EmbeddingService    # 向量化（Phase 3 使用）
├── chat/
│   ├── ChatController      # POST /api/chat/stream
│   ├── ChatService         # 核心流式处理（@Async）
│   ├── PromptAssembler     # 构建 System Prompt
│   ├── SseEventBuilder     # SSE 事件格式化
│   └── tool/
│       └── SuggestFollowupsTool  # per-request 非 Bean，捕获动态建议
├── conversation/           # 会话 & 消息持久化
├── knowledge/
│   ├── RagService          # Phase 2 为 stub，Phase 3 接入向量检索
│   └── KnowledgeService
├── owner/                  # Owner 信息 & 预设提示词
├── document/               # 文件管理（Phase 3）
└── config/
    ├── AiConfig            # 条件注册 AiChatProvider Bean
    ├── AsyncConfig         # sseTaskExecutor 线程池
    ├── CorsConfig
    └── SecurityConfig
```

### AI 提供商切换逻辑

通过 `AiConfig` 按条件注册唯一的 `AiChatProvider` Bean：

| `ai.provider` | `ai.mock` | 生效提供商 |
|---|---|---|
| `ollama`（默认） | 任意 | OllamaChatProvider（始终真实调用） |
| `claude` | `true`（默认） | MockChatProvider |
| `claude` | `false` | ClaudeChatProvider（需 ANTHROPIC_API_KEY） |

### 核心流式聊天流程

`ChatService.handleStream()`（`@Async("sseTaskExecutor")`）按顺序执行：
1. 创建或复用会话（`conversationId=null` 时为游客模式）
2. 保存 user message 到 DB
3. `RagService.retrieve()` — Phase 2 返回空列表
4. 构建 Spring AI 消息列表（SystemMessage + 历史 + 当前消息）
5. `PromptAssembler.assemble()` 动态构建含 Owner 信息和 RAG 上下文的 System Prompt
6. 创建 per-request `SuggestFollowupsTool` 实例（非 Spring Bean，避免状态共享）
7. `AiChatProvider.streamChat()` 流式订阅
8. 流结束后：检查 `SuggestFollowupsTool.getCapturedSuggestions()`；若为空（小模型不支持 Function Calling），调用 `generateSuggestions()` fallback
9. 保存 assistant message + suggestions，推送 `done` SSE 事件

### 前端架构

**路由**：`app/(client)/chat/page.tsx` → 客户端聊天页。`(client)` 是 Route Group，不影响 URL。

**核心 Hook**：`hooks/useChatStream.ts`
- 使用 `fetch + ReadableStream` 而非 `EventSource`（原因：EventSource 只支持 GET，无法携带 JSON body）
- SSE 三种事件：`token`（追加流式文本）、`done`（结束并保存消息）、`error`
- 游客模式：对话历史存 `localStorage`，每次请求携带完整 `history` 数组（后端不依赖 sessionId）

**API 层**：
- `lib/api.ts`：常规 REST 请求（owner profile、初始提示词）
- SSE 流式对话直接在 `useChatStream` 内处理，不经过 Next.js API Route（避免 SSE 被缓冲）

**Nginx**：`/api/` 请求直接代理到后端（含 SSE 关键配置 `proxy_buffering off`），其余流量到 Next.js。

### 数据库

- PostgreSQL 16 + pgvector，Flyway 管理迁移（`backend/src/main/resources/db/migration/`）
- `V1` 建表，`V2` 种子 owner 数据，`V3` 种子提示词
- `dynamic_suggestions` 表：每条 assistant 消息生成的动态建议（关联 `message_id`）
- `prompt_suggestions` 表：Owner 预设的首屏初始提示词

### Phase 说明

代码注释中 `Phase 2`/`Phase 3` 标识当前完成度：
- **Phase 2（当前）**：SSE 流式聊天可用，RAG 为 stub（空列表），向量检索未接入
- **Phase 3（规划）**：接入真实向量检索，`RagService.retrieve()` 调用 `EmbeddingService` + `KnowledgeRepository.findSimilarByOwner()`
