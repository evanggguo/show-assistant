package com.showassistant.backend.ai.provider;

import com.showassistant.backend.chat.tool.SuggestFollowupsTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * TDD 4.5.3 — Mock AI 提供商
 * 返回固定的模拟 token 流，不调用任何真实 AI API。
 * 适用于本地开发、CI 测试等无模型环境。
 *
 * 通过 application.yml 中 ai.mock=true（默认）激活。
 *
 * 模拟 Tool Use 行为：在返回 token 流之前，
 * 直接调用 SuggestFollowupsTool.suggestFollowups() 以触发 suggestions 捕获，
 * 使 ChatService 的后续逻辑（保存 suggestions、推送 done 事件）能正常运行。
 */
@Slf4j
public class MockChatProvider implements AiChatProvider {

    private static final List<String> MOCK_TOKENS = List.of(
        "您好！", "我是展示助理，", "当前运行在 **Mock 模式**。\n\n",
        "如需接入真实模型，请在配置中设置：\n",
        "- 本地模型：`AI_MOCK=false`，`AI_PROVIDER=ollama`\n",
        "- Claude：`AI_MOCK=false`，`AI_PROVIDER=claude`，`ANTHROPIC_API_KEY=<key>`"
    );

    private static final List<String> MOCK_SUGGESTIONS = List.of(
        "如何切换到真实模型？",
        "系统支持哪些 AI 提供商？",
        "如何配置 Ollama 本地模型？"
    );

    @Override
    public Flux<String> streamChat(List<Message> messages, Object... tools) {
        log.debug("[MockChatProvider] streamChat called, simulating {} tokens", MOCK_TOKENS.size());

        // 模拟 Tool Use：找到 SuggestFollowupsTool 并直接调用，触发 suggestions 捕获
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
