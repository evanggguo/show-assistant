package com.dossier.backend.ai.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * TDD 4.5.3 — Claude (Anthropic) cloud model provider (real implementation)
 * Calls the Anthropic Claude API via Spring AI AnthropicChatModel for streaming chat.
 *
 * Activated via ai.mock=false and ai.provider=claude in application.yml.
 * Requires ANTHROPIC_API_KEY to be set in the environment.
 */
@Slf4j
public class ClaudeChatProvider implements AiChatProvider {

    private final ChatClient chatClient;

    public ClaudeChatProvider(AnthropicChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @Override
    public Flux<String> streamChat(List<Message> messages, Object... tools) {
        log.debug("[ClaudeChatProvider] streamChat called with {} messages", messages.size());
        return chatClient.prompt()
            .messages(messages)
            .tools(tools)
            .stream()
            .content();
    }

    @Override
    public String providerName() {
        return "claude";
    }
}
