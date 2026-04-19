package com.dossier.backend.ai;

/**
 * TDD 4.2 — Vector embedding service interface
 * Defines the standard interface for text-to-vector conversion; required by Phase 3 RAG functionality
 */
public interface EmbeddingService {

    /**
     * TDD 4.2 — Convert text to a vector embedding
     * Returns an empty array on failure without throwing an exception, preserving system availability
     *
     * @param text the text to embed
     * @return vector array (float[]); returns new float[0] on failure
     */
    float[] embed(String text);
}
