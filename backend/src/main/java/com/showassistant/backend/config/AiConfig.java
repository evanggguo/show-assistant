package com.showassistant.backend.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TDD 3.2 — Spring AI 配置
 * 配置 ChatClient Bean，ChatService 通过注入使用
 */
@Configuration
public class AiConfig {

    /**
     * TDD 3.2.1 — 创建 ChatClient Bean
     * 基于 Spring AI 自动配置的 ChatModel 构建 ChatClient
     *
     * @param chatModel Spring AI 自动配置的 ChatModel（对应 application.yml 中的 anthropic 配置）
     * @return 配置好的 ChatClient 实例
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
