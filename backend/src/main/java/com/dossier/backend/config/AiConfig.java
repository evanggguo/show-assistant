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
 * TDD 4.5.4 — AI provider configuration
 * Registers exactly one AiChatProvider Bean based on ai.provider and ai.mock in application.yml.
 *
 * Switch rules:
 *   ai.provider=ollama (default)            → OllamaChatProvider (local real call, ai.mock ignored)
 *   ai.provider=claude, ai.mock=false       → ClaudeChatProvider (requires ANTHROPIC_API_KEY)
 *   ai.provider=claude, ai.mock=true (default) → MockChatProvider (no API key needed)
 *   ai.provider=google, ai.mock=false       → GoogleChatProvider (requires GOOGLE_AI_API_KEY)
 *   ai.provider=google, ai.mock=true (default) → MockChatProvider (no API key needed)
 *
 * Design: local models (Ollama) always make real calls; the mock flag only affects cloud providers.
 */
@Slf4j
@Configuration
public class AiConfig {

    /** Ollama local model provider (active by default). Registered when ai.provider=ollama; ai.mock is ignored. */
    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "ollama", matchIfMissing = true)
    public AiChatProvider ollamaAiChatProvider(OllamaChatModel chatModel) {
        log.info("AI provider: [ollama] — using local Ollama model (mock flag ignored)");
        return new OllamaChatProvider(chatModel);
    }

    /** Claude cloud provider configuration (active when ai.provider=claude). */
    @Configuration
    @ConditionalOnProperty(name = "ai.provider", havingValue = "claude")
    static class ClaudeProviderConfig {

        /** Real Claude provider. Active when ai.provider=claude and ai.mock=false. Requires ANTHROPIC_API_KEY. */
        @Bean
        @ConditionalOnProperty(name = "ai.mock", havingValue = "false")
        public AiChatProvider claudeAiChatProvider(AnthropicChatModel chatModel) {
            log.info("AI provider: [claude] — using Anthropic Claude API");
            return new ClaudeChatProvider(chatModel);
        }

        /** Mock provider (cloud fallback). Active when ai.provider=claude and ai.mock=true (default). No API key required. */
        @Bean
        @ConditionalOnProperty(name = "ai.mock", havingValue = "true", matchIfMissing = true)
        public AiChatProvider mockAiChatProvider() {
            log.info("AI provider: [mock] — claude selected but ai.mock=true, using simulated responses");
            return new MockChatProvider();
        }
    }

    /** Google Generative AI (Gemini) cloud provider configuration (active when ai.provider=google). */
    @Configuration
    @ConditionalOnProperty(name = "ai.provider", havingValue = "google")
    static class GoogleProviderConfig {

        /** Real Google AI Studio (Gemini) provider. Active when ai.provider=google and ai.mock=false. Requires GOOGLE_AI_API_KEY. */
        @Bean
        @ConditionalOnProperty(name = "ai.mock", havingValue = "false")
        public AiChatProvider googleAiChatProvider(GoogleGenAiChatModel chatModel) {
            log.info("AI provider: [google] — using Google Gemini API");
            return new GoogleChatProvider(chatModel);
        }

        /** Mock provider (cloud fallback). Active when ai.provider=google and ai.mock=true (default). No API key required. */
        @Bean
        @ConditionalOnProperty(name = "ai.mock", havingValue = "true", matchIfMissing = true)
        public AiChatProvider googleMockAiChatProvider() {
            log.info("AI provider: [mock] — google selected but ai.mock=true, using simulated responses");
            return new MockChatProvider();
        }
    }
}
