package com.showassistant.backend.knowledge;

import com.showassistant.backend.common.exception.ResourceNotFoundException;
import com.showassistant.backend.knowledge.dto.KnowledgeEntryDto;
import com.showassistant.backend.owner.Owner;
import com.showassistant.backend.owner.OwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * TDD 4.2 — 知识库管理服务
 * 负责知识条目的 CRUD，Phase 3 将集成向量嵌入生成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeRepository knowledgeRepository;
    private final OwnerRepository ownerRepository;

    /**
     * TDD 4.2 — 查询指定 Owner 的所有知识条目
     */
    @Transactional(readOnly = true)
    public List<KnowledgeEntryDto> listByOwner(Long ownerId) {
        return knowledgeRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId)
            .stream()
            .map(this::mapToDto)
            .toList();
    }

    /**
     * TDD 4.2 — 创建新知识条目
     * Phase 3 将在此处调用 EmbeddingService 生成向量
     */
    @Transactional
    public KnowledgeEntryDto create(Long ownerId, String type, String title, String content) {
        Owner owner = ownerRepository.findById(ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Owner", ownerId));

        KnowledgeEntry entry = KnowledgeEntry.builder()
            .owner(owner)
            .type(KnowledgeType.valueOf(type.toUpperCase()))
            .title(title)
            .content(content)
            .build();

        KnowledgeEntry saved = knowledgeRepository.save(entry);
        log.debug("Created knowledge entry id={} for ownerId={}", saved.getId(), ownerId);
        return mapToDto(saved);
    }

    /**
     * 将 KnowledgeEntry 实体映射为 DTO
     */
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
