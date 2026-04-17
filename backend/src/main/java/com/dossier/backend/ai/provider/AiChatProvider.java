package com.dossier.backend.ai.provider;

import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * TDD 4.5.2 — AI 对话提供商接口
 * 抽象流式对话能力，ChatService 只依赖此接口，不感知底层模型。
 * 实现类通过 AiProviderConfig 按配置条件注册为 Spring Bean。
 */
public interface AiChatProvider {

    /**
     * 流式对话。
     * 实现类负责调用对应模型 API，将 token 增量以 Flux<String> 返回。
     * 工具调用（如 SuggestFollowupsTool）由 Spring AI 框架自动触发，
     * Mock 实现则手动调用工具方法以模拟该行为。
     *
     * @param messages 完整消息列表（SystemMessage + 历史 + 当前 UserMessage）
     * @param tools    需要注册的工具实例（varargs）
     * @return token 增量流
     */
    Flux<String> streamChat(List<Message> messages, Object... tools);

    /**
     * 提供商标识，用于日志。
     */
    String providerName();

    /**
     * 是否支持通过 Tool Use（Function Calling）获取建议问题。
     * 返回 false 时，ChatService 将跳过工具注册并使用 generateSuggestions() fallback，
     * 同时 PromptAssembler 会省略要求调用 suggest_followups 的指令，避免模型输出乱码。
     */
    default boolean supportsToolCalling() {
        return true;
    }

    /**
     * Fallback：当 streamChat 的 Tool Use 未触发时（如小模型不支持 Function Calling），
     * 通过额外的非流式调用生成建议问题。
     * 默认返回空列表，OllamaChatProvider 覆盖此方法。
     *
     * @param messages       同 streamChat 的消息列表
     * @param assistantReply 刚完成的流式回复全文，用于构造 fallback prompt
     * @return 建议问题列表（最多 3 条），无法生成时返回空列表
     */
    default List<String> generateSuggestions(List<Message> messages, String assistantReply) {
        return List.of();
    }
}
