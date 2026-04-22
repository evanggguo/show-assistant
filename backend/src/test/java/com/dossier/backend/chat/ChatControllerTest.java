package com.dossier.backend.chat;

import com.dossier.backend.chat.dto.ChatRequest;
import com.dossier.backend.security.AiTokenBudgetService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatController Unit Tests")
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private AiTokenBudgetService aiTokenBudgetService;

    @InjectMocks
    private ChatController chatController;

    @Test
    @DisplayName("POST /api/chat/stream: returns SseEmitter and chatService.handleStream is called")
    void should_return_sse_emitter_and_call_handleStream() {
        ChatRequest req = new ChatRequest(null, "hello", null);
        SseEmitter mockEmitter = mock(SseEmitter.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(chatService.createEmitter()).thenReturn(mockEmitter);

        SseEmitter result = chatController.stream(req, httpRequest);

        assertThat(result).isSameAs(mockEmitter);
        verify(chatService).createEmitter();
        verify(aiTokenBudgetService).consumeOrThrow();
    }

    @Test
    @DisplayName("POST /api/chat/stream: request with conversationId also returns SseEmitter and calls handleStream")
    void should_handle_request_with_conversation_id() {
        ChatRequest req = new ChatRequest(42L, "continue conversation", null);
        SseEmitter mockEmitter = mock(SseEmitter.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(chatService.createEmitter()).thenReturn(mockEmitter);

        SseEmitter result = chatController.stream(req, httpRequest);

        assertThat(result).isSameAs(mockEmitter);
        verify(aiTokenBudgetService).consumeOrThrow();
    }
}
