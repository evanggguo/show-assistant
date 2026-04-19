package com.dossier.backend.chat.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TDD 4.4 — Dynamic follow-up suggestions tool
 * Not a Spring Bean; a fresh instance is created per request to avoid shared state.
 * The model calls suggest_followups at the end of its reply; Spring AI's @Tool captures the call.
 *
 * Design principles (TDD 4.4.1):
 * - Per-request instance: instantiated by ChatService on every request, not a singleton Bean
 * - State isolation: each instance serves exactly one conversation, no concurrency issues
 * - Tool name matches the instruction in the System Prompt for reliable model invocation
 */
@Slf4j
public class SuggestFollowupsTool {

    private final List<String> capturedSuggestions = new ArrayList<>();

    /**
     * TDD 4.4.2 — suggest_followups tool method
     * Auto-invoked by Spring AI when the model calls the tool; captures suggested follow-up questions.
     * The model is instructed via the System Prompt to call this tool at the end of its reply.
     */
    @Tool(name = "suggest_followups",
          description = "Call this tool after completing your answer to provide 2-3 relevant follow-up questions for the user. " +
                        "IMPORTANT: Generate the questions in the SAME language the user used in their message. " +
                        "suggestions is a JSON array, each question under 30 characters.")
    public String suggestFollowups(List<String> suggestions) {
        if (suggestions != null && !suggestions.isEmpty()) {
            capturedSuggestions.addAll(suggestions);
            log.debug("SuggestFollowupsTool captured {} suggestions", suggestions.size());
        }
        return "";
    }

    /**
     * TDD 4.4.3 — Get captured follow-up suggestions
     * Called after streaming finishes. Returns an empty list if the model did not invoke the tool.
     */
    public List<String> getCapturedSuggestions() {
        return Collections.unmodifiableList(capturedSuggestions);
    }
}
