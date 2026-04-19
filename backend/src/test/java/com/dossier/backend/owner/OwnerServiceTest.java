package com.dossier.backend.owner;

import com.dossier.backend.common.exception.ResourceNotFoundException;
import com.dossier.backend.owner.dto.OwnerProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OwnerService.
 * Covers owner profile retrieval and initial suggestions list scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OwnerService Unit Tests")
class OwnerServiceTest {

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private PromptSuggestionRepository promptSuggestionRepository;

    @InjectMocks
    private OwnerService ownerService;

    private Owner testOwner;

    @BeforeEach
    void setUp() {
        testOwner = Owner.builder()
            .id(1L)
            .name("Test Owner")
            .tagline("Full-stack Developer, passionate about tech")
            .avatarUrl("https://example.com/avatar.jpg")
            .build();
    }

    // ===== getOwnerProfile =====

    @Test
    @DisplayName("getOwnerProfile: maps to OwnerProfileResponse correctly when owner exists")
    void should_return_owner_profile_when_owner_exists() {
        // given
        when(ownerRepository.findById(1L)).thenReturn(Optional.of(testOwner));

        // when
        OwnerProfileResponse response = ownerService.getOwnerProfile();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Test Owner");
        assertThat(response.getTagline()).isEqualTo("Full-stack Developer, passionate about tech");
        assertThat(response.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
    }

    @Test
    @DisplayName("getOwnerProfile: throws ResourceNotFoundException when owner is not found")
    void should_throw_ResourceNotFoundException_when_owner_not_found() {
        // given
        when(ownerRepository.findById(1L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> ownerService.getOwnerProfile())
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getOwnerProfile(ownerId): returns correct profile for the specified owner")
    void should_return_profile_for_specific_owner_id() {
        // given
        Owner owner2 = Owner.builder().id(2L).name("Another Owner").build();
        when(ownerRepository.findById(2L)).thenReturn(Optional.of(owner2));

        // when
        OwnerProfileResponse response = ownerService.getOwnerProfile(2L);

        // then
        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getName()).isEqualTo("Another Owner");
    }

    @Test
    @DisplayName("getOwnerProfile(ownerId): throws ResourceNotFoundException when specified owner is not found")
    void should_throw_when_specific_owner_not_found() {
        // given
        when(ownerRepository.findById(99L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> ownerService.getOwnerProfile(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ===== getInitialSuggestions =====

    @Test
    @DisplayName("getInitialSuggestions: returns enabled suggestions sorted by sortOrder")
    void should_return_sorted_enabled_suggestions() {
        // given
        PromptSuggestion ps1 = PromptSuggestion.builder()
            .id(1L).text("Tell me about yourself").sortOrder(0).enabled(true).build();
        PromptSuggestion ps2 = PromptSuggestion.builder()
            .id(2L).text("What projects have you worked on recently?").sortOrder(1).enabled(true).build();
        PromptSuggestion ps3 = PromptSuggestion.builder()
            .id(3L).text("What are your strongest technical skills?").sortOrder(2).enabled(true).build();

        when(promptSuggestionRepository.findByOwnerIdAndEnabledTrueOrderBySortOrderAsc(1L))
            .thenReturn(List.of(ps1, ps2, ps3));

        // when
        List<String> suggestions = ownerService.getInitialSuggestions();

        // then
        assertThat(suggestions).containsExactly(
            "Tell me about yourself",
            "What projects have you worked on recently?",
            "What are your strongest technical skills?"
        );
    }

    @Test
    @DisplayName("getInitialSuggestions: returns empty list when no suggestions are available")
    void should_return_empty_list_when_no_suggestions() {
        // given
        when(promptSuggestionRepository.findByOwnerIdAndEnabledTrueOrderBySortOrderAsc(1L))
            .thenReturn(Collections.emptyList());

        // when
        List<String> suggestions = ownerService.getInitialSuggestions();

        // then
        assertThat(suggestions).isEmpty();
    }

    @Test
    @DisplayName("getInitialSuggestions(ownerId): queries by the specified ownerId")
    void should_query_suggestions_by_owner_id() {
        // given
        PromptSuggestion ps = PromptSuggestion.builder()
            .id(10L).text("About me").sortOrder(0).enabled(true).build();
        when(promptSuggestionRepository.findByOwnerIdAndEnabledTrueOrderBySortOrderAsc(2L))
            .thenReturn(List.of(ps));

        // when
        List<String> suggestions = ownerService.getInitialSuggestions(2L);

        // then
        assertThat(suggestions).containsExactly("About me");
    }
}
