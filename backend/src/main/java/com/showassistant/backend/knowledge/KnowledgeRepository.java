package com.showassistant.backend.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TDD 5.1 — KnowledgeEntry 数据访问层
 * Phase 3 将添加向量相似度查询方法
 */
@Repository
public interface KnowledgeRepository extends JpaRepository<KnowledgeEntry, Long> {

    /**
     * TDD 4.2 — Phase 3 预留：按余弦相似度查询最近邻知识条目
     * 当前 Phase 2 不调用此方法，占位供 Phase 3 实现
     *
     * @param ownerId   Owner ID
     * @param embedding 查询向量
     * @param limit     返回条数
     * @return 相似知识条目列表
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

    /**
     * 查询指定 Owner 的所有知识条目
     */
    List<KnowledgeEntry> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
