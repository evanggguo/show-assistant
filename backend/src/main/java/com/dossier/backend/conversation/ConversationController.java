package com.dossier.backend.conversation;

import com.dossier.backend.common.response.ApiResponse;
import com.dossier.backend.conversation.dto.ConversationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TDD 6.5 — Conversation API controller
 * Provides conversation history query endpoints
 */
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * TDD 6.5.1 — GET /api/conversations/{id}
     * Get the full history for a conversation, including all messages and the latest dynamic suggestions
     *
     * @param id conversation ID
     * @return conversation detail (with message list and latest suggestions)
     */
    @GetMapping("/{id}")
    public ApiResponse<ConversationResponse> getConversation(@PathVariable Long id) {
        return ApiResponse.ok(conversationService.getConversation(id));
    }
}
