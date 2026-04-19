package com.dossier.backend.chat;

import com.dossier.backend.ai.provider.AiChatProvider;
import com.dossier.backend.chat.dto.ChatRequest;
import com.dossier.backend.common.exception.ResourceNotFoundException;
import com.dossier.backend.conversation.Conversation;
import com.dossier.backend.conversation.ConversationService;
import com.dossier.backend.conversation.Message;
import com.dossier.backend.conversation.dto.MessageResponse;
import com.dossier.backend.knowledge.RagService;
import com.dossier.backend.owner.Owner;
import com.dossier.backend.owner.OwnerService;
import com.dossier.backend.owner.dto.OwnerProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChatService.
 * Covers the full handleStream business flow, including guest mode, logged-in user mode, normal completion, and error paths.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ChatService Unit Tests")
class ChatServiceTest {

    @Mock
    private AiChatProvider aiChatProvider;

    @Mock
    private ConversationService conversationService;

    @Mock
    private OwnerService ownerService;

    @Mock
    private RagService ragService;

    @Mock
    private PromptAssembler promptAssembler;

    @Mock
    private SseEventBuilder sseEventBuilder;

    @InjectMocks
    private ChatService chatService;

    @Mock
    private SseEmitter emitter;

    private Owner testOwner;
    private Conversation testConversation;
    private OwnerProfileResponse ownerProfile;

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

        ownerProfile = OwnerProfileResponse.builder()
            .id(1L)
            .name("Test Owner")
            .tagline("test tagline")
            .build();

        when(ownerService.getOwnerProfile(anyLong())).thenReturn(ownerProfile);
        when(promptAssembler.assemble(any(), anyList(), anyBoolean(), any())).thenReturn("System prompt");
        when(ragService.retrieve(anyLong(), anyString())).thenReturn(Collections.emptyList());
        when(aiChatProvider.providerName()).thenReturn("mock");
    }

    // ===== Guest new conversation =====

    @Test
    @DisplayName("handleStream: guest new conversation (conversationId=null), happy path — creates new conversation")
    void should_create_new_conversation_for_guest_when_conversationId_is_null() {
        // given
        ChatRequest req = new ChatRequest(null, "hello", null);
        Message savedMsg = Message.builder().id(100L).role("assistant").content("hello world").build();

        when(conversationService.createConversation(1L, null)).thenReturn(testConversation);
        when(conversationService.saveUserMessage(anyLong(), anyString()))
            .thenReturn(Message.builder().id(50L).build());
        when(conversationService.saveAssistantMessage(anyLong(), anyString(), anyList()))
            .thenReturn(savedMsg);
        when(aiChatProvider.streamChat(anyList())).thenReturn(Flux.just("hello", " world"));

        // when
        chatService.handleStream(req, emitter);

        // then
        verify(conversationService).createConversation(1L, null);
        verify(conversationService).saveUserMessage(10L, "hello");
    }

    @Test
    @DisplayName("handleStream: guest mode, history messages from the frontend are correctly appended to the AI message list")
    void should_include_guest_history_in_ai_messages() {
        // given
        List<ChatRequest.HistoryMessage> history = List.of(
            new ChatRequest.HistoryMessage("user", "previous question"),
            new ChatRequest.HistoryMessage("assistant", "previous answer")
        );
        ChatRequest req = new ChatRequest(null, "new question", history);
        Message savedMsg = Message.builder().id(100L).role("assistant").content("answer").build();

        when(conversationService.createConversation(1L, null)).thenReturn(testConversation);
        when(conversationService.saveUserMessage(anyLong(), anyString()))
            .thenReturn(Message.builder().id(50L).build());
        when(conversationService.saveAssistantMessage(anyLong(), anyString(), anyList()))
            .thenReturn(savedMsg);
        when(aiChatProvider.streamChat(anyList())).thenReturn(Flux.just("answer"));

        // when
        chatService.handleStream(req, emitter);

        // then: verify streamChat is called with system + 2 history + current = 4 messages
        ArgumentCaptor<List> messagesCaptor = ArgumentCaptor.forClass(List.class);
        verify(aiChatProvider).streamChat(messagesCaptor.capture());
        assertThat(messagesCaptor.getValue()).hasSize(4);
    }

    // ===== Logged-in user session with existing conversationId =====

    @Test
    @DisplayName("handleStream: with existing conversationId, uses it directly without creating a new conversation")
    void should_not_create_new_conversation_when_conversationId_exists() {
        // given
        ChatRequest req = new ChatRequest(10L, "hello", null);
        Message savedMsg = Message.builder().id(100L).role("assistant").content("answer").build();

        when(conversationService.saveUserMessage(10L, "hello"))
            .thenReturn(Message.builder().id(50L).build());
        when(conversationService.loadHistory(10L, 20)).thenReturn(Collections.emptyList());
        when(conversationService.saveAssistantMessage(eq(10L), anyString(), anyList()))
            .thenReturn(savedMsg);
        when(aiChatProvider.streamChat(anyList())).thenReturn(Flux.just("answer"));

        // when
        chatService.handleStream(req, emitter);

        // then
        verify(conversationService, never()).createConversation(anyLong(), any());
        verify(conversationService).saveUserMessage(10L, "hello");
    }

    @Test
    @DisplayName("handleStream: logged-in user, loads history from DB (excluding last user message)")
    void should_load_history_from_db_for_logged_in_user() {
        // given
        List<MessageResponse> dbHistory = List.of(
            MessageResponse.builder().id(1L).role("user").content("history question 1").build(),
            MessageResponse.builder().id(2L).role("assistant").content("history answer 1").build(),
            MessageResponse.builder().id(3L).role("user").content("current question").build() // last entry excluded
        );
        ChatRequest req = new ChatRequest(10L, "current question", null);
        Message savedMsg = Message.builder().id(100L).role("assistant").content("answer").build();

        when(conversationService.saveUserMessage(10L, "current question"))
            .thenReturn(Message.builder().id(3L).build());
        when(conversationService.loadHistory(10L, 20)).thenReturn(dbHistory);
        when(conversationService.saveAssistantMessage(eq(10L), anyString(), anyList()))
            .thenReturn(savedMsg);
        when(aiChatProvider.streamChat(anyList())).thenReturn(Flux.just("answer"));

        // when
        chatService.handleStream(req, emitter);

        // then: system + 2 history (last excluded) + current user = 4
        ArgumentCaptor<List> messagesCaptor = ArgumentCaptor.forClass(List.class);
        verify(aiChatProvider).streamChat(messagesCaptor.capture());
        assertThat(messagesCaptor.getValue()).hasSize(4);
    }

    // ===== Stream completion behaviour =====

    @Test
    @DisplayName("handleStream: on stream complete, saveAssistantMessage is called with accumulated fullText and suggestions")
    void should_call_saveAssistantMessage_on_stream_complete() {
        // given
        ChatRequest req = new ChatRequest(null, "hello", null);
        Message savedMsg = Message.builder().id(100L).role("assistant").content("hello world").build();

        when(conversationService.createConversation(1L, null)).thenReturn(testConversation);
        when(conversationService.saveUserMessage(anyLong(), anyString()))
            .thenReturn(Message.builder().id(50L).build());
        when(conversationService.saveAssistantMessage(anyLong(), anyString(), anyList()))
            .thenReturn(savedMsg);
        when(aiChatProvider.streamChat(anyList())).thenReturn(Flux.just("hello", " world"));

        // when
        chatService.handleStream(req, emitter);

        // then
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(conversationService).saveAssistantMessage(eq(10L), contentCaptor.capture(), anyList());
        assertThat(contentCaptor.getValue()).isEqualTo("hello world");
    }

    @Test
    @DisplayName("handleStream: on stream complete, sendDone SSE event is sent with correct messageId")
    void should_send_done_event_on_stream_complete() {
        // given
        ChatRequest req = new ChatRequest(null, "hello", null);
        Message savedMsg = Message.builder().id(999L).role("assistant").content("full answer").build();

        when(conversationService.createConversation(1L, null)).thenReturn(testConversation);
        when(conversationService.saveUserMessage(anyLong(), anyString()))
            .thenReturn(Message.builder().id(50L).build());
        when(conversationService.saveAssistantMessage(anyLong(), anyString(), anyList()))
            .thenReturn(savedMsg);
        when(aiChatProvider.streamChat(anyList())).thenReturn(Flux.just("full answer"));

        // when
        chatService.handleStream(req, emitter);

        // then
        verify(sseEventBuilder).sendDone(eq(emitter), eq(999L), anyList());
    }

    @Test
    @DisplayName("handleStream: on stream complete, emitter.complete() is called")
    void should_complete_emitter_on_stream_complete() throws Exception {
        // given
        ChatRequest req = new ChatRequest(null, "hello", null);
        Message savedMsg = Message.builder().id(100L).role("assistant").content("answer").build();

        when(conversationService.createConversation(1L, null)).thenReturn(testConversation);
        when(conversationService.saveUserMessage(anyLong(), anyString()))
            .thenReturn(Message.builder().id(50L).build());
        when(conversationService.saveAssistantMessage(anyLong(), anyString(), anyList()))
            .thenReturn(savedMsg);
        when(aiChatProvider.streamChat(anyList())).thenReturn(Flux.just("answer"));

        // when
        chatService.handleStream(req, emitter);

        // then
        verify(emitter).complete();
    }

    // ===== Stream error behaviour =====

    @Test
    @DisplayName("handleStream: on stream error, sendError SSE event is sent")
    void should_send_error_event_on_stream_error() {
        // given
        ChatRequest req = new ChatRequest(null, "hello", null);
        RuntimeException streamError = new RuntimeException("AI service unavailable");

        when(conversationService.createConversation(1L, null)).thenReturn(testConversation);
        when(conversationService.saveUserMessage(anyLong(), anyString()))
            .thenReturn(Message.builder().id(50L).build());
        when(aiChatProvider.streamChat(anyList())).thenReturn(Flux.error(streamError));

        // when
        chatService.handleStream(req, emitter);

        // then
        verify(sseEventBuilder).sendError(eq(emitter), eq("STREAM_ERROR"), anyString());
    }

    @Test
    @DisplayName("handleStream: on stream error, emitter.completeWithError is called")
    void should_complete_emitter_with_error_on_stream_error() {
        // given
        ChatRequest req = new ChatRequest(null, "hello", null);
        RuntimeException streamError = new RuntimeException("AI service unavailable");

        when(conversationService.createConversation(1L, null)).thenReturn(testConversation);
        when(conversationService.saveUserMessage(anyLong(), anyString()))
            .thenReturn(Message.builder().id(50L).build());
        when(aiChatProvider.streamChat(anyList())).thenReturn(Flux.error(streamError));

        // when
        chatService.handleStream(req, emitter);

        // then
        verify(emitter).completeWithError(streamError);
    }

    // ===== Setup-phase exceptions =====

    @Test
    @DisplayName("handleStream: when ownerService throws during setup, sendError is called")
    void should_send_error_when_owner_service_throws() {
        // given
        ChatRequest req = new ChatRequest(null, "hello", null);
        when(conversationService.createConversation(1L, null)).thenReturn(testConversation);
        when(conversationService.saveUserMessage(anyLong(), anyString()))
            .thenReturn(Message.builder().id(50L).build());
        when(ownerService.getOwnerProfile(anyLong())).thenThrow(new ResourceNotFoundException("Owner", 1L));

        // when
        chatService.handleStream(req, emitter);

        // then
        verify(sseEventBuilder).sendError(eq(emitter), eq("INTERNAL_ERROR"), anyString());
    }

    @Test
    @DisplayName("handleStream: when a setup-phase exception occurs, emitter.completeWithError is called")
    void should_complete_emitter_with_error_on_setup_exception() {
        // given
        ChatRequest req = new ChatRequest(null, "hello", null);
        RuntimeException setupError = new RuntimeException("internal error");
        when(conversationService.createConversation(anyLong(), any())).thenThrow(setupError);

        // when
        chatService.handleStream(req, emitter);

        // then
        verify(emitter).completeWithError(setupError);
    }

    // ===== Token push =====

    @Test
    @DisplayName("handleStream: sseEventBuilder.sendToken is called for each token")
    void should_call_sendToken_for_each_token() {
        // given
        ChatRequest req = new ChatRequest(null, "hello", null);
        Message savedMsg = Message.builder().id(100L).role("assistant").content("hello world").build();

        when(conversationService.createConversation(1L, null)).thenReturn(testConversation);
        when(conversationService.saveUserMessage(anyLong(), anyString()))
            .thenReturn(Message.builder().id(50L).build());
        when(conversationService.saveAssistantMessage(anyLong(), anyString(), anyList()))
            .thenReturn(savedMsg);
        when(aiChatProvider.streamChat(anyList())).thenReturn(Flux.just("hello", " world"));

        // when
        chatService.handleStream(req, emitter);

        // then
        verify(sseEventBuilder).sendToken(emitter, "hello");
        verify(sseEventBuilder).sendToken(emitter, " world");
    }
}
