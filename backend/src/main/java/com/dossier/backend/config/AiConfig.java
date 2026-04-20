package com.dossier.backend.config;

import com.dossier.backend.ai.provider.AiChatProvider;
import com.dossier.backend.ai.provider.ClaudeChatProvider;
import com.dossier.backend.ai.provider.GcpEnvironmentDetector;
import com.dossier.backend.ai.provider.GoogleChatProvider;
import com.dossier.backend.ai.provider.MockChatProvider;
import com.dossier.backend.ai.provider.OllamaChatProvider;
import com.dossier.backend.ai.provider.VertexAiChatProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TDD 4.5.4 — AI provider configuration
 * Registers exactly one AiChatProvider Bean based on ai.provider and ai.mock in application.yml.
 *
 * Switch rules:
 *   ai.provider=ollama (default)                      → OllamaChatProvider (local, ai.mock ignored)
 *   ai.provider=claude, ai.mock=false                 → ClaudeChatProvider (requires ANTHROPIC_API_KEY)
 *   ai.provider=claude, ai.mock=true (default)        → MockChatProvider
 *   ai.provider=google, ai.mock=false, on GCP         → VertexAiChatProvider (ADC, no API key needed)
 *   ai.provider=google, ai.mock=false, off GCP        → GoogleChatProvider (requires GOOGLE_AI_API_KEY)
 *   ai.provider=google, ai.mock=true (default)        → MockChatProvider
 *
 * GCP detection: GcpEnvironmentDetector checks GOOGLE_CLOUD_PROJECT env var first, then probes
 * the metadata server (http://metadata.google.internal) with a 500ms timeout.
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

    /**
     * Google/Vertex AI cloud provider configuration (active when ai.provider=google).
     *
     * On GCP (detected by GcpEnvironmentDetector): uses VertexAiGeminiChatModel with ADC.
     * Off GCP: uses GoogleGenAiChatModel with GOOGLE_AI_API_KEY.
     * ObjectProvider<T> is used for both model types so that a missing bean causes a graceful
     * fallback rather than a hard startup failure.
     */
    @Configuration
    @ConditionalOnProperty(name = "ai.provider", havingValue = "google")
    static class GoogleProviderConfig {

        /**
         * Real Google family provider. Active when ai.provider=google and ai.mock=false.
         * Selects Vertex AI when running on GCP (ADC), or Google AI Studio when off GCP (API key).
         */
        @Bean
        @ConditionalOnProperty(name = "ai.mock", havingValue = "false")
        public AiChatProvider googleAiChatProvider(
                ObjectProvider<GoogleGenAiChatModel> googleModel,
                ObjectProvider<VertexAiGeminiChatModel> vertexModel) {

            if (GcpEnvironmentDetector.isRunningOnGcp()) {
                VertexAiGeminiChatModel vm = vertexModel.getIfAvailable();
                if (vm != null) {
                    log.info("AI provider: [vertex-ai] — GCP detected, using Vertex AI Gemini via ADC");
                    return new VertexAiChatProvider(vm);
                }
                log.warn("AI provider: GCP detected but VertexAiGeminiChatModel bean is unavailable "
                    + "(check GOOGLE_CLOUD_PROJECT / spring.ai.vertex.ai.gemini.project-id); "
                    + "falling back to Google AI Studio");
            }

            log.info("AI provider: [google] — using Google AI Studio Gemini API");
            return new GoogleChatProvider(googleModel.getObject());
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
