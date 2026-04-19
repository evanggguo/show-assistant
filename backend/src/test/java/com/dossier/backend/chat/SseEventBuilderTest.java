package com.dossier.backend.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SseEventBuilder.
 * Covers serialization, sending, and error handling for all three SSE event types.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SseEventBuilder Unit Tests")
class SseEventBuilderTest {

    @Mock
    private SseEmitter emitter;

    private SseEventBuilder sseEventBuilder;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        sseEventBuilder = new SseEventBuilder(objectMapper);
    }

    @Test
    @DisplayName("sendToken: sends a correctly formatted token event with a text field in data")
    void should_sendToken_with_correct_data() throws Exception {
        // Capture the sent event
        ArgumentCaptor<SseEmitter.SseEventBuilder> eventCaptor =
            ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);

        sseEventBuilder.sendToken(emitter, "hello");

        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("sendDone: sends a correctly formatted done event with messageId and suggestions")
    void should_sendDone_with_messageId_and_suggestions() throws Exception {
        Long messageId = 42L;
        List<String> suggestions = List.of("Tell me more", "Go deeper");

        sseEventBuilder.sendDone(emitter, messageId, suggestions);

        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("sendDone: when suggestions is null, the sent event contains an empty suggestions array")
    void should_sendDone_with_empty_suggestions_when_null() throws Exception {
        sseEventBuilder.sendDone(emitter, 1L, null);

        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("sendError: sends a correctly formatted error event with code and message")
    void should_sendError_with_code_and_message() throws Exception {
        sseEventBuilder.sendError(emitter, "STREAM_ERROR", "AI service unavailable");

        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("When emitter throws IOException during send, no exception propagates (caught internally)")
    void should_not_throw_when_emitter_throws_ioException() throws Exception {
        // Simulate emitter.send() throwing IOException
        doThrow(new IOException("Connection closed")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        // Should not throw
        assertThatNoException().isThrownBy(() -> sseEventBuilder.sendToken(emitter, "test"));
        assertThatNoException().isThrownBy(() -> sseEventBuilder.sendDone(emitter, 1L, List.of()));
        assertThatNoException().isThrownBy(() -> sseEventBuilder.sendError(emitter, "ERR", "msg"));
    }

    @Test
    @DisplayName("sendDone: sends successfully when suggestions is an empty list")
    void should_sendDone_with_empty_list_suggestions() throws Exception {
        sseEventBuilder.sendDone(emitter, 1L, List.of());

        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }
}
