package com.showassistant.backend.ai.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.ollama.OllamaChatModel;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * TDD 4.5.3 — Ollama 本地模型提供商（真实实现）
 * 通过 Spring AI OllamaChatModel 调用本地 Ollama 服务进行流式对话。
 *
 * 通过 application.yml 中 ai.mock=false，ai.provider=ollama 激活。
 * Ollama 服务地址由 spring.ai.ollama.base-url 配置（默认 http://localhost:11434）。
 */
@Slf4j
public class OllamaChatProvider implements AiChatProvider {

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
}
