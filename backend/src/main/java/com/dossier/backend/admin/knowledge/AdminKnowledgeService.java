package com.dossier.backend.admin.knowledge;

import com.dossier.backend.admin.knowledge.dto.CreateKnowledgeRequest;
import com.dossier.backend.admin.knowledge.dto.UpdateKnowledgeRequest;
import com.dossier.backend.knowledge.KnowledgeService;
import com.dossier.backend.knowledge.dto.KnowledgeEntryDto;
import com.dossier.backend.owner.OwnerContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Admin knowledge service — delegates to KnowledgeService using the current authenticated owner. */
@Service
@RequiredArgsConstructor
public class AdminKnowledgeService {

    private final KnowledgeService knowledgeService;
    private final OwnerContextHolder ownerContextHolder;

    public List<KnowledgeEntryDto> listAll() {
        return knowledgeService.listByOwner(ownerContextHolder.getCurrentOwnerId());
    }

    public KnowledgeEntryDto create(CreateKnowledgeRequest request) {
        return knowledgeService.create(
            ownerContextHolder.getCurrentOwnerId(),
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
