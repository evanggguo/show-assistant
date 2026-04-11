package com.showassistant.backend.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TDD 5.1 — Document 数据访问层
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * 查询指定 Owner 的所有文档，按创建时间倒序
     */
    List<Document> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    /**
     * 查询待处理的文档（Phase 3 后台处理用）
     */
    List<Document> findByStatus(DocumentStatus status);
}
