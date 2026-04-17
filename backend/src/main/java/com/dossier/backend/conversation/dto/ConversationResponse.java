package com.dossier.backend.conversation.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * TDD 6.5.1 — 会话详情响应 DTO
 * 包含会话基本信息、完整消息历史和最新动态提示词
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
