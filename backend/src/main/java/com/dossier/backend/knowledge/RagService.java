package com.dossier.backend.knowledge;

import com.dossier.backend.ai.EmbeddingService;
import com.dossier.backend.knowledge.dto.KnowledgeEntryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        List<KnowledgeEntry> vectorEntries =
            knowledgeRepository.findSimilarByOwner(ownerId, vectorStr, topK);

        // Keyword boost: extract key terms from the query and include matching entries
        // that vector search may miss due to semantic distance (e.g. formal document language)
        List<KnowledgeEntry> keywordEntries = extractKeywords(query).stream()
            .flatMap(kw -> knowledgeRepository.findByKeyword(ownerId, kw, 3).stream())
            .toList();

        // Merge: vector results first, then append any keyword-only hits (dedup by id)
        Map<Long, KnowledgeEntry> merged = new LinkedHashMap<>();
        vectorEntries.forEach(e -> merged.put(e.getId(), e));
        keywordEntries.forEach(e -> merged.putIfAbsent(e.getId(), e));
        List<KnowledgeEntry> entries = new ArrayList<>(merged.values());

        log.debug("RAG retrieved {} entries for ownerId={} (vector={}, keyword={}): {}",
            entries.size(), ownerId, vectorEntries.size(), keywordEntries.size(),
            entries.stream().map(KnowledgeEntry::getTitle).toList());
        return entries.stream().map(this::mapToDto).toList();
    }

    /** Convenience overload using the default topK=5. */
    public List<KnowledgeEntryDto> retrieve(Long ownerId, String query) {
        return retrieve(ownerId, query, 5);
    }

    private static final java.util.Set<String> CJK_STOP = java.util.Set.of(
        "你的", "我的", "他的", "是什", "什么", "么样", "如何", "怎么", "有没", "没有",
        "可以", "能不", "不能", "告诉", "知道", "请问", "问一", "一下"
    );

    /**
     * Extract searchable keywords from the query for hybrid keyword+vector search.
     * Uses 2-char CJK bigrams (filtered for stop words) and English words (3+ chars).
     */
    private List<String> extractKeywords(String query) {
        if (query == null || query.isBlank()) return Collections.emptyList();
        List<String> keywords = new ArrayList<>();
        // CJK: 2-char bigrams, skip stop words
        for (int i = 0; i <= query.length() - 2; i++) {
            String sub = query.substring(i, i + 2);
            if (!CJK_STOP.contains(sub)
                    && sub.chars().allMatch(c -> (c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3400 && c <= 0x4DBF))) {
                keywords.add(sub);
                if (keywords.size() >= 4) break;
            }
        }
        // English: words of 3+ chars
        for (String word : query.split("\\s+")) {
            String clean = word.replaceAll("[^a-zA-Z0-9]", "");
            if (clean.length() >= 3) {
                keywords.add(clean);
                if (keywords.size() >= 6) break;
            }
        }
        return keywords;
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
