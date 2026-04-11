package com.showassistant.backend.owner;

import com.showassistant.backend.common.response.ApiResponse;
import com.showassistant.backend.owner.dto.OwnerProfileResponse;
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
 * OwnerController 单元测试
 * 验证控制器对 Owner 简介和初始 suggestions 的处理
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OwnerController 单元测试")
class OwnerControllerTest {

    @Mock
    private OwnerService ownerService;

    @InjectMocks
    private OwnerController ownerController;

    @Test
    @DisplayName("GET /api/owner/profile：正常返回 OwnerProfileResponse")
    void should_return_owner_profile() {
        // given
        OwnerProfileResponse profile = OwnerProfileResponse.builder()
            .id(1L)
            .name("测试主人")
            .tagline("全栈开发者")
            .avatarUrl("https://example.com/avatar.jpg")
            .build();
        when(ownerService.getOwnerProfile()).thenReturn(profile);

        // when
        ApiResponse<OwnerProfileResponse> result = ownerController.getOwnerProfile();

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getName()).isEqualTo("测试主人");
        assertThat(result.getData().getTagline()).isEqualTo("全栈开发者");
    }

    @Test
    @DisplayName("GET /api/suggestions/initial：返回初始提示词列表")
    void should_return_initial_suggestions() {
        // given
        List<String> suggestions = List.of("介绍一下你自己", "你最近做了什么项目");
        when(ownerService.getInitialSuggestions()).thenReturn(suggestions);

        // when
        ApiResponse<List<String>> result = ownerController.getInitialSuggestions();

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsExactly("介绍一下你自己", "你最近做了什么项目");
    }

    @Test
    @DisplayName("GET /api/suggestions/initial：无可用 suggestions 时返回空列表")
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
