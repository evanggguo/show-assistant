package com.dossier.backend.owner;

import com.dossier.backend.common.exception.ResourceNotFoundException;
import com.dossier.backend.owner.dto.OwnerProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * TDD 6.4 — Owner business service
 * Provides owner profile lookup and suggestion management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OwnerService {

    // Default to owner with ID=1 (MVP single-user mode)
    private static final Long DEFAULT_OWNER_ID = 1L;

    private final OwnerRepository ownerRepository;
    private final PromptSuggestionRepository promptSuggestionRepository;

    /** TDD 6.4.1 — Get the default owner profile (ID=1, MVP single-owner mode). */
    @Transactional(readOnly = true)
    public OwnerProfileResponse getOwnerProfile() {
        Owner owner = ownerRepository.findById(DEFAULT_OWNER_ID)
            .orElseThrow(() -> new ResourceNotFoundException("Owner", DEFAULT_OWNER_ID));
        return mapToProfileResponse(owner);
    }

    /** TDD 6.4.1 — Get the profile of the owner with the given ID. */
    @Transactional(readOnly = true)
    public OwnerProfileResponse getOwnerProfile(Long ownerId) {
        Owner owner = ownerRepository.findById(ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Owner", ownerId));
        return mapToProfileResponse(owner);
    }

    /** TDD 6.4.2 — Get the enabled initial suggestions for an owner, sorted by sort_order ascending. */
    @Transactional(readOnly = true)
    public List<String> getInitialSuggestions(Long ownerId) {
        return promptSuggestionRepository
            .findByOwnerIdAndEnabledTrueOrderBySortOrderAsc(ownerId)
            .stream()
            .map(PromptSuggestion::getText)
            .toList();
    }

    /** TDD 6.4.2 — Get the initial suggestions for the default owner. */
    @Transactional(readOnly = true)
    public List<String> getInitialSuggestions() {
        return getInitialSuggestions(DEFAULT_OWNER_ID);
    }

    /** TDD 6.4.1 — Get the default owner entity (internal use only). */
    @Transactional(readOnly = true)
    public Owner getDefaultOwner() {
        return ownerRepository.findById(DEFAULT_OWNER_ID)
            .orElseThrow(() -> new ResourceNotFoundException("Owner", DEFAULT_OWNER_ID));
    }

    /** Find an owner by username (used by the client routing layer). */
    @Transactional(readOnly = true)
    public Owner getOwnerByUsername(String username) {
        return ownerRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Owner", username));
    }

    /** Map an Owner entity to an OwnerProfileResponse DTO. */
    private OwnerProfileResponse mapToProfileResponse(Owner owner) {
        return OwnerProfileResponse.builder()
            .id(owner.getId())
            .name(owner.getName())
            .tagline(owner.getTagline())
            .avatarUrl(owner.getAvatarUrl())
            .contact(owner.getContact())
            .customPrompt(owner.getCustomPrompt())
            .build();
    }
}
