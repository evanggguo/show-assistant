package com.dossier.backend.ai.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Google AI Studio (Gemini) cloud model provider.
 * Calls the Google AI Studio API via Spring AI's GoogleGenAiChatModel for streaming chat.
 *
 * Activated when ai.provider=google and ai.mock=false in application.yml.
 * Requires GOOGLE_AI_API_KEY in the environment.
 *
 * Note: tools are not passed to Gemini to avoid stream parsing errors with function calling.
 * generateSuggestions() fallback is used instead.
 */
@Slf4j
public class GoogleChatProvider implements AiChatProvider {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String SUGGESTION_PROMPT_TEMPLATE =
        "Based on the assistant's reply below, generate 2-3 follow-up questions a visitor might ask.\n\n" +
        "Assistant's reply:\n%s\n\n" +
        "IMPORTANT: Generate the questions in the SAME language as the assistant's reply above.\n" +
        "Output ONLY a JSON array with no other text. Format: [\"question1\", \"question2\"]. Each question under 30 characters.";

    private final ChatClient chatClient;

    public GoogleChatProvider(GoogleGenAiChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @Override
    public Flux<String> streamChat(List<Message> messages, Object... tools) {
        log.debug("[GoogleChatProvider] streamChat called with {} messages", messages.size());
        // Tools are not passed: Gemini function calling causes stream parsing errors;
        // suggestions are generated via generateSuggestions() fallback instead.
        return chatClient.prompt()
            .messages(messages)
            .stream()
            .content();
    }

    @Override
    public String providerName() {
        return "google";
    }

    @Override
    public boolean supportsToolCalling() {
        return false;
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
            log.warn("[GoogleChatProvider] generateSuggestions failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<String> parseSuggestions(String response) {
        if (response == null || response.isBlank()) return List.of();
        try {
            int start = response.indexOf('[');
            int end = response.lastIndexOf(']');
            if (start == -1 || end <= start) {
                log.warn("[GoogleChatProvider] No JSON array in suggestion response: {}", response);
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
            log.warn("[GoogleChatProvider] Failed to parse suggestions JSON: {}", e.getMessage());
            return List.of();
        }
    }
}
