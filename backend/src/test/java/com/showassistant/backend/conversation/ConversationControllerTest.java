package com.showassistant.backend.conversation;

import com.showassistant.backend.common.exception.ResourceNotFoundException;
import com.showassistant.backend.common.response.ApiResponse;
import com.showassistant.backend.conversation.dto.ConversationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ConversationController 单元测试
 * 验证控制器对会话查询的处理，包括正常返回和资源不存在场景
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConversationController 单元测试")
class ConversationControllerTest {

    @Mock
    private ConversationService conversationService;

    @InjectMocks
    private ConversationController conversationController;

    @Test
    @DisplayName("GET /api/conversations/{id}：正常返回 ConversationResponse")
    void should_return_conversation_response_when_found() {
        // given
        ConversationResponse response = ConversationResponse.builder()
            .id(10L)
            .ownerId(1L)
            .source("web")
            .messages(Collections.emptyList())
            .lastSuggestions(Collections.emptyList())
            .build();
        when(conversationService.getConversation(10L)).thenReturn(response);

        // when
        ApiResponse<ConversationResponse> result = conversationController.getConversation(10L);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isSameAs(response);
        assertThat(result.getData().getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("GET /api/conversations/{id}：会话不存在时抛出 ResourceNotFoundException（由全局异常处理器处理为 404）")
    void should_throw_when_conversation_not_found() {
        // given
        when(conversationService.getConversation(999L))
            .thenThrow(new ResourceNotFoundException("Conversation", 999L));

        // when / then
        assertThatThrownBy(() -> conversationController.getConversation(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
