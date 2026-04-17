package com.dossier.backend.owner;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TDD 5.1 — PromptSuggestion 实体
 * Owner 预设的初始引导提示词，展示在对话界面供访客快速选择
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "prompt_suggestions")
public class PromptSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @Column(name = "text", nullable = false, length = 500)
    private String text;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;
}
