package com.showassistant.backend.knowledge;

import com.showassistant.backend.knowledge.dto.KnowledgeEntryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * TDD 4.2 — RAG（检索增强生成）服务
 * Phase 2: stub 实现，retrieve() 直接返回空列表。
 * Phase 3: 接入真正的向量检索——将 query 文本转为 embedding，
 *          调用 KnowledgeRepository.findSimilarByOwner() 检索最相关知识条目，
 *          过滤低相似度结果，返回 KnowledgeEntryDto 列表供 PromptAssembler 注入上下文。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    // Phase 3 依赖注入
    // private final KnowledgeRepository knowledgeRepository;
    // private final EmbeddingService embeddingService;

    /**
     * TDD 4.2 — 检索与 query 相关的知识条目
     * Phase 2 stub: 直接返回空列表，不执行向量检索。
     * Phase 3 实现向量检索：
     *   1. 调用 EmbeddingService.embed(query) 获取查询向量
     *   2. 调用 KnowledgeRepository.findSimilarByOwner(ownerId, vector, topK) 检索
     *   3. 将结果映射为 KnowledgeEntryDto 列表并返回
     *
     * @param ownerId Owner ID
     * @param query   用户的查询文本
     * @param topK    最多返回的知识条目数
     * @return 相关知识条目列表（Phase 2 始终为空列表）
     */
    public List<KnowledgeEntryDto> retrieve(Long ownerId, String query, int topK) {
        // Phase 2 stub — 直接返回空列表
        log.debug("RAG retrieve (Phase 2 stub): ownerId={}, query='{}', topK={}", ownerId, query, topK);
        return Collections.emptyList();
    }

    /**
     * TDD 4.2 — 便捷重载，使用默认 topK=5
     */
    public List<KnowledgeEntryDto> retrieve(Long ownerId, String query) {
        return retrieve(ownerId, query, 5);
    }
}
