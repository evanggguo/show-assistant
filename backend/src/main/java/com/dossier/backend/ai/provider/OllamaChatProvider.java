package com.dossier.backend.ai.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.ollama.OllamaChatModel;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * TDD 4.5.3 — Ollama local model provider (real implementation).
 * Calls the local Ollama service via Spring AI OllamaChatModel for streaming chat.
 *
 * Activated via ai.provider=ollama (default) in application.yml; ai.mock has no effect on the local model.
 * The Ollama service URL is configured via spring.ai.ollama.base-url (default http://localhost:11434).
 */
@Slf4j
public class OllamaChatProvider implements AiChatProvider {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String SUGGESTION_PROMPT_TEMPLATE =
        "Based on the following assistant response, generate 2-3 follow-up questions a visitor might find interesting.\n\n" +
        "Assistant response:\n%s\n\n" +
        "Requirement: output a JSON array only, no other text. Format: [\"question 1\", \"question 2\"]. Keep each question under 20 words.";

    private final ChatClient chatClient;

    public OllamaChatProvider(OllamaChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @Override
    public Flux<String> streamChat(List<Message> messages, Object... tools) {
        log.debug("[OllamaChatProvider] streamChat called with {} messages", messages.size());
        return chatClient.prompt()
            .messages(messages)
            .tools(tools)
            .stream()
            .content();
    }

    @Override
    public String providerName() {
        return "ollama";
    }

    @Override
    public List<String> generateSuggestions(List<Message> messages, String assistantReply) {
        try {
            String prompt = String.format(SUGGESTION_PROMPT_TEMPLATE, assistantReply);
            String response = chatClient.prompt()
                .messages(List.of(new UserMessage(prompt)))
                .call()
                .content();
            return parseSuggestions(response);
        } catch (Exception e) {
            log.warn("[OllamaChatProvider] generateSuggestions fallback failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<String> parseSuggestions(String response) {
        if (response == null || response.isBlank()) return List.of();
        try {
            int start = response.indexOf('[');
            int end = response.lastIndexOf(']');
            if (start == -1 || end <= start) {
                log.warn("[OllamaChatProvider] No JSON array in suggestion response: {}", response);
                return List.of();
            }
            List<String> result = MAPPER.readValue(
                response.substring(start, end + 1),
                new TypeReference<List<String>>() {});
            return result.stream()
                .filter(s -> s != null && !s.isBlank())
                .limit(3)
                .toList();
        } catch (Exception e) {
            log.warn("[OllamaChatProvider] Failed to parse suggestions JSON: {}", e.getMessage());
            return List.of();
        }
    }
}
