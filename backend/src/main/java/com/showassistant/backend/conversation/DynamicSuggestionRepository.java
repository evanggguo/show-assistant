package com.showassistant.backend.conversation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TDD 5.1 — DynamicSuggestion 数据访问层
 */
@Repository
public interface DynamicSuggestionRepository extends JpaRepository<DynamicSuggestion, Long> {

    /**
     * TDD 4.4 — 查询指定消息的动态提示词，按 sort_order 升序
     */
    List<DynamicSuggestion> findByMessageIdOrderBySortOrderAsc(Long messageId);
}
