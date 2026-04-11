# Show Assistant — 开发日志

---

## 2026-04-10 | 后端初始化（Phase 2 完成）

### 完成内容

完成后端 Spring Boot 项目的初始搭建，目标是跑通 TDD 第 4 节描述的核心链路：**流式对话 + Tool Use 动态提示词**（Phase 2，无 RAG）。

#### 生成文件概览

| 类别 | 文件数 | 说明 |
|------|--------|------|
| 配置类 | 4 | SecurityConfig、CorsConfig、AsyncConfig、AiConfig |
| 对话核心 | 5 | ChatController、ChatService、PromptAssembler、SseEventBuilder、SuggestFollowupsTool |
| 会话模块 | 7 | Conversation/Message/DynamicSuggestion 实体 + Repository + Service + Controller |
| Owner 模块 | 6 | Owner/PromptSuggestion 实体 + Repository + Service + Controller + DTO |
| 知识库模块 | 5 | KnowledgeEntry、KnowledgeType、KnowledgeRepository、KnowledgeService、RagService |
| 文档模块 | 3 | Document、DocumentStatus、DocumentRepository |
| AI 服务层 | 2 | EmbeddingService（接口）、SpringAiEmbeddingService |
| 公共模块 | 4 | ApiResponse、BusinessException、ResourceNotFoundException、GlobalExceptionHandler |
| 数据库迁移 | 3 | V1 建表、V2 初始化 Owner、V3 初始化提示词 |
| 配置文件 | 2 | application.yml、pom.xml |
| **合计** | **43 Java + 配置** | `mvn compile` BUILD SUCCESS (2.7s) |

---

### 关键设计实现（对应 TDD）

**TDD 4.3 — 流式对话 SSE 协议**
- 三种事件类型：`token`（逐字）、`done`（含 messageId + suggestions）、`error`
- 前端用 `fetch + ReadableStream` 接收（而非 EventSource，原因：EventSource 只支持 GET）
- Spring Boot 侧使用 `SseEmitter`（非 WebFlux），配合专用线程池（`sseTaskExecutor`）

**TDD 4.4 — 动态提示词 Tool Use 方案**
- 使用 Spring AI `@Tool` 注解定义 `suggest_followups` 工具
- `SuggestFollowupsTool` 为**非 Spring Bean**，每次请求 `new` 一个实例，避免状态共享
- 工具参数名为 `suggestions`（与 TDD JSON Schema 设计一致）
- 通过 `chatClient.prompt().tools(suggestTool).stream().content()` 注册并订阅

**TDD 4.2 — RAG（Phase 2 stub）**
- `RagService.retrieve()` 直接返回 `Collections.emptyList()`
- `PromptAssembler` 在 RAG 上下文为空时不注入"参考资料"段落

**TDD 5 — 数据库**
- Flyway 管理 DDL，`ddl-auto: validate`（JPA 只验证，不自动变更）
- `knowledge_entries.embedding` 为 `vector(1536)` 列，已在 V1 迁移中建好 HNSW 索引
- JPA 实体中 `embedding` 字段暂标为 `@Transient`（Phase 3 接入 pgvector 时替换）

---

### 过程中解决的问题

**1. Spring AI 1.0.0 artifact ID 变更**
- 旧命名：`spring-ai-anthropic-spring-boot-starter`
- 正确命名：`spring-ai-starter-model-anthropic`
- 原因：Spring AI 1.0.0 统一了命名规范

**2. Maven Central 不可达**
- 现象：连接超时，DNS 解析到 `198.18.0.169`（保留段，被拦截）
- 解决：创建 `~/.m2/settings.xml`，配置阿里云 Maven 镜像
  ```xml
  <mirror>
    <id>aliyun-central</id>
    <mirrorOf>central</mirrorOf>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
  ```

**3. spring-boot-starter-security 本地无缓存**
- 本地 Maven 仓库中该依赖未缓存，且阿里云镜像当时未完全同步
- 解决：Phase 2 暂时注释该依赖，`SecurityConfig` 改为空占位实现
- 恢复方式：待网络正常后取消 `pom.xml` 中注释，恢复 `SecurityConfig` 完整实现

**4. KnowledgeEntry.embedding 类型映射**
- `@JdbcTypeCode(SqlTypes.VECTOR)` + pgvector Hibernate 集成在 Phase 2 不必要
- 若 pgvector 未正确注册到 Hibernate，可能导致启动失败
- 解决：Phase 2 用 `@Transient` 绕过，Phase 3 再替换为完整向量映射

**5. SuggestFollowupsTool 参数名不一致**
- 初始生成代码使用 `questions`，与 TDD 4.4.2 JSON Schema 设计的 `suggestions` 不符
- Spring AI 用参数名生成 JSON Schema，模型会收到错误的字段名
- 解决：修正为 `suggestions`

---

---

## 2026-04-10 | 前端初始化（Phase 2 完成）

### 完成内容

| 文件 | 说明 |
|------|------|
| `hooks/useChatStream.ts` | 核心 SSE Hook，fetch+ReadableStream 解析 token/done/error |
| `lib/types.ts` | 全部 TypeScript 类型（Message、OwnerProfile、ChatRequest 等） |
| `lib/api.ts` | fetchOwnerProfile / fetchInitialSuggestions REST 封装 |
| `lib/storage.ts` | localStorage 游客历史管理，最多 20 条，SSR 安全 |
| `components/chat/ChatPage.tsx` | 主容器，首屏↔对话切换，加载 API 数据，错误提示 |
| `components/chat/MessageList.tsx` | 消息列表，新消息后自动滚动到底部 |
| `components/chat/MessageBubble.tsx` | user 右对齐灰色气泡；assistant 左对齐白色气泡 + Markdown 渲染 |
| `components/chat/StreamingBubble.tsx` | 流式气泡，末尾光标 animate-pulse 闪烁 |
| `components/chat/SuggestionCards.tsx` | 横向可滚动提示词卡片，点击触发发送 |
| `components/chat/ChatInput.tsx` | textarea，Enter 发送/Shift+Enter 换行，流式中禁用 |
| `components/OwnerProfile.tsx` | hero（首屏大头像）/ compact（对话中顶部行内）两种模式 |

### 过程中解决的问题

| 问题 | 原因 | 解决 |
|------|------|------|
| SSE 接口路径错误 `/api/chat` | 生成代码漏掉了 `/stream` 后缀 | 修正为 `/api/chat/stream` |
| `prose` 类不生效 | Tailwind v4 需要显式注册 typography 插件 | 安装 `@tailwindcss/typography`，在 globals.css 加 `@plugin` 指令 |
| Google Fonts 构建失败 | 网络不通，无法下载 Geist 字体 | layout.tsx 改用系统字体栈（-apple-system 等） |

### 启动方式

```bash
cd frontend && npm run dev   # 开发模式，访问 http://localhost:3000/chat
```

后端需同时运行在 `http://localhost:8080`（见后端启动方式）。

---

### 当前 Phase 状态

| Phase | 状态 | 说明 |
|-------|------|------|
| Phase 1 — 基础设施 | ✅ 完成 | 后端骨架、Flyway 迁移、Config 类 |
| Phase 2 — 流式对话核心 | ✅ 完成 | 后端 SSE + Tool Use；前端 useChatStream + Chat UI |
| Phase 3 — RAG 接入 | 🔲 待做 | RagService stub 替换为真实向量检索 + pgvector |
| Phase 4 — 首屏体验完善 | 🔲 待做 | SSO 登录、历史会话恢复、主题切换 |
| Phase 5 — 管理端 | 🔲 待做 | 知识库录入、文件上传、Security 鉴权 |

---

### 启动方式

```bash
# 启动 PostgreSQL（pgvector 镜像）
docker run -d --name showassistant-pg \
  -e POSTGRES_DB=showassistant \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  pgvector/pgvector:pg16

# 启动后端
export ANTHROPIC_API_KEY=sk-ant-xxx
cd backend && mvn spring-boot:run
```

**验证核心 SSE 接口：**
```bash
curl -N -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "你好，请介绍一下你的经历"}' \
  --no-buffer
```

预期输出：
```
event: token
data: {"text":"你好"}

event: token
data: {"text":"！我"}

...

event: done
data: {"messageId":1,"suggestions":["他擅长哪些技术？","有哪些项目案例？","如何联系他？"]}
```
