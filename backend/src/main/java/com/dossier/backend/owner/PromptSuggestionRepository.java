package com.dossier.backend.owner;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TDD 5.1 — PromptSuggestion 数据访问层
 * 提供提示词的查询方法
 */
@Repository
public interface PromptSuggestionRepository extends JpaRepository<PromptSuggestion, Long> {

    /**
     * TDD 6.4.2 — 查询指定 Owner 的启用提示词，按 sort_order 升序排列
     */
    List<PromptSuggestion> findByOwnerIdAndEnabledTrueOrderBySortOrderAsc(Long ownerId);

    /**
     * 管理端 — 查询指定 Owner 的所有提示词（含禁用），按 sort_order 升序排列
     */
    List<PromptSuggestion> findByOwnerIdOrderBySortOrderAsc(Long ownerId);

    void deleteByOwnerId(Long ownerId);
}
