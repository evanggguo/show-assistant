package com.dossier.backend.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * TDD 4.1 — SSE 事件构建器
 * 封装 SSE 事件的序列化和发送，统一管理三种事件类型：
 * - event: token  — 流式文本 token
 * - event: done   — 流结束，含 messageId 和 suggestions
 * - event: error  — 错误事件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseEventBuilder {

    private final ObjectMapper objectMapper;

    /**
     * TDD 4.1.1 — 发送 token 事件
     * 将单个文本 token 推送给客户端
     *
     * @param emitter SSE 连接
     * @param text    当前 token 文本
     */
    public void sendToken(SseEmitter emitter, String text) {
        send(emitter, "token", Map.of("text", text));
    }

    /**
     * TDD 4.1.2 — 发送 done 事件
     * 流式回复结束时推送，包含保存后的消息 ID 和动态 suggestions
     *
     * @param emitter     SSE 连接
     * @param messageId   保存到数据库的 assistant 消息 ID
     * @param suggestions AI 动态生成的跟进提示词列表
     */
    public void sendDone(SseEmitter emitter, Long messageId, List<String> suggestions) {
        send(emitter, "done", Map.of(
            "messageId", messageId,
            "suggestions", suggestions != null ? suggestions : List.of()
        ));
    }

    /**
     * TDD 4.1.3 — 发送 error 事件
     * 发生错误时推送错误码和描述信息
     *
     * @param emitter SSE 连接
     * @param code    错误码
     * @param message 用户可读的错误描述
     */
    public void sendError(SseEmitter emitter, String code, String message) {
        send(emitter, "error", Map.of("code", code, "message", message));
    }

    /**
     * 内部通用发送方法，将 Map 序列化为 JSON 并构建 SSE 事件
     */
    private void send(SseEmitter emitter, String eventName, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            emitter.send(
                SseEmitter.event()
                    .name(eventName)
                    .data(json)
            );
        } catch (Exception e) {
            log.error("Failed to send SSE event '{}': {}", eventName, e.getMessage());
        }
    }
}
