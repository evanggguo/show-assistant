package com.dossier.backend.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TDD 5.1 — KnowledgeEntry data access layer
 * Phase 3 will add vector similarity query methods
 */
@Repository
public interface KnowledgeRepository extends JpaRepository<KnowledgeEntry, Long> {

    /**
     * TDD 4.2 — Reserved for Phase 3: query nearest-neighbor knowledge entries by cosine similarity
     * Not called in Phase 2; placeholder for Phase 3 implementation
     *
     * @param ownerId   Owner ID
     * @param embedding query vector
     * @param limit     number of results to return
     * @return list of similar knowledge entries
     */
    @Query(value = """
        SELECT * FROM knowledge_entries
        WHERE owner_id = :ownerId AND embedding IS NOT NULL
        ORDER BY embedding <=> CAST(:embedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<KnowledgeEntry> findSimilarByOwner(
        @Param("ownerId") Long ownerId,
        @Param("embedding") String embedding,
        @Param("limit") int limit
    );

    /** Full-text keyword search for entries containing the query text. */
    @Query(value = """
        SELECT * FROM knowledge_entries
        WHERE owner_id = :ownerId AND content ILIKE '%' || :keyword || '%'
        LIMIT :limit
        """, nativeQuery = true)
    List<KnowledgeEntry> findByKeyword(
        @Param("ownerId") Long ownerId,
        @Param("keyword") String keyword,
        @Param("limit") int limit
    );

    /**
     * Query all knowledge entries for the specified owner
     */
    List<KnowledgeEntry> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    void deleteByOwnerId(Long ownerId);

    void deleteBySourceDoc(Long sourceDocId);
}
