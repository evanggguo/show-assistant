# Show Assistant — 部署指南

## 目录

1. [环境要求](#1-环境要求)
2. [快速部署（Docker Compose）](#2-快速部署docker-compose)
3. [本地开发启动](#3-本地开发启动)
4. [环境变量说明](#4-环境变量说明)
5. [服务健康检查](#5-服务健康检查)
6. [常见问题](#6-常见问题)

---

## 1. 环境要求

| 工具 | 最低版本 | 说明 |
|------|----------|------|
| Docker | 24.0+ | 容器运行时 |
| Docker Compose | 2.20+ | 已内置于 Docker Desktop |
| Git | 任意 | 拉取代码 |

> 本地开发模式还需要：JDK 21、Maven 3.9+、Node.js 20+

---

## 2. 快速部署（Docker Compose）

### 2.1 克隆代码

```bash
git clone https://github.com/evanggguo/show-assistant.git
cd show-assistant
```

### 2.2 创建环境变量文件

```bash
cp .env.example .env
# 编辑 .env，填入真实值
```

`.env` 文件内容示例见[第 4 节](#4-环境变量说明)。

### 2.3 一键启动

```bash
docker compose up -d
```

首次启动会自动构建镜像，约需 3～5 分钟（取决于网速）。

### 2.4 验证启动

```bash
docker compose ps
```

三个服务均显示 `healthy` 或 `running` 后，访问：

- 前端：<http://localhost:3000/chat>
- 后端健康端点：<http://localhost:8080/actuator/health>

### 2.5 停止服务

```bash
docker compose down          # 保留数据卷
docker compose down -v       # 同时删除数据卷（清空数据库）
```

### 2.6 更新部署

```bash
git pull
docker compose up -d --build
```

---

## 3. 本地开发启动

本地开发不需要 Docker，各服务独立启动，修改后热重载。

### 3.1 启动 PostgreSQL

```bash
docker run -d --name showassistant-pg \
  -e POSTGRES_DB=showassistant \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  pgvector/pgvector:pg16
```

### 3.2 启动后端

```bash
export ANTHROPIC_API_KEY=sk-ant-xxxx
cd backend
mvn spring-boot:run
```

后端默认监听 `http://localhost:8080`。

> **Maven 镜像加速**（国内网络）：在 `~/.m2/settings.xml` 中配置阿里云镜像：
> ```xml
> <settings>
>   <mirrors>
>     <mirror>
>       <id>aliyun-central</id>
>       <mirrorOf>central</mirrorOf>
>       <url>https://maven.aliyun.com/repository/public</url>
>     </mirror>
>   </mirrors>
> </settings>
> ```

### 3.3 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认监听 `http://localhost:3000`。

---

## 4. 环境变量说明

在项目根目录创建 `.env` 文件（`.env` 已加入 `.gitignore`，不会提交到 Git）：

```dotenv
# ── 必填 ──────────────────────────────────────────────────────────────
# Anthropic API Key，从 https://console.anthropic.com 获取
ANTHROPIC_API_KEY=sk-ant-xxxx

# ── 可选（有默认值）────────────────────────────────────────────────────
# 数据库密码，默认 postgres
DB_PASSWORD=postgres

# 前端访问后端的地址
# - 本地 Docker Compose：默认 http://localhost:8080
# - 生产服务器：填写服务器公网 IP 或域名，如 https://api.example.com
NEXT_PUBLIC_API_URL=http://localhost:8080
```

> **注意**：`NEXT_PUBLIC_API_URL` 在 Next.js 构建时会被内联到前端代码中，修改后需要重新构建前端镜像（`docker compose up -d --build frontend`）。

---

## 5. 服务健康检查

| 服务 | 检查地址 | 预期响应 |
|------|----------|----------|
| PostgreSQL | `pg_isready -U postgres -d showassistant` | `accepting connections` |
| 后端 | `http://localhost:8080/actuator/health` | `{"status":"UP"}` |
| 前端 | `http://localhost:3000/chat` | HTTP 200 |

**验证核心 SSE 接口：**

```bash
curl -N -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "你好，介绍一下你自己"}' \
  --no-buffer
```

预期输出：

```
event: token
data: {"text":"你"}

event: token
data: {"text":"好"}

...

event: done
data: {"messageId":1,"suggestions":["他擅长哪些技术？","有哪些项目案例？","如何联系他？"]}
```

---

## 6. 常见问题

### Q: 前端页面空白或 API 请求失败

检查 `NEXT_PUBLIC_API_URL` 是否正确。如果前端和后端部署在不同机器，需要填写后端的实际可访问地址，并确保 CORS 配置允许前端域名。

### Q: 后端启动报 `FlywayException: Validate failed`

数据库 schema 与 Flyway 迁移脚本不一致。常见原因是手动修改了数据库表结构。解决方法：

```bash
# 清空数据库并重建（会丢失数据）
docker compose down -v
docker compose up -d
```

### Q: `docker compose up` 卡在后端构建阶段（Maven 下载依赖超时）

国内网络访问 Maven Central 可能超时。在 `backend/Dockerfile` 的构建阶段中，Maven 会使用容器内的默认镜像源。可临时在 `backend/Dockerfile` 中挂载本地 `.m2` 缓存：

```bash
# 确保本地已有 ~/.m2 缓存，然后构建时挂载
docker build --build-arg MAVEN_OPTS="-Dmaven.repo.remote=https://maven.aliyun.com/repository/public" ./backend
```

或者先在本地运行 `mvn dependency:go-offline` 缓存依赖，再构建 Docker 镜像。

### Q: 前端镜像构建失败（Next.js standalone 模式）

确认 `frontend/next.config.ts` 中包含 `output: 'standalone'` 配置。否则构建产物中不会生成 `.next/standalone` 目录，导致 `COPY --from=builder /app/.next/standalone ./` 失败。

### Q: 修改后端代码后如何重新部署

```bash
docker compose up -d --build backend
```

只重新构建并重启后端服务，不影响数据库和前端。
