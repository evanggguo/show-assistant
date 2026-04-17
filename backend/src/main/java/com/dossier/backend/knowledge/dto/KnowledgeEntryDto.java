package com.dossier.backend.knowledge.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * TDD 4.2 — 知识条目 DTO
 * 用于 RagService 检索结果的数据传输对象
 */
@Data
@Builder
public class KnowledgeEntryDto {

    private Long id;
    private String type;
    private String title;
    private String content;
    private Double similarity;
    private OffsetDateTime createdAt;
}
