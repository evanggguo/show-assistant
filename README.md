# Dossier

> 基于 AI 的个人展示助理 —— 让潜在客户通过聊天了解你的技能、履历与作品。

## 产品简介

Dossier 是为自由职业者和小团队设计的 AI 名片系统，包含两个 Web 子系统：

- **客户端（Client Portal）**：类 Gemini 风格的聊天界面，访客无需登录，直接与 AI 对话了解拥有者信息。
- **管理端（Admin Console）**：拥有者维护知识库（支持文字/文件录入）、配置个人信息与初始提示词。

## 技术栈

| 层次 | 技术 |
|------|------|
| 后端 | Java 21 + Spring Boot 3.4 + Spring AI 1.0 |
| 前端 | Next.js + React + TypeScript + Tailwind CSS |
| 数据库 | PostgreSQL 16 + pgvector |
| AI | 多提供商抽象（Ollama / Claude / Mock） |
| 部署 | Docker Compose + Nginx |

## 快速开始

### 前置要求

- Docker & Docker Compose
- （可选）Ollama，用于本地推理

### 一键启动（Mock 模式）

```bash
docker compose up -d
```

默认以 **Mock 模式**启动，无需任何 API Key，服务运行在 `http://localhost:3000`。

### 使用本地 Ollama 模型

确保 Ollama 已运行并已拉取所需模型（默认 `qwen2.5:7b`）：

```bash
ollama pull qwen2.5:7b
```

启动时切换到 Ollama 提供商：

```bash
AI_PROVIDER=ollama docker compose up -d
```

### 使用 Claude API

```bash
AI_PROVIDER=claude AI_MOCK=false ANTHROPIC_API_KEY=<your-key> docker compose up -d
```

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DB_PASSWORD` | `postgres` | PostgreSQL 密码 |
| `AI_PROVIDER` | `ollama` | AI 提供商：`ollama` / `claude` |
| `AI_MOCK` | `true` | 云端提供商模拟模式开关（`ollama` 时忽略此项） |
| `AI_OLLAMA_BASE_URL` | `http://ollama:11434` | Ollama 服务地址 |
| `AI_OLLAMA_MODEL` | `qwen2.5:1.5b` | Ollama 模型名称 |
| `ANTHROPIC_API_KEY` | `placeholder` | Claude API Key |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | 跨域允许来源 |

## 项目结构

```
dossier/
├── backend/          # Spring Boot 后端
│   └── src/main/java/com/dossier/backend/
│       ├── owner/        # 拥有者信息 & 提示词配置
│       ├── conversation/ # 会话 & 消息 & 动态提示词
│       ├── knowledge/    # 知识库 & RAG 检索
│       ├── document/     # 文件管理
│       └── common/       # 统一响应 & 异常处理
├── frontend/         # Next.js 前端
│   ├── app/          # 页面路由
│   └── components/   # UI 组件（聊天界面等）
├── docs/
│   └── db-design.md  # 数据库表设计（ER 图）
├── docker-compose.yml
└── nginx.conf        # 反向代理（SSE 长连接支持）
```

## 主要 API

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/owner/profile` | 获取拥有者信息 |
| `GET` | `/api/owner/suggestions` | 获取首屏提示词列表 |
| `POST` | `/api/conversations` | 创建新会话 |
| `POST` | `/api/conversations/{id}/messages/stream` | SSE 流式对话 |
| `GET` | `/api/conversations/{id}/messages` | 获取历史消息 |

## AI 提供商配置说明

后端使用 Spring AI 抽象层，支持运行时切换提供商：

- **Mock 模式**（默认）：返回固定模拟数据，无需外部依赖，适合开发调试。
- **Ollama**：调用本地模型，`AI_MOCK` 对此提供商无效，始终发送真实请求。
- **Claude**：调用 Anthropic API，需设置 `ANTHROPIC_API_KEY`，可通过 `AI_MOCK=true` 跳过实际调用。

不支持 Tool Use 的小模型（如部分 Ollama 模型）会自动降级到文本解析模式生成动态建议。

## 数据库

使用 Flyway 管理数据库迁移，pgvector 扩展支持向量检索（Phase 3 规划）。

详细表结构见 [docs/db-design.md](docs/db-design.md)。

## 本地开发

### 后端

```bash
cd backend
./mvnw spring-boot:run
```

需要本地运行 PostgreSQL（或通过 Docker 启动）：

```bash
docker compose up postgres -d
```

### 前端

```bash
cd frontend
npm install
npm run dev
```

前端默认访问 `http://localhost:3000`，API 请求代理到 `http://localhost:8080`。
