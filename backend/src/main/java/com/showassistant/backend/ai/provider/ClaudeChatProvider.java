package com.showassistant.backend.ai.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * TDD 4.5.3 — Claude（Anthropic）云端模型提供商（真实实现）
 * 通过 Spring AI AnthropicChatModel 调用 Anthropic Claude API 进行流式对话。
 *
 * 通过 application.yml 中 ai.mock=false，ai.provider=claude 激活。
 * 需要在环境变量中设置 ANTHROPIC_API_KEY。
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
