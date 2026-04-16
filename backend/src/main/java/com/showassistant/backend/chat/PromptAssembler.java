package com.showassistant.backend.chat;

import com.showassistant.backend.knowledge.dto.KnowledgeEntryDto;
import com.showassistant.backend.owner.dto.OwnerProfileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TDD 4.3.2 — System Prompt 构建器
 *
 * 防幻觉设计：
 * - 始终注入"行为准则"段落，强制 AI 只能基于知识库内容回答
 * - RAG 有内容时：注入知识条目，AI 必须基于此作答
 * - RAG 为空时：明确声明无可用内容，AI 必须礼貌告知用户并引导联系 owner
 * - 对于问候等通用对话例外处理，避免过度拒绝
 */
@Slf4j
@Component
public class PromptAssembler {

    public String assemble(OwnerProfileResponse ownerProfile, List<KnowledgeEntryDto> retrievedContext) {
        return assemble(ownerProfile, retrievedContext, true);
    }

    public String assemble(OwnerProfileResponse ownerProfile, List<KnowledgeEntryDto> retrievedContext,
                           boolean includeToolInstruction) {
        StringBuilder sb = new StringBuilder();
        boolean hasRagContent = retrievedContext != null && !retrievedContext.isEmpty();

        // ── 身份设定 ───────────────────────────────────────────────
        sb.append("你是 ").append(ownerProfile.getName()).append(" 的 AI 个人助手。\n");
        if (ownerProfile.getTagline() != null && !ownerProfile.getTagline().isBlank()) {
            sb.append(ownerProfile.getName()).append(" 的简介：")
              .append(ownerProfile.getTagline()).append("\n");
        }
        sb.append("\n");

        // ── 行为准则（始终注入，核心防幻觉约束） ───────────────────
        sb.append("## Rules (MUST follow strictly)\n");
        sb.append("1. **LANGUAGE**: Always reply in the SAME language the visitor used. ");
        sb.append("If the visitor writes in English, reply in English. ");
        sb.append("If in Chinese, reply in Chinese. The knowledge base language does NOT affect your reply language.\n");
        sb.append("2. Only answer based on the knowledge base content provided below.\n");
        sb.append("3. Do not use any information outside the knowledge base, including your training data.\n");
        sb.append("4. Do not speculate, add, or fabricate content not explicitly in the knowledge base.\n");
        sb.append("5. For greetings or small talk, respond naturally and friendly.\n");
        sb.append("6. If the knowledge base has no answer, politely say so and suggest contacting ")
          .append(ownerProfile.getName()).append(" directly.\n\n");

        // ── 知识库（有内容 / 无内容 两种场景） ──────────────────────
        sb.append("## ").append(ownerProfile.getName()).append(" 的知识库\n");

        if (hasRagContent) {
            sb.append("以下是关于 ").append(ownerProfile.getName())
              .append(" 的参考资料，你的所有具体回答必须严格基于此内容：\n\n");
            for (int i = 0; i < retrievedContext.size(); i++) {
                KnowledgeEntryDto entry = retrievedContext.get(i);
                sb.append(i + 1).append(". ");
                if (entry.getTitle() != null && !entry.getTitle().isBlank()) {
                    sb.append("**").append(entry.getTitle()).append("**\n");
                }
                sb.append(entry.getContent()).append("\n\n");
            }
        } else {
            sb.append("当前暂无与该问题相关的知识库内容。\n");
            sb.append("请告知访客你暂时无法提供该信息，并友好地建议其直接联系 ")
              .append(ownerProfile.getName()).append(" 获取更多信息。\n\n");
        }

        // ── 工具调用指令（仅在提供商支持 Function Calling 时注入） ──
        if (includeToolInstruction) {
            sb.append("## Important Instruction\n");
            sb.append("After completing your answer, you MUST call the `suggest_followups` tool ");
            sb.append("with 2-3 relevant follow-up questions. ");
            sb.append("These questions MUST be in the same language the visitor used. ");
            sb.append("Make them natural and valuable to guide the visitor further.\n");
        }

        String prompt = sb.toString();
        log.debug("Assembled system prompt length={}, hasRagContext={}, includeToolInstruction={}",
            prompt.length(), hasRagContent, includeToolInstruction);
        return prompt;
    }
}
