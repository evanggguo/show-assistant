package com.dossier.backend.knowledge;

/**
 * TDD 5.1 — Knowledge entry type enum
 * Defines the classification of entries in the knowledge base
 */
public enum KnowledgeType {
    /** Plain text content */
    TEXT,
    /** FAQ question-answer pair */
    FAQ,
    /** Content extracted from a document */
    DOCUMENT_CHUNK,
    /** Structured data */
    STRUCTURED
}
