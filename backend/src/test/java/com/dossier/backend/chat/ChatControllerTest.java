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
 * ChatController 单元测试
 * 验证控制器正确创建 SseEmitter 并调用 chatService.handleStream
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatController 单元测试")
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatController chatController;

    @Test
    @DisplayName("POST /api/chat/stream：返回 SseEmitter，chatService.handleStream 被调用")
    void should_return_sse_emitter_and_call_handleStream() {
        // given
        ChatRequest req = new ChatRequest(null, "你好", null);
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
    @DisplayName("POST /api/chat/stream：有 conversationId 的请求，同样返回 SseEmitter 并调用 handleStream")
    void should_handle_request_with_conversation_id() {
        // given
        ChatRequest req = new ChatRequest(42L, "继续对话", null);
        SseEmitter mockEmitter = mock(SseEmitter.class);
        when(chatService.createEmitter()).thenReturn(mockEmitter);

        // when
        SseEmitter result = chatController.stream(req);

        // then
        assertThat(result).isSameAs(mockEmitter);
        verify(chatService).handleStream(req, mockEmitter);
    }
}
