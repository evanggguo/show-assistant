package com.dossier.backend.knowledge;

import com.dossier.backend.ai.EmbeddingService;
import com.dossier.backend.common.exception.ResourceNotFoundException;
import com.dossier.backend.knowledge.dto.KnowledgeEntryDto;
import com.dossier.backend.owner.Owner;
import com.dossier.backend.owner.OwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Knowledge base management service.
 * Handles knowledge entry CRUD; Phase 3 integrates vector embedding generation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeRepository knowledgeRepository;
    private final OwnerRepository ownerRepository;
    private final EmbeddingService embeddingService;

    @Transactional(readOnly = true)
    public List<KnowledgeEntryDto> listByOwner(Long ownerId) {
        return knowledgeRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId)
            .stream()
            .map(this::mapToDto)
            .toList();
    }

    @Transactional
    public KnowledgeEntryDto create(Long ownerId, String type, String title, String content) {
        Owner owner = ownerRepository.findById(ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Owner", ownerId));

        float[] embedding = embeddingService.embed(content);

        KnowledgeEntry entry = KnowledgeEntry.builder()
            .owner(owner)
            .type(KnowledgeType.valueOf(type.toUpperCase()))
            .title(title)
            .content(content)
            .embedding(embedding.length > 0 ? embedding : null)
            .build();

        KnowledgeEntry saved = knowledgeRepository.save(entry);
        log.debug("Created knowledge entry id={} for ownerId={}, hasEmbedding={}",
            saved.getId(), ownerId, embedding.length > 0);
        return mapToDto(saved);
    }

    @Transactional
    public KnowledgeEntryDto update(Long id, String title, String content) {
        KnowledgeEntry entry = knowledgeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("KnowledgeEntry", id));

        if (title != null) entry.setTitle(title);
        if (content != null && !content.equals(entry.getContent())) {
            entry.setContent(content);
            // Re-generate embedding when content changes
            float[] embedding = embeddingService.embed(content);
            entry.setEmbedding(embedding.length > 0 ? embedding : null);
        }

        return mapToDto(knowledgeRepository.save(entry));
    }

    @Transactional
    public void delete(Long id) {
        if (!knowledgeRepository.existsById(id)) {
            throw new ResourceNotFoundException("KnowledgeEntry", id);
        }
        knowledgeRepository.deleteById(id);
        log.debug("Deleted knowledge entry id={}", id);
    }

    @Transactional(readOnly = true)
    public KnowledgeEntryDto findById(Long id) {
        return knowledgeRepository.findById(id)
            .map(this::mapToDto)
            .orElseThrow(() -> new ResourceNotFoundException("KnowledgeEntry", id));
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
