package com.showassistant.backend.conversation;

import com.showassistant.backend.common.exception.ResourceNotFoundException;
import com.showassistant.backend.conversation.dto.ConversationResponse;
import com.showassistant.backend.conversation.dto.MessageResponse;
import com.showassistant.backend.owner.Owner;
import com.showassistant.backend.owner.OwnerRepository;
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
 * ConversationService 单元测试
 * 覆盖会话创建、消息保存、历史加载和会话详情查询的完整场景
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConversationService 单元测试")
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
            .name("测试主人")
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
    @DisplayName("createConversation：owner 不存在时抛 ResourceNotFoundException")
    void should_throw_ResourceNotFoundException_when_owner_not_found() {
        // given
        when(ownerRepository.findById(99L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> conversationService.createConversation(99L, null))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createConversation：正常创建，返回新会话")
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
    @DisplayName("createConversation：游客会话 userId 为 null，source 为 web")
    void should_create_guest_conversation_with_null_userId() {
        // given
        when(ownerRepository.findById(1L)).thenReturn(Optional.of(testOwner));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            // 模拟数据库赋 ID
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
    @DisplayName("saveUserMessage：会话不存在时抛 ResourceNotFoundException")
    void should_throw_when_conversation_not_found_on_saveUserMessage() {
        // given
        when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> conversationService.saveUserMessage(999L, "你好"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("saveUserMessage：正常保存用户消息")
    void should_save_user_message_successfully() {
        // given
        Message savedMsg = Message.builder().id(100L).role("user").content("你好").build();
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(testConversation));
        when(messageRepository.save(any(Message.class))).thenReturn(savedMsg);

        // when
        Message result = conversationService.saveUserMessage(10L, "你好");

        // then
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getRole()).isEqualTo("user");
        assertThat(result.getContent()).isEqualTo("你好");
        verify(messageRepository).save(any(Message.class));
    }

    // ===== saveAssistantMessage =====

    @Test
    @DisplayName("saveAssistantMessage：suggestions 为空列表时，不保存 DynamicSuggestion")
    void should_not_save_dynamic_suggestions_when_empty() {
        // given
        Message savedMsg = Message.builder().id(200L).role("assistant").content("回复内容").build();
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(testConversation));
        when(messageRepository.save(any(Message.class))).thenReturn(savedMsg);

        // when
        conversationService.saveAssistantMessage(10L, "回复内容", Collections.emptyList());

        // then
        verify(dynamicSuggestionRepository, never()).save(any(DynamicSuggestion.class));
    }

    @Test
    @DisplayName("saveAssistantMessage：suggestions 为 null 时，不保存 DynamicSuggestion")
    void should_not_save_dynamic_suggestions_when_null() {
        // given
        Message savedMsg = Message.builder().id(200L).role("assistant").content("回复内容").build();
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(testConversation));
        when(messageRepository.save(any(Message.class))).thenReturn(savedMsg);

        // when
        conversationService.saveAssistantMessage(10L, "回复内容", null);

        // then
        verify(dynamicSuggestionRepository, never()).save(any(DynamicSuggestion.class));
    }

    @Test
    @DisplayName("saveAssistantMessage：suggestions 有内容时，按顺序保存，sortOrder 从 0 开始")
    void should_save_dynamic_suggestions_with_correct_sort_order() {
        // given
        Message savedMsg = Message.builder().id(200L).role("assistant").content("回复内容").build();
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(testConversation));
        when(messageRepository.save(any(Message.class))).thenReturn(savedMsg);
        when(dynamicSuggestionRepository.save(any(DynamicSuggestion.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        List<String> suggestions = List.of("问题A", "问题B", "问题C");

        // when
        conversationService.saveAssistantMessage(10L, "回复内容", suggestions);

        // then
        ArgumentCaptor<DynamicSuggestion> captor = ArgumentCaptor.forClass(DynamicSuggestion.class);
        verify(dynamicSuggestionRepository, times(3)).save(captor.capture());

        List<DynamicSuggestion> saved = captor.getAllValues();
        assertThat(saved.get(0).getText()).isEqualTo("问题A");
        assertThat(saved.get(0).getSortOrder()).isEqualTo(0);
        assertThat(saved.get(1).getText()).isEqualTo("问题B");
        assertThat(saved.get(1).getSortOrder()).isEqualTo(1);
        assertThat(saved.get(2).getText()).isEqualTo("问题C");
        assertThat(saved.get(2).getSortOrder()).isEqualTo(2);
    }

    // ===== loadHistory =====

    @Test
    @DisplayName("loadHistory：返回按时间正序的消息（内部反转 DESC 查询结果）")
    void should_return_messages_in_ascending_order() {
        // given
        // findRecentByConversationId 返回倒序（DESC），服务层会 reverse
        Message msg1 = Message.builder().id(1L).role("user").content("消息1")
            .createdAt(OffsetDateTime.now().minusMinutes(2)).build();
        Message msg2 = Message.builder().id(2L).role("assistant").content("回复1")
            .createdAt(OffsetDateTime.now().minusMinutes(1)).build();
        Message msg3 = Message.builder().id(3L).role("user").content("消息2")
            .createdAt(OffsetDateTime.now()).build();

        // 模拟 DESC 查询返回 [msg3, msg2, msg1]（必须是可修改列表，因为服务层会调用 Collections.reverse）
        when(messageRepository.findRecentByConversationId(eq(10L), any(Pageable.class)))
            .thenReturn(new ArrayList<>(List.of(msg3, msg2, msg1)));
        when(dynamicSuggestionRepository.findByMessageIdOrderBySortOrderAsc(anyLong()))
            .thenReturn(Collections.emptyList());

        // when
        List<MessageResponse> result = conversationService.loadHistory(10L, 20);

        // then - 服务层 reverse 后应为正序 [msg1, msg2, msg3]
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(2).getId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("loadHistory：会话无消息时返回空列表")
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
    @DisplayName("getConversation：会话不存在时抛 ResourceNotFoundException")
    void should_throw_when_conversation_not_found_on_getConversation() {
        // given
        when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> conversationService.getConversation(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getConversation：最后一条 assistant 消息有 suggestions 时，lastSuggestions 正确返回")
    void should_return_lastSuggestions_from_last_assistant_message() {
        // given
        Message assistantMsg = Message.builder().id(50L).role("assistant").content("AI 回复").build();
        DynamicSuggestion ds1 = DynamicSuggestion.builder().id(1L).text("建议1").sortOrder(0).build();
        DynamicSuggestion ds2 = DynamicSuggestion.builder().id(2L).text("建议2").sortOrder(1).build();

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
        assertThat(response.getLastSuggestions()).containsExactly("建议1", "建议2");
    }

    @Test
    @DisplayName("getConversation：没有 assistant 消息时，lastSuggestions 为空列表")
    void should_return_empty_lastSuggestions_when_no_assistant_message() {
        // given
        Message userMsg = Message.builder().id(30L).role("user").content("用户消息").build();

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
    @DisplayName("getConversation：正常返回会话基本信息")
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
