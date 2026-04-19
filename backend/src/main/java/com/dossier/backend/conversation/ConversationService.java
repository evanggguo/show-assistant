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
 * TDD 4.3 — Conversation management service.
 * Handles conversation creation, message saving, and history loading.
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
     * TDD 4.3.1 — Create a new conversation.
     * Creates a conversation for the given owner; userId is null for guest sessions.
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

    /** TDD 4.3.2 — Persist a user message to the database. */
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
     * TDD 4.3.3 — Persist the assistant message and its dynamic suggestions
     * after the AI streaming reply finishes.
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

        // Save dynamic suggestions
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
     * TDD 4.3.3 — Load conversation history.
     * Fetches the most recent {@code limit} messages in ascending chronological order (suitable as AI context).
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> loadHistory(Long conversationId, int limit) {
        List<Message> messages = messageRepository.findRecentByConversationId(
            conversationId, PageRequest.of(0, limit));
        // findRecentByConversationId returns descending order; reverse to ascending
        Collections.reverse(messages);
        return messages.stream()
            .map(this::mapToMessageResponse)
            .toList();
    }

    /**
     * TDD 6.5.1 — Get full conversation details including all messages
     * and the suggestions of the latest assistant message.
     */
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(Long conversationId) {
        Conversation conversation = getConversationEntity(conversationId);

        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<MessageResponse> messageResponses = messages.stream()
            .map(this::mapToMessageResponse)
            .toList();

        // Get suggestions of the last assistant message
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

    /** Internal: load a conversation entity by ID, throwing if not found. */
    private Conversation getConversationEntity(Long conversationId) {
        return conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));
    }

    /** Map a Message entity to a MessageResponse DTO. */
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
