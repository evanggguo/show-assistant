package com.showassistant.backend.admin.knowledge;

import com.showassistant.backend.admin.knowledge.dto.CreateKnowledgeRequest;
import com.showassistant.backend.admin.knowledge.dto.UpdateKnowledgeRequest;
import com.showassistant.backend.knowledge.KnowledgeService;
import com.showassistant.backend.knowledge.dto.KnowledgeEntryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理端知识库服务 — 委托 KnowledgeService，DEFAULT_OWNER_ID=1L
 */
@Service
@RequiredArgsConstructor
public class AdminKnowledgeService {

    private static final Long DEFAULT_OWNER_ID = 1L;

    private final KnowledgeService knowledgeService;

    public List<KnowledgeEntryDto> listAll() {
        return knowledgeService.listByOwner(DEFAULT_OWNER_ID);
    }

    public KnowledgeEntryDto create(CreateKnowledgeRequest request) {
        return knowledgeService.create(
            DEFAULT_OWNER_ID,
            request.getType(),
            request.getTitle(),
            request.getContent()
        );
    }

    public KnowledgeEntryDto update(Long id, UpdateKnowledgeRequest request) {
        return knowledgeService.update(id, request.getTitle(), request.getContent());
    }

    public void delete(Long id) {
        knowledgeService.delete(id);
    }
}
