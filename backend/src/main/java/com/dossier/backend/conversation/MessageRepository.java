package com.dossier.backend.conversation;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TDD 5.1 — Message data access layer
 * Provides message query methods supporting history loading
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * TDD 4.3.3 — Load conversation history messages
     * Query the N most recent messages for a conversation, fetched in DESC order and reversed to ASC
     */
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC")
    List<Message> findRecentByConversationId(@Param("conversationId") Long conversationId, Pageable pageable);

    /**
     * TDD 4.3.3 — Query the last assistant message in a conversation (to retrieve the latest suggestions)
     */
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId AND m.role = 'assistant' ORDER BY m.createdAt DESC")
    List<Message> findLastAssistantMessage(@Param("conversationId") Long conversationId, Pageable pageable);

    /**
     * Query all messages in a conversation, sorted by time ascending
     */
    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    void deleteByConversation_OwnerId(Long ownerId);
}
