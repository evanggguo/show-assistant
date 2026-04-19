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
 * TDD 4.1 — SSE event builder
 * Encapsulates SSE event serialisation and sending for three event types:
 * - event: token  — streaming text token
 * - event: done   — stream ended, includes messageId and suggestions
 * - event: error  — error event
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseEventBuilder {

    private final ObjectMapper objectMapper;

    /** TDD 4.1.1 — Send a token event with a single text token. */
    public void sendToken(SseEmitter emitter, String text) {
        send(emitter, "token", Map.of("text", text));
    }

    /** TDD 4.1.2 — Send a done event when streaming finishes, including the saved message ID and dynamic suggestions. */
    public void sendDone(SseEmitter emitter, Long messageId, List<String> suggestions) {
        send(emitter, "done", Map.of(
            "messageId", messageId,
            "suggestions", suggestions != null ? suggestions : List.of()
        ));
    }

    /** TDD 4.1.3 — Send an error event with an error code and human-readable description. */
    public void sendError(SseEmitter emitter, String code, String message) {
        send(emitter, "error", Map.of("code", code, "message", message));
    }

    /** Internal helper: serialises data to JSON and sends an SSE event. */
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
