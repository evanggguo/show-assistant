package com.dossier.backend.chat.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * TDD 4.1 — Streaming chat request DTO
 * Supports both guest mode (no conversationId) and authenticated users.
 * In guest mode the frontend carries the history array; for logged-in users it is loaded from the DB.
 */
public record ChatRequest(
    /** Conversation ID. Null means a new guest conversation; the backend will create one automatically. */
    Long conversationId,

    @NotBlank(message = "Message content must not be blank")
    String message,

    /**
     * History messages carried by the guest (used when conversationId is null).
     * Ignored for logged-in users; their history is loaded from the database.
     */
    List<HistoryMessage> history
) {
    /** TDD 4.1 — History message record for guest-mode context. */
    public record HistoryMessage(String role, String content) {}
}
