package com.dossier.backend.chat;

import com.dossier.backend.chat.dto.ChatRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChatController.
 * Verifies that the controller correctly creates a SseEmitter and calls chatService.handleStream.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatController Unit Tests")
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatController chatController;

    @Test
    @DisplayName("POST /api/chat/stream: returns SseEmitter and chatService.handleStream is called")
    void should_return_sse_emitter_and_call_handleStream() {
        // given
        ChatRequest req = new ChatRequest(null, "hello", null);
        SseEmitter mockEmitter = mock(SseEmitter.class);
        when(chatService.createEmitter()).thenReturn(mockEmitter);

        // when
        SseEmitter result = chatController.stream(req);

        // then
        assertThat(result).isSameAs(mockEmitter);
        verify(chatService).createEmitter();
        verify(chatService).handleStream(req, mockEmitter);
    }

    @Test
    @DisplayName("POST /api/chat/stream: request with conversationId also returns SseEmitter and calls handleStream")
    void should_handle_request_with_conversation_id() {
        // given
        ChatRequest req = new ChatRequest(42L, "continue conversation", null);
        SseEmitter mockEmitter = mock(SseEmitter.class);
        when(chatService.createEmitter()).thenReturn(mockEmitter);

        // when
        SseEmitter result = chatController.stream(req);

        // then
        assertThat(result).isSameAs(mockEmitter);
        verify(chatService).handleStream(req, mockEmitter);
    }
}
