package com.dossier.backend.conversation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * TDD 5.1 — Conversation data access layer
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    void deleteByOwnerId(Long ownerId);
}
