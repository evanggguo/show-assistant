package com.dossier.backend.conversation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TDD 5.1 — DynamicSuggestion data access layer
 */
@Repository
public interface DynamicSuggestionRepository extends JpaRepository<DynamicSuggestion, Long> {

    /**
     * TDD 4.4 — Query dynamic suggestions for the specified message, sorted by sort_order ascending
     */
    List<DynamicSuggestion> findByMessageIdOrderBySortOrderAsc(Long messageId);

    void deleteByMessage_Conversation_OwnerId(Long ownerId);
}
