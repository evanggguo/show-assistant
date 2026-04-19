package com.dossier.backend.owner;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TDD 5.1 — PromptSuggestion data access layer
 * Provides query methods for prompt suggestions
 */
@Repository
public interface PromptSuggestionRepository extends JpaRepository<PromptSuggestion, Long> {

    /**
     * TDD 6.4.2 — Query enabled suggestions for the specified owner, sorted by sort_order ascending
     */
    List<PromptSuggestion> findByOwnerIdAndEnabledTrueOrderBySortOrderAsc(Long ownerId);

    /**
     * Admin — Query all suggestions for the specified owner (including disabled), sorted by sort_order ascending
     */
    List<PromptSuggestion> findByOwnerIdOrderBySortOrderAsc(Long ownerId);

    void deleteByOwnerId(Long ownerId);
}
