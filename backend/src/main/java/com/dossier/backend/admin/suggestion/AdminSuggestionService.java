package com.dossier.backend.admin.suggestion;

import com.dossier.backend.admin.suggestion.dto.CreateSuggestionRequest;
import com.dossier.backend.admin.suggestion.dto.SuggestionResponse;
import com.dossier.backend.admin.suggestion.dto.UpdateSuggestionRequest;
import com.dossier.backend.common.exception.ResourceNotFoundException;
import com.dossier.backend.owner.Owner;
import com.dossier.backend.owner.OwnerContextHolder;
import com.dossier.backend.owner.PromptSuggestion;
import com.dossier.backend.owner.PromptSuggestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Admin suggestion CRUD service. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSuggestionService {

    private final PromptSuggestionRepository suggestionRepository;
    private final OwnerContextHolder ownerContextHolder;

    @Transactional(readOnly = true)
    public List<SuggestionResponse> listAll() {
        return suggestionRepository.findByOwnerIdOrderBySortOrderAsc(ownerContextHolder.getCurrentOwnerId())
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional
    public SuggestionResponse create(CreateSuggestionRequest request) {
        Owner owner = ownerContextHolder.getCurrentOwner();

        PromptSuggestion suggestion = PromptSuggestion.builder()
            .owner(owner)
            .text(request.getText())
            .sortOrder(request.getSortOrder())
            .enabled(request.getEnabled() != null ? request.getEnabled() : true)
            .build();

        return mapToResponse(suggestionRepository.save(suggestion));
    }

    @Transactional
    public SuggestionResponse update(Long id, UpdateSuggestionRequest request) {
        PromptSuggestion suggestion = suggestionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("PromptSuggestion", id));

        if (request.getText() != null) suggestion.setText(request.getText());
        if (request.getSortOrder() != null) suggestion.setSortOrder(request.getSortOrder());
        if (request.getEnabled() != null) suggestion.setEnabled(request.getEnabled());

        return mapToResponse(suggestionRepository.save(suggestion));
    }

    @Transactional
    public void delete(Long id) {
        if (!suggestionRepository.existsById(id)) {
            throw new ResourceNotFoundException("PromptSuggestion", id);
        }
        suggestionRepository.deleteById(id);
        log.debug("Deleted suggestion id={}", id);
    }

    private SuggestionResponse mapToResponse(PromptSuggestion s) {
        return SuggestionResponse.builder()
            .id(s.getId())
            .text(s.getText())
            .sortOrder(s.getSortOrder())
            .enabled(s.getEnabled())
            .build();
    }
}
