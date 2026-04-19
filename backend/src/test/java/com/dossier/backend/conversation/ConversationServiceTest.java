package com.dossier.backend.conversation;

import com.dossier.backend.common.exception.ResourceNotFoundException;
import com.dossier.backend.conversation.dto.ConversationResponse;
import com.dossier.backend.conversation.dto.MessageResponse;
import com.dossier.backend.owner.Owner;
import com.dossier.backend.owner.OwnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConversationService.
 * Covers conversation creation, message saving, history loading, and conversation detail retrieval.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConversationService Unit Tests")
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private DynamicSuggestionRepository dynamicSuggestionRepository;

    @Mock
    private OwnerRepository ownerRepository;

    @InjectMocks
    private ConversationService conversationService;

    private Owner testOwner;
    private Conversation testConversation;

    @BeforeEach
    void setUp() {
        testOwner = Owner.builder()
            .id(1L)
            .name("Test Owner")
            .build();

        testConversation = Conversation.builder()
            .id(10L)
            .owner(testOwner)
            .source("web")
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();
    }

    // ===== createConversation =====

    @Test
    @DisplayName("createConversation: throws ResourceNotFoundException when owner is not found")
    void should_throw_ResourceNotFoundException_when_owner_not_found() {
        // given
        when(ownerRepository.findById(99L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> conversationService.createConversation(99L, null))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createConversation: creates and returns a new conversation when owner exists")
    void should_create_conversation_when_owner_exists() {
        // given
        when(ownerRepository.findById(1L)).thenReturn(Optional.of(testOwner));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

        // when
        Conversation result = conversationService.createConversation(1L, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    @DisplayName("createConversation: guest conversation has null userId and source=web")
    void should_create_guest_conversation_with_null_userId() {
        // given
        when(ownerRepository.findById(1L)).thenReturn(Optional.of(testOwner));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            // Simulate DB assigning an ID
            return Conversation.builder()
                .id(10L)
                .owner(c.getOwner())
                .userId(c.getUserId())
                .source(c.getSource())
                .build();
        });

        // when
        Conversation result = conversationService.createConversation(1L, null);

        // then
        assertThat(result.getUserId()).isNull();
        assertThat(result.getSource()).isEqualTo("web");
    }

    // ===== saveUserMessage =====

    @Test
    @DisplayName("saveUserMessage: throws ResourceNotFoundException when conversation is not found")
    void should_throw_when_conversation_not_found_on_saveUserMessage() {
        // given
        when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> conversationService.saveUserMessage(999L, "hello"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("saveUserMessage: saves user message successfully")
    void should_save_user_message_successfully() {
        // given
        Message savedMsg = Message.builder().id(100L).role("user").content("hello").build();
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(testConversation));
        when(messageRepository.save(any(Message.class))).thenReturn(savedMsg);

        // when
        Message result = conversationService.saveUserMessage(10L, "hello");

        // then
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getRole()).isEqualTo("user");
        assertThat(result.getContent()).isEqualTo("hello");
        verify(messageRepository).save(any(Message.class));
    }

    // ===== saveAssistantMessage =====

    @Test
    @DisplayName("saveAssistantMessage: does not save DynamicSuggestion when suggestions list is empty")
    void should_not_save_dynamic_suggestions_when_empty() {
        // given
        Message savedMsg = Message.builder().id(200L).role("assistant").content("reply content").build();
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(testConversation));
        when(messageRepository.save(any(Message.class))).thenReturn(savedMsg);

        // when
        conversationService.saveAssistantMessage(10L, "reply content", Collections.emptyList());

        // then
        verify(dynamicSuggestionRepository, never()).save(any(DynamicSuggestion.class));
    }

    @Test
    @DisplayName("saveAssistantMessage: does not save DynamicSuggestion when suggestions is null")
    void should_not_save_dynamic_suggestions_when_null() {
        // given
        Message savedMsg = Message.builder().id(200L).role("assistant").content("reply content").build();
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(testConversation));
        when(messageRepository.save(any(Message.class))).thenReturn(savedMsg);

        // when
        conversationService.saveAssistantMessage(10L, "reply content", null);

        // then
        verify(dynamicSuggestionRepository, never()).save(any(DynamicSuggestion.class));
    }

    @Test
    @DisplayName("saveAssistantMessage: when suggestions have content, saves in order with sortOrder starting at 0")
    void should_save_dynamic_suggestions_with_correct_sort_order() {
        // given
        Message savedMsg = Message.builder().id(200L).role("assistant").content("reply content").build();
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(testConversation));
        when(messageRepository.save(any(Message.class))).thenReturn(savedMsg);
        when(dynamicSuggestionRepository.save(any(DynamicSuggestion.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        List<String> suggestions = List.of("Question A", "Question B", "Question C");

        // when
        conversationService.saveAssistantMessage(10L, "reply content", suggestions);

        // then
        ArgumentCaptor<DynamicSuggestion> captor = ArgumentCaptor.forClass(DynamicSuggestion.class);
        verify(dynamicSuggestionRepository, times(3)).save(captor.capture());

        List<DynamicSuggestion> saved = captor.getAllValues();
        assertThat(saved.get(0).getText()).isEqualTo("Question A");
        assertThat(saved.get(0).getSortOrder()).isEqualTo(0);
        assertThat(saved.get(1).getText()).isEqualTo("Question B");
        assertThat(saved.get(1).getSortOrder()).isEqualTo(1);
        assertThat(saved.get(2).getText()).isEqualTo("Question C");
        assertThat(saved.get(2).getSortOrder()).isEqualTo(2);
    }

    // ===== loadHistory =====

    @Test
    @DisplayName("loadHistory: returns messages in ascending chronological order (internally reverses DESC query result)")
    void should_return_messages_in_ascending_order() {
        // given
        // findRecentByConversationId returns DESC order; the service layer reverses it
        Message msg1 = Message.builder().id(1L).role("user").content("message 1")
            .createdAt(OffsetDateTime.now().minusMinutes(2)).build();
        Message msg2 = Message.builder().id(2L).role("assistant").content("reply 1")
            .createdAt(OffsetDateTime.now().minusMinutes(1)).build();
        Message msg3 = Message.builder().id(3L).role("user").content("message 2")
            .createdAt(OffsetDateTime.now()).build();

        // Simulate DESC query returning [msg3, msg2, msg1] (must be mutable because the service calls Collections.reverse)
        when(messageRepository.findRecentByConversationId(eq(10L), any(Pageable.class)))
            .thenReturn(new ArrayList<>(List.of(msg3, msg2, msg1)));
        when(dynamicSuggestionRepository.findByMessageIdOrderBySortOrderAsc(anyLong()))
            .thenReturn(Collections.emptyList());

        // when
        List<MessageResponse> result = conversationService.loadHistory(10L, 20);

        // then - after service reversal, order should be [msg1, msg2, msg3]
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(2).getId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("loadHistory: returns empty list when conversation has no messages")
    void should_return_empty_list_when_no_messages() {
        // given
        when(messageRepository.findRecentByConversationId(eq(10L), any(Pageable.class)))
            .thenReturn(Collections.emptyList());

        // when
        List<MessageResponse> result = conversationService.loadHistory(10L, 20);

        // then
        assertThat(result).isEmpty();
    }

    // ===== getConversation =====

    @Test
    @DisplayName("getConversation: throws ResourceNotFoundException when conversation is not found")
    void should_throw_when_conversation_not_found_on_getConversation() {
        // given
        when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> conversationService.getConversation(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getConversation: returns correct lastSuggestions from the last assistant message")
    void should_return_lastSuggestions_from_last_assistant_message() {
        // given
        Message assistantMsg = Message.builder().id(50L).role("assistant").content("AI reply").build();
        DynamicSuggestion ds1 = DynamicSuggestion.builder().id(1L).text("suggestion 1").sortOrder(0).build();
        DynamicSuggestion ds2 = DynamicSuggestion.builder().id(2L).text("suggestion 2").sortOrder(1).build();

        when(conversationRepository.findById(10L)).thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(10L))
            .thenReturn(List.of(assistantMsg));
        when(messageRepository.findLastAssistantMessage(eq(10L), any(Pageable.class)))
            .thenReturn(List.of(assistantMsg));
        when(dynamicSuggestionRepository.findByMessageIdOrderBySortOrderAsc(50L))
            .thenReturn(List.of(ds1, ds2));

        // when
        ConversationResponse response = conversationService.getConversation(10L);

        // then
        assertThat(response.getLastSuggestions()).containsExactly("suggestion 1", "suggestion 2");
    }

    @Test
    @DisplayName("getConversation: returns empty lastSuggestions when there are no assistant messages")
    void should_return_empty_lastSuggestions_when_no_assistant_message() {
        // given
        Message userMsg = Message.builder().id(30L).role("user").content("user message").build();

        when(conversationRepository.findById(10L)).thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(10L))
            .thenReturn(List.of(userMsg));
        when(messageRepository.findLastAssistantMessage(eq(10L), any(Pageable.class)))
            .thenReturn(Collections.emptyList());
        when(dynamicSuggestionRepository.findByMessageIdOrderBySortOrderAsc(30L))
            .thenReturn(Collections.emptyList());

        // when
        ConversationResponse response = conversationService.getConversation(10L);

        // then
        assertThat(response.getLastSuggestions()).isEmpty();
    }

    @Test
    @DisplayName("getConversation: returns basic conversation info correctly")
    void should_return_conversation_basic_info() {
        // given
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(10L))
            .thenReturn(Collections.emptyList());
        when(messageRepository.findLastAssistantMessage(eq(10L), any(Pageable.class)))
            .thenReturn(Collections.emptyList());

        // when
        ConversationResponse response = conversationService.getConversation(10L);

        // then
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getOwnerId()).isEqualTo(1L);
        assertThat(response.getSource()).isEqualTo("web");
    }
}
