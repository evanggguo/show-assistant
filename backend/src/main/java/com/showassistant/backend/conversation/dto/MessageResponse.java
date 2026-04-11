package com.showassistant.backend.conversation.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * TDD 6.5.1 — 消息响应 DTO
 * 对外暴露的消息信息
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
