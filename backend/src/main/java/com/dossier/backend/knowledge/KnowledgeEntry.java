package com.dossier.backend.knowledge;

import com.dossier.backend.common.VectorType;
import com.dossier.backend.owner.Owner;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

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
     * 向量嵌入，用于语义检索（768 维，gemini-embedding-001）。
     * 使用自定义 VectorType 通过 PGvector 写入 PostgreSQL vector 列。
     */
    @Type(VectorType.class)
    @Column(name = "embedding", columnDefinition = "vector(768)")
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
