package com.dossier.backend.ai.provider;

import com.dossier.backend.chat.tool.SuggestFollowupsTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * TDD 4.5.3 — Mock AI provider
 * Returns a fixed simulated token stream without calling any real AI API.
 * Only activated for cloud providers (e.g. Claude) when ai.mock=true; Ollama is unaffected.
 *
 * Simulates Tool Use: before returning the token stream, it directly calls
 * SuggestFollowupsTool.suggestFollowups() to trigger suggestions capture,
 * allowing ChatService's downstream logic (save suggestions, push done event) to work normally.
 */
@Slf4j
public class MockChatProvider implements AiChatProvider {

    private static final List<String> MOCK_TOKENS = List.of(
        "Hello! ", "I'm the Dossier AI assistant, ", "currently running in **Mock mode**.\n\n",
        "To connect a real model, set the following in your configuration:\n",
        "- Local model (recommended): `AI_PROVIDER=ollama` (no other config needed; mock flag is ignored for local models)\n",
        "- Claude: `AI_PROVIDER=claude`, `AI_MOCK=false`, `ANTHROPIC_API_KEY=<key>`"
    );

    private static final List<String> MOCK_SUGGESTIONS = List.of(
        "How do I switch to a real model?",
        "Which AI providers are supported?",
        "How do I configure the Ollama local model?"
    );

    @Override
    public Flux<String> streamChat(List<Message> messages, Object... tools) {
        log.debug("[MockChatProvider] streamChat called, simulating {} tokens", MOCK_TOKENS.size());

        // Simulate Tool Use: find SuggestFollowupsTool and invoke it directly to trigger suggestions capture
        for (Object tool : tools) {
            if (tool instanceof SuggestFollowupsTool suggestTool) {
                suggestTool.suggestFollowups(MOCK_SUGGESTIONS);
                log.debug("[MockChatProvider] simulated suggest_followups tool call");
                break;
            }
        }

        return Flux.fromIterable(MOCK_TOKENS);
    }

    @Override
    public String providerName() {
        return "mock";
    }
}
