package com.showassistant.backend.conversation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * TDD 5.1 — Conversation 数据访问层
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
}
