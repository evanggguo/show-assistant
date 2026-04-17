package com.dossier.backend.config;

import com.dossier.backend.ai.provider.AiChatProvider;
import com.dossier.backend.ai.provider.ClaudeChatProvider;
import com.dossier.backend.ai.provider.GoogleChatProvider;
import com.dossier.backend.ai.provider.MockChatProvider;
import com.dossier.backend.ai.provider.OllamaChatProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TDD 4.5.4 — AI 提供商配置
 * 根据 application.yml 中的 ai.provider 和 ai.mock 条件，
 * 注册唯一的 AiChatProvider Bean。
 *
 * 切换规则：
 *   ai.provider=ollama（默认）              → OllamaChatProvider（本地真实调用，忽略 ai.mock）
 *   ai.provider=claude, ai.mock=false       → ClaudeChatProvider（需要 ANTHROPIC_API_KEY）
 *   ai.provider=claude, ai.mock=true（默认）→ MockChatProvider（无需 API Key）
 *   ai.provider=google, ai.mock=false       → GoogleChatProvider（需要 GOOGLE_AI_API_KEY）
 *   ai.provider=google, ai.mock=true（默认）→ MockChatProvider（无需 API Key）
 *
 * 设计原则：本地物理模型（Ollama）始终真实调用，mock 开关仅对云端提供商生效。
 */
@Slf4j
@Configuration
public class AiConfig {

    /**
     * Ollama 本地模型提供商（默认激活）
     * ai.provider=ollama 时注册，忽略 ai.mock——本地模型始终真实调用。
     */
    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "ollama", matchIfMissing = true)
    public AiChatProvider ollamaAiChatProvider(OllamaChatModel chatModel) {
        log.info("AI provider: [ollama] — using local Ollama model (mock flag ignored)");
        return new OllamaChatProvider(chatModel);
    }

    /**
     * Claude 云端模型提供商配置（ai.provider=claude 时激活）
     */
    @Configuration
    @ConditionalOnProperty(name = "ai.provider", havingValue = "claude")
    static class ClaudeProviderConfig {

        /**
         * 真实 Claude 提供商
         * ai.provider=claude, ai.mock=false 时激活
         * 需要环境变量 ANTHROPIC_API_KEY
         */
        @Bean
        @ConditionalOnProperty(name = "ai.mock", havingValue = "false")
        public AiChatProvider claudeAiChatProvider(AnthropicChatModel chatModel) {
            log.info("AI provider: [claude] — using Anthropic Claude API");
            return new ClaudeChatProvider(chatModel);
        }

        /**
         * Mock 提供商（cloud fallback）
         * ai.provider=claude, ai.mock=true（默认）时激活，无需 API Key。
         */
        @Bean
        @ConditionalOnProperty(name = "ai.mock", havingValue = "true", matchIfMissing = true)
        public AiChatProvider mockAiChatProvider() {
            log.info("AI provider: [mock] — claude selected but ai.mock=true, using simulated responses");
            return new MockChatProvider();
        }
    }

    /**
     * Google Generative AI（Gemini）云端模型提供商配置（ai.provider=google 时激活）
     */
    @Configuration
    @ConditionalOnProperty(name = "ai.provider", havingValue = "google")
    static class GoogleProviderConfig {

        /**
         * 真实 Google AI Studio（Gemini）提供商
         * ai.provider=google, ai.mock=false 时激活
         * 需要环境变量 GOOGLE_AI_API_KEY（Google AI Studio API Key）
         */
        @Bean
        @ConditionalOnProperty(name = "ai.mock", havingValue = "false")
        public AiChatProvider googleAiChatProvider(GoogleGenAiChatModel chatModel) {
            log.info("AI provider: [google] — using Google Gemini API");
            return new GoogleChatProvider(chatModel);
        }

        /**
         * Mock 提供商（cloud fallback）
         * ai.provider=google, ai.mock=true（默认）时激活，无需 API Key。
         */
        @Bean
        @ConditionalOnProperty(name = "ai.mock", havingValue = "true", matchIfMissing = true)
        public AiChatProvider googleMockAiChatProvider() {
            log.info("AI provider: [mock] — google selected but ai.mock=true, using simulated responses");
            return new MockChatProvider();
        }
    }
}
