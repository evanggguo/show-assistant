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
 * SuggestFollowupsTool 单元测试
 * 覆盖工具调用、捕获列表获取和边界条件处理
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SuggestFollowupsTool 单元测试")
class SuggestFollowupsToolTest {

    private SuggestFollowupsTool suggestFollowupsTool;

    @BeforeEach
    void setUp() {
        suggestFollowupsTool = new SuggestFollowupsTool();
    }

    @Test
    @DisplayName("调用 suggestFollowups(List) 后，getCapturedSuggestions() 返回同一列表内容")
    void should_capture_suggestions_when_called_with_list() {
        // given
        List<String> suggestions = List.of("问题1", "问题2", "问题3");

        // when
        String result = suggestFollowupsTool.suggestFollowups(suggestions);
        List<String> captured = suggestFollowupsTool.getCapturedSuggestions();

        // then
        assertThat(captured).containsExactly("问题1", "问题2", "问题3");
        assertThat(result).isEqualTo("");
    }

    @Test
    @DisplayName("参数为 null 时，getCapturedSuggestions() 返回空列表")
    void should_return_empty_list_when_null_suggestions() {
        // when
        suggestFollowupsTool.suggestFollowups(null);
        List<String> captured = suggestFollowupsTool.getCapturedSuggestions();

        // then
        assertThat(captured).isEmpty();
    }

    @Test
    @DisplayName("参数为空列表时，getCapturedSuggestions() 返回空列表")
    void should_return_empty_list_when_empty_suggestions() {
        // when
        suggestFollowupsTool.suggestFollowups(Collections.emptyList());
        List<String> captured = suggestFollowupsTool.getCapturedSuggestions();

        // then
        assertThat(captured).isEmpty();
    }

    @Test
    @DisplayName("返回值为空字符串（不影响对话文本）")
    void should_return_empty_string() {
        // when
        String result = suggestFollowupsTool.suggestFollowups(List.of("问题A"));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("未调用 suggestFollowups 时，getCapturedSuggestions() 返回空列表")
    void should_return_empty_list_when_never_called() {
        // when
        List<String> captured = suggestFollowupsTool.getCapturedSuggestions();

        // then
        assertThat(captured).isEmpty();
    }

    @Test
    @DisplayName("getCapturedSuggestions() 返回不可修改列表")
    void should_return_unmodifiable_list() {
        // given
        suggestFollowupsTool.suggestFollowups(List.of("问题1"));

        // when
        List<String> captured = suggestFollowupsTool.getCapturedSuggestions();

        // then
        assertThatThrownBy(() -> captured.add("额外问题"))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
