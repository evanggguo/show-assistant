package com.showassistant.backend.ai.provider;

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
}
