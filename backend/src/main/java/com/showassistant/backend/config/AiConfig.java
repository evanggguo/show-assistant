package com.showassistant.backend.config;

import com.showassistant.backend.ai.provider.AiChatProvider;
import com.showassistant.backend.ai.provider.ClaudeChatProvider;
import com.showassistant.backend.ai.provider.MockChatProvider;
import com.showassistant.backend.ai.provider.OllamaChatProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TDD 4.5.4 — AI 提供商配置
 * 根据 application.yml 中的 ai.mock 和 ai.provider 条件，
 * 注册唯一的 AiChatProvider Bean。
 *
 * 切换规则：
 *   ai.mock=true（默认）       → MockChatProvider（无需任何外部服务）
 *   ai.mock=false, ai.provider=ollama（默认） → OllamaChatProvider（本地 Ollama）
 *   ai.mock=false, ai.provider=claude          → ClaudeChatProvider（需要 ANTHROPIC_API_KEY）
 */
@Slf4j
@Configuration
public class AiConfig {

    /**
     * Mock 提供商（默认激活）
     * ai.mock=true 时注册，无需 API Key 或 Ollama 服务。
     */
    @Bean
    @ConditionalOnProperty(name = "ai.mock", havingValue = "true", matchIfMissing = true)
    public AiChatProvider mockAiChatProvider() {
        log.info("AI provider: [mock] — using simulated responses");
        return new MockChatProvider();
    }

    /**
     * 真实提供商配置（ai.mock=false 时激活）
     */
    @Configuration
    @ConditionalOnProperty(name = "ai.mock", havingValue = "false")
    static class RealProviderConfig {

        /**
         * Ollama 本地模型提供商
         * ai.mock=false, ai.provider=ollama（默认）时激活
         */
        @Bean
        @ConditionalOnProperty(name = "ai.provider", havingValue = "ollama", matchIfMissing = true)
        public AiChatProvider ollamaAiChatProvider(OllamaChatModel chatModel) {
            log.info("AI provider: [ollama] — using local Ollama model");
            return new OllamaChatProvider(chatModel);
        }

        /**
         * Claude 云端模型提供商
         * ai.mock=false, ai.provider=claude 时激活
         * 需要环境变量 ANTHROPIC_API_KEY
         */
        @Bean
        @ConditionalOnProperty(name = "ai.provider", havingValue = "claude")
        public AiChatProvider claudeAiChatProvider(AnthropicChatModel chatModel) {
            log.info("AI provider: [claude] — using Anthropic Claude API");
            return new ClaudeChatProvider(chatModel);
        }
    }
}
