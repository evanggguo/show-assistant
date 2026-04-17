package com.dossier.backend.conversation;

import com.dossier.backend.common.response.ApiResponse;
import com.dossier.backend.conversation.dto.ConversationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TDD 6.5 — 会话 API 控制器
 * 提供会话历史查询接口
 */
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * TDD 6.5.1 — GET /api/conversations/{id}
     * 获取指定会话的完整历史，包含所有消息和最新动态提示词
     *
     * @param id 会话 ID
     * @return 会话详情（含消息列表和最新 suggestions）
     */
    @GetMapping("/{id}")
    public ApiResponse<ConversationResponse> getConversation(@PathVariable Long id) {
        return ApiResponse.ok(conversationService.getConversation(id));
    }
}
