package com.dossier.backend.conversation.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * TDD 6.5.1 — Conversation detail response DTO
 * Contains basic conversation info, full message history, and the latest dynamic suggestions
 */
@Data
@Builder
public class ConversationResponse {

    private Long id;
    private Long ownerId;
    private String source;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<MessageResponse> messages;
    private List<String> lastSuggestions;
}
