package com.showassistant.backend.ai.provider;

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
 * Google AI Studio（Gemini）云端模型提供商
 * 通过 Spring AI GoogleGenAiChatModel 调用 Google AI Studio API 进行流式对话。
 *
 * 通过 application.yml 中 ai.provider=google, ai.mock=false 激活。
 * 需要在环境变量中设置 GOOGLE_AI_API_KEY（Google AI Studio 的 API Key）。
 *
 * 注意：不向 Gemini 传递工具（避免 function calling 后续流解析异常），
 * 改用 generateSuggestions() fallback 生成建议问题。
 */
@Slf4j
public class GoogleChatProvider implements AiChatProvider {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String SUGGESTION_PROMPT_TEMPLATE =
        "请根据以下助手回答，生成 2-3 个访客可能感兴趣的跟进问题。\n\n" +
        "助手的回答：\n%s\n\n" +
        "要求：只输出 JSON 数组，不含任何其他文字，格式：[\"问题1\", \"问题2\"]，每条不超过 20 字。";

    private final ChatClient chatClient;

    public GoogleChatProvider(GoogleGenAiChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @Override
    public Flux<String> streamChat(List<Message> messages, Object... tools) {
        log.debug("[GoogleChatProvider] streamChat called with {} messages", messages.size());
        // 不传递 tools：Gemini function calling 后续流存在解析异常，
        // 改由 generateSuggestions() fallback 生成建议。
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
