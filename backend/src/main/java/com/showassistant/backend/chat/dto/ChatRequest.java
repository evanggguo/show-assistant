package com.showassistant.backend.chat.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * TDD 4.1 — 流式对话请求 DTO
 * 客户端发送的聊天请求，支持游客（无 conversationId）和登录用户两种模式。
 * 游客模式下，前端负责携带 history 维护上下文；登录用户从数据库加载。
 */
public record ChatRequest(
    /**
     * 会话 ID，null 表示游客新会话，后端将自动创建会话
     */
    Long conversationId,

    /**
     * 用户发送的消息内容，不能为空
     */
    @NotBlank(message = "消息内容不能为空")
    String message,

    /**
     * 游客携带的历史消息列表（conversationId 为 null 时使用），
     * 登录用户此字段忽略，历史从数据库加载
     */
    List<HistoryMessage> history
) {
    /**
     * TDD 4.1 — 历史消息记录
     * 游客模式下前端携带的上下文历史
     */
    public record HistoryMessage(String role, String content) {}
}
