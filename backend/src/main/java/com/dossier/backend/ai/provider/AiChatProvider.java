package com.dossier.backend.ai.provider;

import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * TDD 4.5.2 — AI chat provider interface
 * Abstracts streaming chat; ChatService only depends on this interface, not specific models.
 * Implementations are conditionally registered as Spring Beans via AiConfig.
 */
public interface AiChatProvider {

    /**
     * Stream a chat response.
     * Implementations call their respective model API and return token increments as Flux<String>.
     * Tool calls (e.g. SuggestFollowupsTool) are auto-triggered by Spring AI;
     * the Mock implementation invokes tool methods manually to simulate this behaviour.
     *
     * @param messages complete message list (SystemMessage + history + current UserMessage)
     * @param tools    tool instances to register (varargs)
     * @return incremental token stream
     */
    Flux<String> streamChat(List<Message> messages, Object... tools);

    /** Provider identifier used in log messages. */
    String providerName();

    /**
     * Whether this provider supports Function Calling for obtaining follow-up suggestions.
     * When false, ChatService skips tool registration and falls back to generateSuggestions(),
     * and PromptAssembler omits the suggest_followups instruction to avoid garbled model output.
     */
    default boolean supportsToolCalling() {
        return true;
    }

    /**
     * Fallback: generate follow-up suggestions via a separate non-streaming call when Tool Use
     * is not triggered (e.g. small models without Function Calling support).
     * Default returns an empty list; OllamaChatProvider overrides this.
     *
     * @param messages       same message list as streamChat
     * @param assistantReply full text of the just-completed streaming reply, used to build the fallback prompt
     * @return up to 3 suggestion questions, or an empty list on failure
     */
    default List<String> generateSuggestions(List<Message> messages, String assistantReply) {
        return List.of();
    }
}
