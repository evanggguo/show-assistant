package com.dossier.backend.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TDD 5.1 — Document data access layer
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * Query all documents for the specified owner, sorted by creation time descending
     */
    List<Document> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    /**
     * Query documents awaiting processing (used by Phase 3 background processor)
     */
    List<Document> findByStatus(DocumentStatus status);

    void deleteByOwnerId(Long ownerId);
}
