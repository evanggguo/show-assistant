package com.dossier.backend.conversation.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * TDD 6.5.1 — Message response DTO
 * Message information exposed to clients
 */
@Data
@Builder
public class MessageResponse {

    private Long id;
    private String role;
    private String content;
    private OffsetDateTime createdAt;
    private List<String> suggestions;
}
