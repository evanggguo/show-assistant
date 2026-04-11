package com.showassistant.backend.chat.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TDD 4.4 — 动态跟进提示词 Tool
 * 非 Spring Bean，每次请求创建独立实例，避免状态共享。
 * 模型在回复末尾调用 suggest_followups 工具，Spring AI 的 @Tool 注解自动捕获调用。
 * 调用后可通过 getCapturedSuggestions() 获取捕获到的建议列表。
 *
 * 设计原则（TDD 4.4.1）：
 * - per-request 实例：不是 Spring Bean，由 ChatService 在每次请求时 new 出来
 * - 状态隔离：每个实例只服务一次对话，不存在并发状态竞争
 * - 工具名称与 System Prompt 中的指令一致，模型可靠调用
 */
@Slf4j
public class SuggestFollowupsTool {

    private final List<String> capturedSuggestions = new ArrayList<>();

    /**
     * TDD 4.4.2 — suggest_followups 工具方法
     * 由 Spring AI 在模型调用工具时自动调用，将建议的跟进问题捕获到实例变量。
     * 模型需要在 System Prompt 的指引下在回复结束时调用此工具。
     *
     * @param suggestions 模型建议的 2-3 个跟进问题列表
     * @return 空字符串（工具调用结果不显示给用户）
     */
    @Tool(name = "suggest_followups",
          description = "在回答完成后调用此工具，提供 2-3 个相关的跟进问题供用户选择。" +
                        "suggestions 为 JSON 数组，每条不超过 20 字。")
    public String suggestFollowups(List<String> suggestions) {
        if (suggestions != null && !suggestions.isEmpty()) {
            capturedSuggestions.addAll(suggestions);
            log.debug("SuggestFollowupsTool captured {} suggestions", suggestions.size());
        }
        return "";
    }

    /**
     * TDD 4.4.3 — 获取捕获到的跟进建议
     * 在流式回复完成后调用，获取模型提供的动态提示词。
     * 若模型未调用工具，则返回空列表。
     *
     * @return 不可修改的建议列表
     */
    public List<String> getCapturedSuggestions() {
        return Collections.unmodifiableList(capturedSuggestions);
    }
}
