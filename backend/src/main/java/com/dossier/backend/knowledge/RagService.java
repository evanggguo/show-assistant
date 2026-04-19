package com.dossier.backend.knowledge;

import com.dossier.backend.ai.EmbeddingService;
import com.dossier.backend.knowledge.dto.KnowledgeEntryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Phase 3 — RAG (Retrieval-Augmented Generation) service.
 * Embeds the user query and retrieves the most relevant knowledge entries by cosine similarity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final KnowledgeRepository knowledgeRepository;
    private final EmbeddingService embeddingService;

    /**
     * Retrieve knowledge entries relevant to the query.
     *
     * @param ownerId owner ID
     * @param query   user query text
     * @param topK    maximum number of results to return
     * @return relevant entries, or an empty list if embedding is unavailable
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

    /** Convenience overload using the default topK=5. */
    public List<KnowledgeEntryDto> retrieve(Long ownerId, String query) {
        return retrieve(ownerId, query, 5);
    }

    /** Format a float[] vector as a pgvector string "[x1,x2,...,xn]". */
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
