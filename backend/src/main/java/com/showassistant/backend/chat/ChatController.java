package com.showassistant.backend.chat;

import com.showassistant.backend.chat.dto.ChatRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * TDD 4.1 — 流式对话 API 控制器
 * 提供基于 SSE（Server-Sent Events）的流式对话接口，
 * 支持游客（无 conversationId）和登录用户两种模式。
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * TDD 4.1 — POST /api/chat/stream
     * 流式对话入口。创建 SseEmitter 并立即返回给客户端（HTTP 200），
     * 同时在异步线程中执行 AI 推理和 SSE 事件推送。
     *
     * SSE 事件格式（TDD 4.1）：
     * - event: token  data: {"text": "..."}
     * - event: done   data: {"messageId": 123, "suggestions": ["...", "..."]}
     * - event: error  data: {"code": "...", "message": "..."}
     *
     * @param req 聊天请求（conversationId 可为 null 表示游客新会话）
     * @return SseEmitter，建立 SSE 长连接
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody ChatRequest req) {
        log.debug("Chat stream request: conversationId={}, messageLength={}",
            req.conversationId(), req.message().length());

        SseEmitter emitter = chatService.createEmitter();
        chatService.handleStream(req, emitter);
        return emitter;
    }
}
