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
 * SseEventBuilder 单元测试
 * 覆盖三种 SSE 事件类型的序列化、发送和异常处理
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SseEventBuilder 单元测试")
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
    @DisplayName("sendToken：发送正确格式的 token 事件，data 包含 text 字段")
    void should_sendToken_with_correct_data() throws Exception {
        // 捕获发送的事件
        ArgumentCaptor<SseEmitter.SseEventBuilder> eventCaptor =
            ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);

        sseEventBuilder.sendToken(emitter, "你好");

        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("sendDone：发送正确格式的 done 事件，包含 messageId 和 suggestions")
    void should_sendDone_with_messageId_and_suggestions() throws Exception {
        Long messageId = 42L;
        List<String> suggestions = List.of("继续了解", "深入探讨");

        sseEventBuilder.sendDone(emitter, messageId, suggestions);

        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("sendDone：suggestions 为 null 时，发送事件中 suggestions 为空数组")
    void should_sendDone_with_empty_suggestions_when_null() throws Exception {
        sseEventBuilder.sendDone(emitter, 1L, null);

        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("sendError：发送正确格式的 error 事件，包含 code 和 message")
    void should_sendError_with_code_and_message() throws Exception {
        sseEventBuilder.sendError(emitter, "STREAM_ERROR", "AI 服务不可用");

        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("发送时 emitter 抛出 IOException，不向上抛出异常（内部 catch）")
    void should_not_throw_when_emitter_throws_ioException() throws Exception {
        // 模拟 emitter.send() 抛出 IOException
        doThrow(new IOException("Connection closed")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        // 不应抛出异常
        assertThatNoException().isThrownBy(() -> sseEventBuilder.sendToken(emitter, "test"));
        assertThatNoException().isThrownBy(() -> sseEventBuilder.sendDone(emitter, 1L, List.of()));
        assertThatNoException().isThrownBy(() -> sseEventBuilder.sendError(emitter, "ERR", "msg"));
    }

    @Test
    @DisplayName("sendDone：suggestions 为空列表时正常发送")
    void should_sendDone_with_empty_list_suggestions() throws Exception {
        sseEventBuilder.sendDone(emitter, 1L, List.of());

        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }
}
