-- V4__update_embedding_dim.sql
-- Phase 3: Change knowledge_entries.embedding dimension from 1536 to 768
-- Uses the Google text-embedding-004 model (768-dim output)

-- 1. Drop the old HNSW vector index
DROP INDEX IF EXISTS idx_knowledge_entries_embedding;

-- 2. Clear existing embeddings (incompatible dimensions; must be regenerated)
UPDATE knowledge_entries SET embedding = NULL;

-- 3. Alter column type to vector(768)
ALTER TABLE knowledge_entries
    ALTER COLUMN embedding TYPE vector(768)
    USING NULL;

-- 4. Rebuild the HNSW index (768-dim, cosine similarity)
CREATE INDEX idx_knowledge_entries_embedding
    ON knowledge_entries USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
