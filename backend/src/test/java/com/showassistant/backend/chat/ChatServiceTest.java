package com.showassistant.backend.chat;

import com.showassistant.backend.chat.dto.ChatRequest;
import com.showassistant.backend.common.exception.ResourceNotFoundException;
import com.showassistant.backend.conversation.Conversation;
import com.showassistant.backend.conversation.ConversationService;
import com.showassistant.backend.conversation.Message;
import com.showassistant.backend.conversation.dto.MessageResponse;
import com.showassistant.backend.knowledge.RagService;
import com.showassistant.backend.knowledge.dto.KnowledgeEntryDto;
import com.showassistant.backend.owner.Owner;
import com.showassistant.backend.owner.OwnerService;
import com.showassistant.backend.owner.dto.OwnerProfileResponse;
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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ChatService 单元测试
 * 覆盖 handleStream 的完整业务流程，包括游客模式、登录用户模式、正常完成和异常路径
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ChatService 单元测试")
class ChatServiceTest {

    @Mock
    private ChatClient chatClient;

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

    // ChatClient 链式调用 mock 对象
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.StreamResponseSpec streamSpec;

    private Owner testOwner;
    private Conversation testConversation;
    private OwnerProfileResponse ownerProfile;

    @BeforeEach
    void setUp() {
        testOwner = Owner.builder()
            .id(1L)
            .name("测试主人")
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
            .name("测试主人")
            .tagline("测试 tagline")
            .build();

        // 构建 ChatClient 链式 mock
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        streamSpec = mock(ChatClient.StreamResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamSpec);

        // 默认 mock：ownerService 和 ragService 正常返回
        when(ownerService.getOwnerProfile()).thenReturn(ownerProfile);
        when(promptAssembler.assemble(any(), anyList())).thenReturn("System prompt");
        when(ragService.retrieve(anyLong(), anyString())).thenReturn(Collections.emptyList());
    }

    // ===== 游客新会话 =====

    @Test
    @DisplayName("handleStream：游客新会话（conversationId=null），正常流程——创建新会话")
    void should_create_new_conversation_for_guest_when_conversationId_is_null() {
        // given
        ChatRequest req = new ChatRequest(null, "你好", null);
        Message savedMsg = Message.builder().id(100L).role("assistant").content("你好，世界").build();

        when(conversationService.createConversation(1L, null)).thenReturn(testConversation);
        when(conversationService.saveUserMessage(anyLong(), anyString()))
            .thenReturn(Message.builder().id(50L).build());
        when(conversationService.saveAssistantMessage(anyLong(), anyString(), anyList()))
            .thenReturn(savedMsg);
        when(streamSpec.content()).thenReturn(Flux.just("你好", "，世界"));

        // when
        chatService.handleStream(req, emitter);

        // then
        verify(conversationService).createConversation(1L, null);
        verify(conversationService).saveUserMessage(10L, "你好");
    }

    @Test
    @DisplayName("handleStream：游客模式，前端携带的 history 消息被正确附加到 AI 消息列表")
    void should_include_guest_history_in_ai_messages() {
        // given
        List<ChatRequest.HistoryMessage> history = List.of(
            new ChatRequest.HistoryMessage("user", "之前的问题"),
            new ChatRequest.HistoryMessage("assistant", "之前的回答")
        );
        ChatRequest req = new ChatRequest(null, "新问题", history);
        Message savedMsg = Message.builder().id(100L).role("assistant").content("回答").build();

        when(conversationService.createConversation(1L, null)).thenReturn(testConversation);
        when(conversationService.saveUserMessage(anyLong(), anyString()))
            .thenReturn(Message.builder().id(50L).build());
        when(conversationService.saveAssistantMessage(anyLong(), anyString(), anyList()))
            .thenReturn(savedMsg);
        when(streamSpec.content()).thenReturn(Flux.just("回答内容"));

        // when
        chatService.handleStream(req, emitter);

        // then
        // 验证 requestSpec.messages() 被调用，且消息列表包含历史和当前消息
        ArgumentCaptor<List> messagesCaptor = ArgumentCaptor.forClass(List.class);
        verify(requestSpec).messages(messagesCaptor.capture());
        List<?> aiMessages = messagesCaptor.getValue();
        // 包含 system message + 2 条历史 + 1 条当前用户消息 = 4 条
        assertThat(aiMessages).hasSize(4);
    }

    // ===== 已有 conversationId 的登录用户会话 =====

    @Test
    @DisplayName("handleStream：已有 conversationId，直接使用请求中的 conversationId（不创建新会话）")
    void should_not_create_new_conversation_when_conversationId_exists() {
        // given
        ChatRequest req = new ChatRequest(10L, "你好", null);
        Message savedMsg = Message.builder().id(100L).role("assistant").content("回答").build();

        when(conversationService.saveUserMessage(10L, "你好"))
            .thenReturn(Message.builder().id(50L).build());
        when(conversationService.loadHistory(10L, 20)).thenReturn(Collections.emptyList());
        when(conversationService.saveAssistantMessage(eq(10L), anyString(), anyList()))
            .thenReturn(savedMsg);
        when(streamSpec.content()).thenReturn(Flux.just("回答"));

        // when
        chatService.handleStream(req, emitter);

        // then
        verify(conversationService, never()).createConversation(anyLong(), any());
        verify(conversationService).saveUserMessage(10L, "你好");
    }

    @Test
    @DisplayName("handleStream：登录用户，从数据库加载历史消息（排除最后一条 user 消息）")
    void should_load_history_from_db_for_logged_in_user() {
        // given
        List<MessageResponse> dbHistory = List.of(
            MessageResponse.builder().id(1L).role("user").content("历史问题1").build(),
            MessageResponse.builder().id(2L).role("assistant").content("历史回答1").build(),
            MessageResponse.builder().id(3L).role("user").content("当前问题").build() // 最后一条被排除
        );
        ChatRequest req = new ChatRequest(10L, "当前问题", null);
        Message savedMsg = Message.builder().id(100L).role("assistant").content("回答").build();

        when(conversationService.saveUserMessage(10L, "当前问题"))
            .thenReturn(Message.builder().id(3L).build());
        when(conversationService.loadHistory(10L, 20)).thenReturn(dbHistory);
        when(conversationService.saveAssistantMessage(eq(10L), anyString(), anyList()))
            .thenReturn(savedMsg);
        when(streamSpec.content()).thenReturn(Flux.just("回答"));

        // when
        chatService.handleStream(req, emitter);

        // then
        ArgumentCaptor<List> messagesCaptor = ArgumentCaptor.forClass(List.class);
        verify(requestSpec).messages(messagesCaptor.capture());
        List<?> aiMessages = messagesCaptor.getValue();
        // system message + 2 条历史（排除最后一条）+ 1 条当前 user = 4
        assertThat(aiMessages).hasSize(4);
    }

    // ===== stream 完成时的行为 =====

    @Test
    @DisplayName("handleStream：stream 完成时，saveAssistantMessage 被调用（含累积 fullText 和 suggestions）")
    void should_call_saveAssistantMessage_on_stream_complete() {
        // given
        ChatRequest req = new ChatRequest(null, "你好", null);
        Message savedMsg = Message.builder().id(100L).role("assistant").content("你好，世界").build();

        when(conversationService.createConversation(1L, null)).thenReturn(testConversation);
        when(conversationService.saveUserMessage(anyLong(), anyString()))
            .thenReturn(Message.builder().id(50L).build());
        when(conversationService.saveAssistantMessage(anyLong(), anyString(), anyList()))
            .thenReturn(savedMsg);
        // 流返回两个 token
        when(streamSpec.content()).thenReturn(Flux.just("你好", "，世界"));

        // when
        chatService.handleStream(req, emitter);

        // then
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(conversationService).saveAssistantMessage(
            eq(10L), contentCaptor.capture(), anyList());
        // 两个 token 应该被累积
        assertThat(contentCaptor.getValue()).isEqualTo("你好，世界");
    }

    @Test
    @DisplayName("handleStream：stream 完成时，sendDone SSE 事件被正确发送（含 messageId）")
    void should_send_done_event_on_stream_complete() {
        // given
        ChatRequest req = new ChatRequest(null, "你好", null);
        Message savedMsg = Message.builder().id(999L).role("assistant").content("完整回答").build();

        when(conversationService.createConversation(1L, null)).thenReturn(testConversation);
        when(conversationService.saveUserMessage(anyLong(), anyString()))
            .thenReturn(Message.builder().id(50L).build());
        when(conversationService.saveAssistantMessage(anyLong(), anyString(), anyList()))
            .thenReturn(savedMsg);
        when(streamSpec.content()).thenReturn(Flux.just("完整回答"));

        // when
        chatService.handleStream(req, emitter);

        // then
        verify(sseEventBuilder).sendDone(eq(emitter), eq(999L), anyList());
    }

    @Test
    @DisplayName("handleStream：stream 完成时，emitter.complete() 被调用")
    void should_complete_emitter_on_stream_complete() throws Exception {
        // given
        ChatRequest req = new ChatRequest(null, "你好", null);
        Message savedMsg = Message.builder().id(100L).role("assistant").content("回答").build();

        when(conversationService.createConversation(1L, null)).thenReturn(testConversation);
        when(conversationService.saveUserMessage(anyLong(), anyString()))
            .thenReturn(Message.builder().id(50L).build());
        when(conversationService.saveAssistantMessage(anyLong(), anyString(), anyList()))
            .thenReturn(savedMsg);
        when(streamSpec.content()).thenReturn(Flux.just("回答"));

        // when
        chatService.handleStream(req, emitter);

        // then
        verify(emitter).complete();
    }

    // ===== stream 出错时的行为 =====

    @Test
    @DisplayName("handleStream：stream 出错时，sendError SSE 事件被调用")
    void should_send_error_event_on_stream_error() {
        // given
        ChatRequest req = new ChatRequest(null, "你好", null);
        RuntimeException streamError = new RuntimeException("AI 服务不可用");

        when(conversationService.createConversation(1L, null)).thenReturn(testConversation);
        when(conversationService.saveUserMessage(anyLong(), anyString()))
            .thenReturn(Message.builder().id(50L).build());
        when(streamSpec.content()).thenReturn(Flux.error(streamError));

        // when
        chatService.handleStream(req, emitter);

        // then
        verify(sseEventBuilder).sendError(eq(emitter), eq("STREAM_ERROR"), anyString());
    }

    @Test
    @DisplayName("handleStream：stream 出错时，emitter.completeWithError 被调用")
    void should_complete_emitter_with_error_on_stream_error() {
        // given
        ChatRequest req = new ChatRequest(null, "你好", null);
        RuntimeException streamError = new RuntimeException("AI 服务不可用");

        when(conversationService.createConversation(1L, null)).thenReturn(testConversation);
        when(conversationService.saveUserMessage(anyLong(), anyString()))
            .thenReturn(Message.builder().id(50L).build());
        when(streamSpec.content()).thenReturn(Flux.error(streamError));

        // when
        chatService.handleStream(req, emitter);

        // then
        verify(emitter).completeWithError(streamError);
    }

    // ===== setup 阶段异常 =====

    @Test
    @DisplayName("handleStream：setup 阶段 ownerService 抛出异常时，sendError 被调用")
    void should_send_error_when_owner_service_throws() {
        // given
        ChatRequest req = new ChatRequest(null, "你好", null);
        when(conversationService.createConversation(1L, null)).thenReturn(testConversation);
        when(conversationService.saveUserMessage(anyLong(), anyString()))
            .thenReturn(Message.builder().id(50L).build());
        when(ragService.retrieve(anyLong(), anyString())).thenReturn(Collections.emptyList());
        when(ownerService.getOwnerProfile()).thenThrow(new ResourceNotFoundException("Owner", 1L));

        // when
        chatService.handleStream(req, emitter);

        // then
        verify(sseEventBuilder).sendError(eq(emitter), eq("INTERNAL_ERROR"), anyString());
    }

    @Test
    @DisplayName("handleStream：setup 阶段异常时，emitter.completeWithError 被调用")
    void should_complete_emitter_with_error_on_setup_exception() {
        // given
        ChatRequest req = new ChatRequest(null, "你好", null);
        RuntimeException setupError = new RuntimeException("内部错误");
        when(conversationService.createConversation(anyLong(), any())).thenThrow(setupError);

        // when
        chatService.handleStream(req, emitter);

        // then
        verify(emitter).completeWithError(setupError);
    }

    // ===== token 推送 =====

    @Test
    @DisplayName("handleStream：每个 token 推送时，sseEventBuilder.sendToken 被调用")
    void should_call_sendToken_for_each_token() {
        // given
        ChatRequest req = new ChatRequest(null, "你好", null);
        Message savedMsg = Message.builder().id(100L).role("assistant").content("你好世界").build();

        when(conversationService.createConversation(1L, null)).thenReturn(testConversation);
        when(conversationService.saveUserMessage(anyLong(), anyString()))
            .thenReturn(Message.builder().id(50L).build());
        when(conversationService.saveAssistantMessage(anyLong(), anyString(), anyList()))
            .thenReturn(savedMsg);
        when(streamSpec.content()).thenReturn(Flux.just("你好", "世界"));

        // when
        chatService.handleStream(req, emitter);

        // then
        verify(sseEventBuilder).sendToken(emitter, "你好");
        verify(sseEventBuilder).sendToken(emitter, "世界");
    }
}
