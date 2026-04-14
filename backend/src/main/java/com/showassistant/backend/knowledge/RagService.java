package com.showassistant.backend.knowledge;

import com.showassistant.backend.ai.EmbeddingService;
import com.showassistant.backend.knowledge.dto.KnowledgeEntryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Phase 3 — RAG（检索增强生成）服务
 * 将用户查询向量化后，通过余弦相似度检索最相关的知识条目。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final KnowledgeRepository knowledgeRepository;
    private final EmbeddingService embeddingService;

    /**
     * 检索与 query 相关的知识条目
     *
     * @param ownerId Owner ID
     * @param query   用户查询文本
     * @param topK    最多返回条数
     * @return 相关知识条目列表（embedding 不可用时返回空列表）
     */
    public List<KnowledgeEntryDto> retrieve(Long ownerId, String query, int topK) {
        log.debug("RAG retrieve: ownerId={}, query='{}', topK={}", ownerId, query, topK);

        float[] embedding = embeddingService.embed(query);
        if (embedding.length == 0) {
            log.warn("Embedding unavailable, skipping RAG for ownerId={}", ownerId);
            return Collections.emptyList();
        }

        String vectorStr = toVectorString(embedding);
        List<KnowledgeEntry> entries =
            knowledgeRepository.findSimilarByOwner(ownerId, vectorStr, topK);

        log.debug("RAG retrieved {} entries for ownerId={}", entries.size(), ownerId);
        return entries.stream().map(this::mapToDto).toList();
    }

    /**
     * 便捷重载，使用默认 topK=5
     */
    public List<KnowledgeEntryDto> retrieve(Long ownerId, String query) {
        return retrieve(ownerId, query, 5);
    }

    /**
     * 将 float[] 向量格式化为 pgvector 字符串格式 "[x1,x2,...,xn]"
     */
    private String toVectorString(float[] embedding) {
        return IntStream.range(0, embedding.length)
            .mapToObj(i -> String.valueOf(embedding[i]))
            .collect(Collectors.joining(",", "[", "]"));
    }

    private KnowledgeEntryDto mapToDto(KnowledgeEntry entry) {
        return KnowledgeEntryDto.builder()
            .id(entry.getId())
            .type(entry.getType().name())
            .title(entry.getTitle())
            .content(entry.getContent())
            .createdAt(entry.getCreatedAt())
            .build();
    }
}
