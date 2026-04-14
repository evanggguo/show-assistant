package com.showassistant.backend.admin.suggestion;

import com.showassistant.backend.admin.suggestion.dto.CreateSuggestionRequest;
import com.showassistant.backend.admin.suggestion.dto.SuggestionResponse;
import com.showassistant.backend.admin.suggestion.dto.UpdateSuggestionRequest;
import com.showassistant.backend.common.exception.ResourceNotFoundException;
import com.showassistant.backend.owner.Owner;
import com.showassistant.backend.owner.OwnerRepository;
import com.showassistant.backend.owner.PromptSuggestion;
import com.showassistant.backend.owner.PromptSuggestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 管理端提示词 CRUD 服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSuggestionService {

    private static final Long DEFAULT_OWNER_ID = 1L;

    private final PromptSuggestionRepository suggestionRepository;
    private final OwnerRepository ownerRepository;

    @Transactional(readOnly = true)
    public List<SuggestionResponse> listAll() {
        return suggestionRepository.findByOwnerIdOrderBySortOrderAsc(DEFAULT_OWNER_ID)
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional
    public SuggestionResponse create(CreateSuggestionRequest request) {
        Owner owner = ownerRepository.findById(DEFAULT_OWNER_ID)
            .orElseThrow(() -> new ResourceNotFoundException("Owner", DEFAULT_OWNER_ID));

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
