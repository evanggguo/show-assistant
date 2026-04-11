package com.showassistant.backend.chat;

import com.showassistant.backend.knowledge.dto.KnowledgeEntryDto;
import com.showassistant.backend.owner.dto.OwnerProfileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TDD 4.3.2 — System Prompt 构建器
 * 根据 Owner 信息和 RAG 检索结果，动态构建发送给 AI 模型的 System Prompt。
 * 设计原则：
 * - 始终包含 Owner 身份信息（name、tagline）
 * - 当存在 RAG 上下文时注入检索内容，空时省略该段落（避免无意义占位）
 * - 包含要求模型调用 suggest_followups 工具的明确指令
 */
@Slf4j
@Component
public class PromptAssembler {

    /**
     * TDD 4.3.2 — 构建完整的 System Prompt
     * 将 Owner 信息和 RAG 检索结果组合成结构化的 System Prompt。
     * retrievedContext 为空列表时不注入"参考资料"段落。
     *
     * @param ownerProfile     Owner 的公开简介信息
     * @param retrievedContext RAG 检索到的相关知识条目（Phase 2 为空列表）
     * @return 组装好的 System Prompt 字符串
     */
    public String assemble(OwnerProfileResponse ownerProfile, List<KnowledgeEntryDto> retrievedContext) {
        StringBuilder sb = new StringBuilder();

        // 身份设定
        sb.append("你是 ").append(ownerProfile.getName()).append(" 的 AI 助手。\n");

        if (ownerProfile.getTagline() != null && !ownerProfile.getTagline().isBlank()) {
            sb.append(ownerProfile.getName()).append(" 的简介：").append(ownerProfile.getTagline()).append("\n");
        }

        sb.append("\n");
        sb.append("你的职责是代表 ").append(ownerProfile.getName())
          .append(" 与访客进行友好、专业的对话，介绍其工作、项目和经历。\n");
        sb.append("回答应简洁、自然、有帮助，使用中文回复（除非访客使用其他语言）。\n");

        // 注入 RAG 检索上下文（仅在有内容时）
        if (retrievedContext != null && !retrievedContext.isEmpty()) {
            sb.append("\n## 参考资料\n");
            sb.append("以下是与当前问题相关的背景信息，请在回答时参考：\n\n");
            for (int i = 0; i < retrievedContext.size(); i++) {
                KnowledgeEntryDto entry = retrievedContext.get(i);
                sb.append(i + 1).append(". ");
                if (entry.getTitle() != null && !entry.getTitle().isBlank()) {
                    sb.append("**").append(entry.getTitle()).append("**\n");
                }
                sb.append(entry.getContent()).append("\n\n");
            }
        }

        // 工具调用指令（TDD 4.4）
        sb.append("\n## 重要指令\n");
        sb.append("在完成每次回答后，你必须调用 `suggest_followups` 工具，");
        sb.append("提供 2-3 个与当前话题相关的跟进问题，帮助访客深入了解。\n");
        sb.append("这些问题应该自然、有价值，引导访客进一步探索感兴趣的内容。\n");

        String prompt = sb.toString();
        log.debug("Assembled system prompt length={}, hasRagContext={}", prompt.length(),
            retrievedContext != null && !retrievedContext.isEmpty());
        return prompt;
    }
}
