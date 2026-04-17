package com.dossier.backend.chat;

import com.dossier.backend.ai.provider.AiChatProvider;
import com.dossier.backend.chat.dto.ChatRequest;
import com.dossier.backend.chat.tool.SuggestFollowupsTool;
import com.dossier.backend.conversation.Conversation;
import com.dossier.backend.conversation.ConversationService;
import com.dossier.backend.conversation.Message;
import com.dossier.backend.knowledge.RagService;
import com.dossier.backend.knowledge.dto.KnowledgeEntryDto;
import com.dossier.backend.owner.OwnerService;
import com.dossier.backend.owner.dto.OwnerProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TDD 4.3 — 核心流式对话服务
 * 负责协调 RAG 检索、Spring AI 流式调用、SSE 事件推送和消息持久化的完整流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int HISTORY_LIMIT = 20;

    private final AiChatProvider aiChatProvider;
    private final ConversationService conversationService;
    private final OwnerService ownerService;
    private final RagService ragService;
    private final PromptAssembler promptAssembler;
    private final SseEventBuilder sseEventBuilder;

    @Value("${app.sse.timeout-ms:180000}")
    private long sseTimeoutMs;

    /**
     * TDD 4.3.1 — 创建 SSE 连接
     * 初始化 SseEmitter 并设置超时时间，返回给控制器
     *
     * @return 配置好超时的 SseEmitter 实例
     */
    public SseEmitter createEmitter() {
        return new SseEmitter(sseTimeoutMs);
    }

    /**
     * TDD 4.3.2 — 核心流式处理方法（异步执行）
     * 完整的流式对话处理流程：
     * 1. 创建/加载会话
     * 2. 保存 user message
     * 3. 调用 RagService 检索知识（Phase 2 返回空列表）
     * 4. 加载历史消息，构造 Spring AI Messages 列表
     * 5. 构建 System Prompt（含 Owner 信息和 RAG 上下文）
     * 6. 创建 per-request SuggestFollowupsTool 实例
     * 7. 通过 Spring AI chatClient 流式订阅
     * 8. onNext: 累积 fullText + 推送 token SSE
     * 9. onComplete: 保存 assistant message + suggestions，推送 done SSE
     * 10. onError: 推送 error SSE，完成 emitter
     *
     * @param req     聊天请求（含 conversationId、message、history）
     * @param emitter 已初始化的 SSE 连接
     */
    /**
     * 向后兼容的旧入口（owner_id=1），供旧路由 /api/chat/stream 使用
     */
    @Async("sseTaskExecutor")
    public void handleStream(ChatRequest req, SseEmitter emitter) {
        handleStream(req, emitter, 1L);
    }

    @Async("sseTaskExecutor")
    public void handleStream(ChatRequest req, SseEmitter emitter, Long ownerId) {
        try {
            // 步骤 1：创建或加载会话
            Long conversationId = resolveConversationId(req, ownerId);

            // 步骤 2：保存 user message
            conversationService.saveUserMessage(conversationId, req.message());

            // 步骤 3：RAG 检索（Phase 2 返回空列表）
            List<KnowledgeEntryDto> ragContext = ragService.retrieve(ownerId, req.message());

            // 步骤 4：构建 Spring AI 消息列表
            boolean toolCallingEnabled = aiChatProvider.supportsToolCalling();
            List<org.springframework.ai.chat.messages.Message> aiMessages =
                buildAiMessages(req, conversationId, ragContext, toolCallingEnabled, ownerId);

            // 步骤 5（在 aiMessages 中已包含 system message）

            // 步骤 6：创建 per-request SuggestFollowupsTool 实例（非 Spring Bean）
            SuggestFollowupsTool suggestTool = new SuggestFollowupsTool();

            // 步骤 7-9：流式调用并处理（TDD 4.5 — 通过 AiChatProvider 接口调用，屏蔽底层模型差异）
            AtomicReference<StringBuilder> fullTextRef = new AtomicReference<>(new StringBuilder());
            final Long finalConversationId = conversationId;
            log.debug("Using AI provider: {}, toolCalling={}", aiChatProvider.providerName(), toolCallingEnabled);

            Object[] tools = toolCallingEnabled ? new Object[]{suggestTool} : new Object[0];
            aiChatProvider.streamChat(aiMessages, tools)
                .subscribe(
                    // onNext: 追加 fullText + 发送 token SSE
                    token -> {
                        fullTextRef.get().append(token);
                        sseEventBuilder.sendToken(emitter, token);
                    },
                    // onError: 推送 error SSE
                    error -> {
                        log.error("Stream error for conversation={}: {}", finalConversationId, error.getMessage(), error);
                        sseEventBuilder.sendError(emitter, "STREAM_ERROR", "AI 服务暂时不可用，请稍后重试");
                        emitter.completeWithError(error);
                    },
                    // onComplete: 保存消息 + 推送 done SSE
                    () -> {
                        String fullText = fullTextRef.get().toString();
                        List<String> suggestions = suggestTool.getCapturedSuggestions();

                        // Fallback：Tool Use 未触发时（如不支持 Function Calling 的小模型），
                        // 通过额外的非流式调用生成建议
                        if (suggestions.isEmpty()) {
                            log.debug("Tool Use suggestions empty for conversation={}, triggering fallback",
                                finalConversationId);
                            suggestions = aiChatProvider.generateSuggestions(aiMessages, fullText);
                            log.debug("Fallback generated {} suggestions for conversation={}",
                                suggestions.size(), finalConversationId);
                        }

                        try {
                            Message savedMsg = conversationService.saveAssistantMessage(
                                finalConversationId, fullText, suggestions);
                            sseEventBuilder.sendDone(emitter, savedMsg.getId(), suggestions);
                            emitter.complete();
                            log.debug("Stream completed for conversation={}, messageId={}",
                                finalConversationId, savedMsg.getId());
                        } catch (Exception e) {
                            log.error("Failed to save assistant message for conversation={}: {}",
                                finalConversationId, e.getMessage(), e);
                            sseEventBuilder.sendError(emitter, "SAVE_ERROR", "消息保存失败");
                            emitter.completeWithError(e);
                        }
                    }
                );

        } catch (Exception e) {
            log.error("handleStream error: {}", e.getMessage(), e);
            sseEventBuilder.sendError(emitter, "INTERNAL_ERROR", "服务内部错误，请稍后重试");
            try {
                emitter.completeWithError(e);
            } catch (Exception ignored) {
                // emitter 已关闭
            }
        }
    }

    /**
     * TDD 4.3.1 — 解析或创建会话 ID
     * conversationId 为 null（游客）时，创建新会话；
     * 否则直接使用请求中的 conversationId
     *
     * @param req 聊天请求
     * @return 有效的会话 ID
     */
    private Long resolveConversationId(ChatRequest req, Long ownerId) {
        if (req.conversationId() == null) {
            // 游客新会话：user_id=null
            Conversation newConversation = conversationService.createConversation(ownerId, null);
            log.debug("Created new guest conversation id={} for ownerId={}", newConversation.getId(), ownerId);
            return newConversation.getId();
        }
        return req.conversationId();
    }

    /**
     * TDD 4.3.2 — 构建 Spring AI 消息列表
     * 包含以下顺序：
     * 1. SystemMessage（含 Owner 信息和 RAG 上下文）
     * 2. 历史消息（来自数据库，或游客请求中携带的 history）
     * 3. 当前用户消息
     *
     * @param req            聊天请求
     * @param conversationId 当前会话 ID
     * @param ragContext     RAG 检索结果
     * @return Spring AI Messages 列表
     */
    private List<org.springframework.ai.chat.messages.Message> buildAiMessages(
        ChatRequest req, Long conversationId, List<KnowledgeEntryDto> ragContext,
        boolean includeToolInstruction, Long ownerId) {

        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();

        // System Message
        OwnerProfileResponse ownerProfile = ownerService.getOwnerProfile(ownerId);
        String systemPrompt = promptAssembler.assemble(ownerProfile, ragContext, includeToolInstruction, req.message());
        messages.add(new SystemMessage(systemPrompt));

        // 历史消息
        if (req.conversationId() == null && req.history() != null && !req.history().isEmpty()) {
            // 游客模式：使用前端携带的历史（排除刚刚保存的那条 user message，避免重复）
            List<ChatRequest.HistoryMessage> history = req.history();
            for (ChatRequest.HistoryMessage h : history) {
                if ("user".equals(h.role())) {
                    messages.add(new UserMessage(h.content()));
                } else if ("assistant".equals(h.role())) {
                    messages.add(new AssistantMessage(h.content()));
                }
            }
        } else if (req.conversationId() != null) {
            // 登录用户：从数据库加载历史（不含刚保存的当前 user message）
            List<com.dossier.backend.conversation.dto.MessageResponse> dbHistory =
                conversationService.loadHistory(conversationId, HISTORY_LIMIT);
            // 排除最后一条（刚保存的 user message）
            int endIdx = dbHistory.size() > 0 ? dbHistory.size() - 1 : 0;
            for (int i = 0; i < endIdx; i++) {
                var msgResp = dbHistory.get(i);
                if ("user".equals(msgResp.getRole())) {
                    messages.add(new UserMessage(msgResp.getContent()));
                } else if ("assistant".equals(msgResp.getRole())) {
                    messages.add(new AssistantMessage(msgResp.getContent()));
                }
            }
        }

        // 当前用户消息
        messages.add(new UserMessage(req.message()));

        return messages;
    }
}
