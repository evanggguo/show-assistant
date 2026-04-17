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
 * TDD 4.5.3 — Ollama 本地模型提供商（真实实现）
 * 通过 Spring AI OllamaChatModel 调用本地 Ollama 服务进行流式对话。
 *
 * 通过 application.yml 中 ai.provider=ollama（默认）激活，ai.mock 对本地模型无效。
 * Ollama 服务地址由 spring.ai.ollama.base-url 配置（默认 http://localhost:11434）。
 */
@Slf4j
public class OllamaChatProvider implements AiChatProvider {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String SUGGESTION_PROMPT_TEMPLATE =
        "请根据以下助手回答，生成 2-3 个访客可能感兴趣的跟进问题。\n\n" +
        "助手的回答：\n%s\n\n" +
        "要求：只输出 JSON 数组，不含任何其他文字，格式：[\"问题1\", \"问题2\"]，每条不超过 20 字。";

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
