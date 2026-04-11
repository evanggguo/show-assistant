-- V1__init_schema.sql
-- TDD 5.1 — 数据库初始化脚本

-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- owners 表：展示者/产品拥有者
CREATE TABLE IF NOT EXISTS owners (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    tagline    VARCHAR(255),
    avatar_url VARCHAR(500),
    contact    JSONB DEFAULT '{}',
    config     JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- client_users 表：来访用户（通过 SSO 登录）
CREATE TABLE IF NOT EXISTS client_users (
    id           BIGSERIAL PRIMARY KEY,
    sso_provider VARCHAR(50)  NOT NULL,
    sso_id       VARCHAR(255) NOT NULL,
    nickname     VARCHAR(100),
    avatar_url   VARCHAR(500),
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE (sso_provider, sso_id)
);

-- conversations 表：对话会话
CREATE TABLE IF NOT EXISTS conversations (
    id         BIGSERIAL PRIMARY KEY,
    owner_id   BIGINT       NOT NULL REFERENCES owners (id),
    user_id    BIGINT REFERENCES client_users (id),
    source     VARCHAR(50)  NOT NULL DEFAULT 'web',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- messages 表：对话消息
CREATE TABLE IF NOT EXISTS messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT      NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant', 'system')),
    content         TEXT        NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- dynamic_suggestions 表：动态跟进提示词（每条 assistant 消息末尾生成）
CREATE TABLE IF NOT EXISTS dynamic_suggestions (
    id         BIGSERIAL PRIMARY KEY,
    message_id BIGINT       NOT NULL REFERENCES messages (id) ON DELETE CASCADE,
    text       VARCHAR(500) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0
);

-- prompt_suggestions 表：Owner 预设的初始提示词
CREATE TABLE IF NOT EXISTS prompt_suggestions (
    id         BIGSERIAL PRIMARY KEY,
    owner_id   BIGINT       NOT NULL REFERENCES owners (id),
    text       VARCHAR(500) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE
);

-- knowledge_entries 表：知识库条目（支持向量检索）
CREATE TABLE IF NOT EXISTS knowledge_entries (
    id         BIGSERIAL PRIMARY KEY,
    owner_id   BIGINT       NOT NULL REFERENCES owners (id),
    type       VARCHAR(50)  NOT NULL DEFAULT 'text',
    title      VARCHAR(255),
    content    TEXT         NOT NULL,
    embedding  vector(1536),
    source_doc BIGINT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- documents 表：上传的源文档
CREATE TABLE IF NOT EXISTS documents (
    id         BIGSERIAL PRIMARY KEY,
    owner_id   BIGINT       NOT NULL REFERENCES owners (id),
    filename   VARCHAR(255) NOT NULL,
    file_type  VARCHAR(50),
    file_size  BIGINT,
    file_path  VARCHAR(500),
    status     VARCHAR(50)  NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- HNSW 向量索引（TDD 5.2 — 向量索引配置）
CREATE INDEX IF NOT EXISTS idx_knowledge_entries_embedding
    ON knowledge_entries USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- 普通索引优化查询
CREATE INDEX IF NOT EXISTS idx_messages_conversation_id ON messages (conversation_id, created_at);
CREATE INDEX IF NOT EXISTS idx_knowledge_entries_owner_id ON knowledge_entries (owner_id);
CREATE INDEX IF NOT EXISTS idx_prompt_suggestions_owner_id ON prompt_suggestions (owner_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_conversations_owner_id ON conversations (owner_id);
