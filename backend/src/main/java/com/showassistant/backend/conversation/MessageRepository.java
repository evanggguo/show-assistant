package com.showassistant.backend.conversation;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TDD 5.1 — Message 数据访问层
 * 提供消息查询方法，支持历史记录加载
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * TDD 4.3.3 — 加载会话历史消息
     * 查询指定会话的最近 N 条消息，按创建时间倒序取，然后按正序返回
     */
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC")
    List<Message> findRecentByConversationId(@Param("conversationId") Long conversationId, Pageable pageable);

    /**
     * TDD 4.3.3 — 查询会话的最后一条 assistant 消息（用于获取最新 suggestions）
     */
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId AND m.role = 'assistant' ORDER BY m.createdAt DESC")
    List<Message> findLastAssistantMessage(@Param("conversationId") Long conversationId, Pageable pageable);

    /**
     * 查询会话的所有消息，按时间正序
     */
    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
