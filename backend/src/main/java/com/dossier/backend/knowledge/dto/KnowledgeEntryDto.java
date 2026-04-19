package com.dossier.backend.knowledge.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * TDD 4.2 — Knowledge entry DTO
 * Data transfer object for RagService retrieval results
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
