package com.showassistant.backend.conversation;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TDD 5.1 — DynamicSuggestion 实体
 * 由 AI 模型在 assistant 消息末尾通过 Tool Use 动态生成的跟进提示词
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dynamic_suggestions")
public class DynamicSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "text", nullable = false, length = 500)
    private String text;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
