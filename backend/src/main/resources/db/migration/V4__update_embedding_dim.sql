-- V4__update_embedding_dim.sql
-- Phase 3: 将 knowledge_entries.embedding 维度从 1536 改为 768
-- 使用 Google text-embedding-004 模型（768 维输出）

-- 1. 删除旧的 HNSW 向量索引
DROP INDEX IF EXISTS idx_knowledge_entries_embedding;

-- 2. 清空现有 embedding 数据（维度不兼容，必须重新生成）
UPDATE knowledge_entries SET embedding = NULL;

-- 3. 修改列类型为 vector(768)
ALTER TABLE knowledge_entries
    ALTER COLUMN embedding TYPE vector(768)
    USING NULL;

-- 4. 重建 HNSW 索引（768 维，余弦相似度）
CREATE INDEX idx_knowledge_entries_embedding
    ON knowledge_entries USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
