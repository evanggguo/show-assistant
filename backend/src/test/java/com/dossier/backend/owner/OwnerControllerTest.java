package com.dossier.backend.owner;

import com.dossier.backend.common.response.ApiResponse;
import com.dossier.backend.owner.dto.OwnerProfileResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OwnerController.
 * Verifies controller handling of owner profile and initial suggestions.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OwnerController Unit Tests")
class OwnerControllerTest {

    @Mock
    private OwnerService ownerService;

    @InjectMocks
    private OwnerController ownerController;

    @Test
    @DisplayName("GET /api/owner/profile: returns OwnerProfileResponse correctly")
    void should_return_owner_profile() {
        // given
        OwnerProfileResponse profile = OwnerProfileResponse.builder()
            .id(1L)
            .name("Test Owner")
            .tagline("Full-stack Developer")
            .avatarUrl("https://example.com/avatar.jpg")
            .build();
        when(ownerService.getOwnerProfile()).thenReturn(profile);

        // when
        ApiResponse<OwnerProfileResponse> result = ownerController.getOwnerProfile();

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getName()).isEqualTo("Test Owner");
        assertThat(result.getData().getTagline()).isEqualTo("Full-stack Developer");
    }

    @Test
    @DisplayName("GET /api/suggestions/initial: returns the initial suggestions list")
    void should_return_initial_suggestions() {
        // given
        List<String> suggestions = List.of("Tell me about yourself", "What projects have you worked on recently?");
        when(ownerService.getInitialSuggestions()).thenReturn(suggestions);

        // when
        ApiResponse<List<String>> result = ownerController.getInitialSuggestions();

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsExactly("Tell me about yourself", "What projects have you worked on recently?");
    }

    @Test
    @DisplayName("GET /api/suggestions/initial: returns empty list when no suggestions are available")
    void should_return_empty_list_when_no_suggestions() {
        // given
        when(ownerService.getInitialSuggestions()).thenReturn(Collections.emptyList());

        // when
        ApiResponse<List<String>> result = ownerController.getInitialSuggestions();

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }
}
