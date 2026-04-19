-- V1__init_schema.sql
-- TDD 5.1 — Database initialization script

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- owners table: portfolio owner
CREATE TABLE IF NOT EXISTS owners (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    tagline    VARCHAR(255),
    avatar_url VARCHAR(500),
    contact    JSONB DEFAULT '{}',
    config     JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- client_users table: visiting users (login via SSO)
CREATE TABLE IF NOT EXISTS client_users (
    id           BIGSERIAL PRIMARY KEY,
    sso_provider VARCHAR(50)  NOT NULL,
    sso_id       VARCHAR(255) NOT NULL,
    nickname     VARCHAR(100),
    avatar_url   VARCHAR(500),
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE (sso_provider, sso_id)
);

-- conversations table: chat sessions
CREATE TABLE IF NOT EXISTS conversations (
    id         BIGSERIAL PRIMARY KEY,
    owner_id   BIGINT       NOT NULL REFERENCES owners (id),
    user_id    BIGINT REFERENCES client_users (id),
    source     VARCHAR(50)  NOT NULL DEFAULT 'web',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- messages table: chat messages
CREATE TABLE IF NOT EXISTS messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT      NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant', 'system')),
    content         TEXT        NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- dynamic_suggestions table: AI-generated follow-up suggestions (appended at the end of each assistant message)
CREATE TABLE IF NOT EXISTS dynamic_suggestions (
    id         BIGSERIAL PRIMARY KEY,
    message_id BIGINT       NOT NULL REFERENCES messages (id) ON DELETE CASCADE,
    text       VARCHAR(500) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0
);

-- prompt_suggestions table: owner-preset initial prompt suggestions
CREATE TABLE IF NOT EXISTS prompt_suggestions (
    id         BIGSERIAL PRIMARY KEY,
    owner_id   BIGINT       NOT NULL REFERENCES owners (id),
    text       VARCHAR(500) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE
);

-- knowledge_entries table: knowledge base entries (supports vector retrieval)
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

-- documents table: uploaded source documents
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

-- HNSW vector index (TDD 5.2 — vector index configuration)
CREATE INDEX IF NOT EXISTS idx_knowledge_entries_embedding
    ON knowledge_entries USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- Regular indexes for query optimization
CREATE INDEX IF NOT EXISTS idx_messages_conversation_id ON messages (conversation_id, created_at);
CREATE INDEX IF NOT EXISTS idx_knowledge_entries_owner_id ON knowledge_entries (owner_id);
CREATE INDEX IF NOT EXISTS idx_prompt_suggestions_owner_id ON prompt_suggestions (owner_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_conversations_owner_id ON conversations (owner_id);
