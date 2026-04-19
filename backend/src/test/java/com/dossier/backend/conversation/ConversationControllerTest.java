package com.dossier.backend.conversation;

import com.dossier.backend.common.exception.ResourceNotFoundException;
import com.dossier.backend.common.response.ApiResponse;
import com.dossier.backend.conversation.dto.ConversationResponse;
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
 * Unit tests for ConversationController.
 * Verifies controller handling of conversation retrieval, including normal and not-found scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConversationController Unit Tests")
class ConversationControllerTest {

    @Mock
    private ConversationService conversationService;

    @InjectMocks
    private ConversationController conversationController;

    @Test
    @DisplayName("GET /api/conversations/{id}: returns ConversationResponse correctly")
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
    @DisplayName("GET /api/conversations/{id}: throws ResourceNotFoundException when conversation is not found (handled as 404 by global exception handler)")
    void should_throw_when_conversation_not_found() {
        // given
        when(conversationService.getConversation(999L))
            .thenThrow(new ResourceNotFoundException("Conversation", 999L));

        // when / then
        assertThatThrownBy(() -> conversationController.getConversation(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
