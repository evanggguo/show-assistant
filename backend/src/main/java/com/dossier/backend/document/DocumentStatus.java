package com.dossier.backend.document;

/**
 * TDD 5.1 — Document processing status enum
 * Tracks the processing progress of uploaded documents
 */
public enum DocumentStatus {
    /** Upload complete, awaiting processing */
    PENDING,
    /** Parsing and chunking in progress */
    PROCESSING,
    /** Processing complete; knowledge entries generated */
    COMPLETED,
    /** Processing failed */
    FAILED
}
