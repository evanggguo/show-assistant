package com.dossier.backend.chat;

import com.dossier.backend.ai.provider.AiChatProvider;
import com.dossier.backend.chat.dto.ChatRequest;
import com.dossier.backend.chat.tool.SuggestFollowupsTool;
import com.dossier.backend.conversation.Conversation;
import com.dossier.backend.conversation.ConversationService;
import com.dossier.backend.conversation.Message;
import com.dossier.backend.knowledge.RagService;
import com.dossier.backend.knowledge.dto.KnowledgeEntryDto;
import com.dossier.backend.owner.OwnerService;
import com.dossier.backend.owner.dto.OwnerProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TDD 4.3 — Core streaming chat service
 * Coordinates RAG retrieval, Spring AI streaming, SSE event pushing, and message persistence.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int HISTORY_LIMIT = 20;

    private final AiChatProvider aiChatProvider;
    private final ConversationService conversationService;
    private final OwnerService ownerService;
    private final RagService ragService;
    private final PromptAssembler promptAssembler;
    private final SseEventBuilder sseEventBuilder;

    @Value("${app.sse.timeout-ms:180000}")
    private long sseTimeoutMs;

    /**
     * TDD 4.3.1 — Create SSE connection
     * Initialises a SseEmitter with the configured timeout and returns it to the controller.
     */
    public SseEmitter createEmitter() {
        return new SseEmitter(sseTimeoutMs);
    }

    /**
     * TDD 4.3.2 — Core streaming handler (async)
     * Full streaming chat pipeline:
     * 1. Create or load conversation
     * 2. Save user message
     * 3. RAG retrieval via RagService (returns empty list in Phase 2)
     * 4. Load history and build Spring AI message list
     * 5. Build System Prompt (includes owner info and RAG context)
     * 6. Create per-request SuggestFollowupsTool instance
     * 7. Stream via Spring AI chatClient
     * 8. onNext: accumulate fullText + push token SSE
     * 9. onComplete: save assistant message + suggestions, push done SSE
     * 10. onError: push error SSE, complete emitter
     */
    /** Backwards-compatible entry point (owner_id=1) for the legacy /api/chat/stream route. */
    @Async("sseTaskExecutor")
    public void handleStream(ChatRequest req, SseEmitter emitter) {
        handleStream(req, emitter, 1L);
    }

    @Async("sseTaskExecutor")
    public void handleStream(ChatRequest req, SseEmitter emitter, Long ownerId) {
        try {
            // Step 1: create or load conversation
            Long conversationId = resolveConversationId(req, ownerId);

            // Step 2: save user message
            conversationService.saveUserMessage(conversationId, req.message());

            // Step 3: RAG retrieval (returns empty list in Phase 2)
            List<KnowledgeEntryDto> ragContext = ragService.retrieve(ownerId, req.message(), 10);

            // Step 4: build Spring AI message list
            boolean toolCallingEnabled = aiChatProvider.supportsToolCalling();
            List<org.springframework.ai.chat.messages.Message> aiMessages =
                buildAiMessages(req, conversationId, ragContext, toolCallingEnabled, ownerId);

            // Step 5 is already included in aiMessages (SystemMessage)

            // Step 6: create per-request SuggestFollowupsTool (not a Spring Bean)
            SuggestFollowupsTool suggestTool = new SuggestFollowupsTool();

            // Steps 7-9: stream and handle (TDD 4.5 — calls through AiChatProvider, model-agnostic)
            AtomicReference<StringBuilder> fullTextRef = new AtomicReference<>(new StringBuilder());
            final Long finalConversationId = conversationId;
            log.debug("Using AI provider: {}, toolCalling={}", aiChatProvider.providerName(), toolCallingEnabled);

            Object[] tools = toolCallingEnabled ? new Object[]{suggestTool} : new Object[0];
            aiChatProvider.streamChat(aiMessages, tools)
                .subscribe(
                    // onNext: accumulate fullText + push token SSE
                    token -> {
                        fullTextRef.get().append(token);
                        sseEventBuilder.sendToken(emitter, token);
                    },
                    // onError: push error SSE
                    error -> {
                        log.error("Stream error for conversation={}: {}", finalConversationId, error.getMessage(), error);
                        sseEventBuilder.sendError(emitter, "STREAM_ERROR", "AI service temporarily unavailable, please try again later");
                        emitter.completeWithError(error);
                    },
                    // onComplete: save message + push done SSE
                    () -> {
                        String fullText = fullTextRef.get().toString();
                        List<String> suggestions = suggestTool.getCapturedSuggestions();

                        // Fallback: when Tool Use is not triggered (e.g. small models without Function Calling),
                        // generate suggestions via an extra non-streaming call
                        if (suggestions.isEmpty()) {
                            log.debug("Tool Use suggestions empty for conversation={}, triggering fallback",
                                finalConversationId);
                            suggestions = aiChatProvider.generateSuggestions(aiMessages, fullText);
                            log.debug("Fallback generated {} suggestions for conversation={}",
                                suggestions.size(), finalConversationId);
                        }

                        try {
                            Message savedMsg = conversationService.saveAssistantMessage(
                                finalConversationId, fullText, suggestions);
                            sseEventBuilder.sendDone(emitter, savedMsg.getId(), suggestions);
                            emitter.complete();
                            log.debug("Stream completed for conversation={}, messageId={}",
                                finalConversationId, savedMsg.getId());
                        } catch (Exception e) {
                            log.error("Failed to save assistant message for conversation={}: {}",
                                finalConversationId, e.getMessage(), e);
                            sseEventBuilder.sendError(emitter, "SAVE_ERROR", "Failed to save message");
                            emitter.completeWithError(e);
                        }
                    }
                );

        } catch (Exception e) {
            log.error("handleStream error: {}", e.getMessage(), e);
            sseEventBuilder.sendError(emitter, "INTERNAL_ERROR", "Internal server error, please try again later");
            try {
                emitter.completeWithError(e);
            } catch (Exception ignored) {
                // emitter already closed
            }
        }
    }

    /**
     * TDD 4.3.1 — Resolve or create conversation ID
     * When conversationId is null (guest), a new conversation is created;
     * otherwise the ID from the request is used directly.
     */
    private Long resolveConversationId(ChatRequest req, Long ownerId) {
        if (req.conversationId() == null) {
            // Guest new conversation: user_id=null
            Conversation newConversation = conversationService.createConversation(ownerId, null);
            log.debug("Created new guest conversation id={} for ownerId={}", newConversation.getId(), ownerId);
            return newConversation.getId();
        }
        return req.conversationId();
    }

    /**
     * TDD 4.3.2 — Build Spring AI message list
     * Order: 1. SystemMessage (owner info + RAG context), 2. history, 3. current user message.
     */
    private List<org.springframework.ai.chat.messages.Message> buildAiMessages(
        ChatRequest req, Long conversationId, List<KnowledgeEntryDto> ragContext,
        boolean includeToolInstruction, Long ownerId) {

        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();

        // System Message
        OwnerProfileResponse ownerProfile = ownerService.getOwnerProfile(ownerId);
        String systemPrompt = promptAssembler.assemble(ownerProfile, ragContext, includeToolInstruction, req.message());
        messages.add(new SystemMessage(systemPrompt));

        // History messages
        if (req.conversationId() == null && req.history() != null && !req.history().isEmpty()) {
            // Guest mode: use history from the frontend (excludes the just-saved user message to avoid duplication)
            List<ChatRequest.HistoryMessage> history = req.history();
            for (ChatRequest.HistoryMessage h : history) {
                if ("user".equals(h.role())) {
                    messages.add(new UserMessage(h.content()));
                } else if ("assistant".equals(h.role())) {
                    messages.add(new AssistantMessage(h.content()));
                }
            }
        } else if (req.conversationId() != null) {
            // Logged-in user: load history from DB (excludes the just-saved user message)
            List<com.dossier.backend.conversation.dto.MessageResponse> dbHistory =
                conversationService.loadHistory(conversationId, HISTORY_LIMIT);
            // Exclude the last entry (the just-saved user message)
            int endIdx = dbHistory.size() > 0 ? dbHistory.size() - 1 : 0;
            for (int i = 0; i < endIdx; i++) {
                var msgResp = dbHistory.get(i);
                if ("user".equals(msgResp.getRole())) {
                    messages.add(new UserMessage(msgResp.getContent()));
                } else if ("assistant".equals(msgResp.getRole())) {
                    messages.add(new AssistantMessage(msgResp.getContent()));
                }
            }
        }

        // Current user message
        messages.add(new UserMessage(req.message()));

        return messages;
    }
}
