package com.showassistant.backend.knowledge;

import com.showassistant.backend.owner.Owner;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Transient;

import java.time.OffsetDateTime;

/**
 * TDD 5.1 — KnowledgeEntry 实体
 * 知识库中的单条知识条目，支持向量嵌入以实现语义检索（Phase 3）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "knowledge_entries")
public class KnowledgeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @Column(name = "type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private KnowledgeType type;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 向量嵌入，用于 Phase 3 语义检索。
     * Phase 2: @Transient 跳过 JPA 映射，数据库列由 Flyway 创建但暂不通过 ORM 操作。
     * Phase 3: 移除 @Transient，改用 @JdbcTypeCode(SqlTypes.VECTOR) + @Array(length=1536)。
     */
    @Transient
    private float[] embedding;

    @Column(name = "source_doc")
    private Long sourceDoc;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
