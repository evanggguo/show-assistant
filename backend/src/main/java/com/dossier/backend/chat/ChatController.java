package com.dossier.backend.chat;

import com.dossier.backend.chat.dto.ChatRequest;
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
 * TDD 4.1 — Streaming chat API controller
 * Provides an SSE (Server-Sent Events) streaming chat endpoint.
 * Supports both guest mode (no conversationId) and authenticated users.
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * TDD 4.1 — POST /api/chat/stream
     * Streaming chat entry point. Creates a SseEmitter and returns it immediately (HTTP 200)
     * while AI inference and SSE event pushing run in an async thread.
     *
     * SSE event format (TDD 4.1):
     * - event: token  data: {"text": "..."}
     * - event: done   data: {"messageId": 123, "suggestions": ["...", "..."]}
     * - event: error  data: {"code": "...", "message": "..."}
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
