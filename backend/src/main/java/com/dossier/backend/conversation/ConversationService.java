package com.dossier.backend.conversation;

import com.dossier.backend.common.exception.ResourceNotFoundException;
import com.dossier.backend.conversation.dto.ConversationResponse;
import com.dossier.backend.conversation.dto.MessageResponse;
import com.dossier.backend.owner.Owner;
import com.dossier.backend.owner.OwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * TDD 4.3 — 会话管理服务
 * 负责会话的创建、消息保存和历史记录加载
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final DynamicSuggestionRepository dynamicSuggestionRepository;
    private final OwnerRepository ownerRepository;

    /**
     * TDD 4.3.1 — 创建新会话
     * 为指定 Owner 创建一个新会话，userId 为 null 时表示游客会话
     *
     * @param ownerId 拥有者 ID
     * @param userId  来访用户 ID，游客为 null
     * @return 新创建的会话实体
     */
    @Transactional
    public Conversation createConversation(Long ownerId, Long userId) {
        Owner owner = ownerRepository.findById(ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Owner", ownerId));

        Conversation conversation = Conversation.builder()
            .owner(owner)
            .userId(userId)
            .source("web")
            .build();

        Conversation saved = conversationRepository.save(conversation);
        log.debug("Created conversation id={} for ownerId={}, userId={}", saved.getId(), ownerId, userId);
        return saved;
    }

    /**
     * TDD 4.3.2 — 保存用户消息
     * 将访客发送的消息持久化到数据库
     *
     * @param conversationId 所属会话 ID
     * @param content        消息内容
     * @return 保存后的 Message 实体
     */
    @Transactional
    public Message saveUserMessage(Long conversationId, String content) {
        Conversation conversation = getConversationEntity(conversationId);
        Message message = Message.builder()
            .conversation(conversation)
            .role("user")
            .content(content)
            .build();
        Message saved = messageRepository.save(message);
        log.debug("Saved user message id={} to conversation={}", saved.getId(), conversationId);
        return saved;
    }

    /**
     * TDD 4.3.3 — 保存 assistant 消息及其动态提示词
     * 在 AI 流式回复结束后，将完整回复内容和动态 suggestions 持久化
     *
     * @param conversationId 所属会话 ID
     * @param content        AI 回复的完整文本
     * @param suggestions    由 SuggestFollowupsTool 捕获的跟进提示词列表
     * @return 保存后的 Message 实体
     */
    @Transactional
    public Message saveAssistantMessage(Long conversationId, String content, List<String> suggestions) {
        Conversation conversation = getConversationEntity(conversationId);

        Message message = Message.builder()
            .conversation(conversation)
            .role("assistant")
            .content(content)
            .build();
        Message savedMessage = messageRepository.save(message);

        // 保存动态提示词
        if (suggestions != null && !suggestions.isEmpty()) {
            for (int i = 0; i < suggestions.size(); i++) {
                DynamicSuggestion ds = DynamicSuggestion.builder()
                    .message(savedMessage)
                    .text(suggestions.get(i))
                    .sortOrder(i)
                    .build();
                dynamicSuggestionRepository.save(ds);
            }
        }

        log.debug("Saved assistant message id={} with {} suggestions to conversation={}",
            savedMessage.getId(), suggestions == null ? 0 : suggestions.size(), conversationId);
        return savedMessage;
    }

    /**
     * TDD 4.3.3 — 加载会话历史消息
     * 查询最近 limit 条消息，按时间正序返回（适合作为 AI 上下文）
     *
     * @param conversationId 会话 ID
     * @param limit          最大返回条数
     * @return 按时间正序排列的 MessageResponse 列表
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> loadHistory(Long conversationId, int limit) {
        List<Message> messages = messageRepository.findRecentByConversationId(
            conversationId, PageRequest.of(0, limit));
        // findRecentByConversationId 返回倒序，需要翻转为正序
        Collections.reverse(messages);
        return messages.stream()
            .map(this::mapToMessageResponse)
            .toList();
    }

    /**
     * TDD 6.5.1 — 获取完整会话详情
     * 包含所有消息历史和最新一条 assistant 消息的 suggestions
     *
     * @param conversationId 会话 ID
     * @return ConversationResponse 包含完整上下文
     */
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(Long conversationId) {
        Conversation conversation = getConversationEntity(conversationId);

        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<MessageResponse> messageResponses = messages.stream()
            .map(this::mapToMessageResponse)
            .toList();

        // 获取最后一条 assistant 消息的 suggestions
        List<String> lastSuggestions = messageRepository
            .findLastAssistantMessage(conversationId, PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .map(m -> dynamicSuggestionRepository.findByMessageIdOrderBySortOrderAsc(m.getId())
                .stream()
                .map(DynamicSuggestion::getText)
                .toList())
            .orElse(Collections.emptyList());

        return ConversationResponse.builder()
            .id(conversation.getId())
            .ownerId(conversation.getOwner().getId())
            .source(conversation.getSource())
            .createdAt(conversation.getCreatedAt())
            .updatedAt(conversation.getUpdatedAt())
            .messages(messageResponses)
            .lastSuggestions(lastSuggestions)
            .build();
    }

    /**
     * 内部方法：根据 ID 加载会话实体，不存在则抛出异常
     */
    private Conversation getConversationEntity(Long conversationId) {
        return conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));
    }

    /**
     * 将 Message 实体映射为 MessageResponse DTO
     */
    private MessageResponse mapToMessageResponse(Message message) {
        List<String> suggestions = dynamicSuggestionRepository
            .findByMessageIdOrderBySortOrderAsc(message.getId())
            .stream()
            .map(DynamicSuggestion::getText)
            .toList();

        return MessageResponse.builder()
            .id(message.getId())
            .role(message.getRole())
            .content(message.getContent())
            .createdAt(message.getCreatedAt())
            .suggestions(suggestions)
            .build();
    }
}
