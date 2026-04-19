package com.dossier.backend.chat.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for SuggestFollowupsTool.
 * Covers tool invocation, captured list retrieval, and edge case handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SuggestFollowupsTool Unit Tests")
class SuggestFollowupsToolTest {

    private SuggestFollowupsTool suggestFollowupsTool;

    @BeforeEach
    void setUp() {
        suggestFollowupsTool = new SuggestFollowupsTool();
    }

    @Test
    @DisplayName("After calling suggestFollowups(List), getCapturedSuggestions() returns the same list content")
    void should_capture_suggestions_when_called_with_list() {
        // given
        List<String> suggestions = List.of("Question 1", "Question 2", "Question 3");

        // when
        String result = suggestFollowupsTool.suggestFollowups(suggestions);
        List<String> captured = suggestFollowupsTool.getCapturedSuggestions();

        // then
        assertThat(captured).containsExactly("Question 1", "Question 2", "Question 3");
        assertThat(result).isEqualTo("");
    }

    @Test
    @DisplayName("When argument is null, getCapturedSuggestions() returns an empty list")
    void should_return_empty_list_when_null_suggestions() {
        // when
        suggestFollowupsTool.suggestFollowups(null);
        List<String> captured = suggestFollowupsTool.getCapturedSuggestions();

        // then
        assertThat(captured).isEmpty();
    }

    @Test
    @DisplayName("When argument is an empty list, getCapturedSuggestions() returns an empty list")
    void should_return_empty_list_when_empty_suggestions() {
        // when
        suggestFollowupsTool.suggestFollowups(Collections.emptyList());
        List<String> captured = suggestFollowupsTool.getCapturedSuggestions();

        // then
        assertThat(captured).isEmpty();
    }

    @Test
    @DisplayName("Return value is an empty string (does not affect conversation text)")
    void should_return_empty_string() {
        // when
        String result = suggestFollowupsTool.suggestFollowups(List.of("Question A"));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("When suggestFollowups has never been called, getCapturedSuggestions() returns an empty list")
    void should_return_empty_list_when_never_called() {
        // when
        List<String> captured = suggestFollowupsTool.getCapturedSuggestions();

        // then
        assertThat(captured).isEmpty();
    }

    @Test
    @DisplayName("getCapturedSuggestions() returns an unmodifiable list")
    void should_return_unmodifiable_list() {
        // given
        suggestFollowupsTool.suggestFollowups(List.of("Question 1"));

        // when
        List<String> captured = suggestFollowupsTool.getCapturedSuggestions();

        // then
        assertThatThrownBy(() -> captured.add("extra question"))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
