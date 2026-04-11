package com.showassistant.backend.owner;

import com.showassistant.backend.common.exception.ResourceNotFoundException;
import com.showassistant.backend.owner.dto.OwnerProfileResponse;
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
 * OwnerService 单元测试
 * 覆盖 Owner 简介查询和初始 suggestions 列表的获取场景
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OwnerService 单元测试")
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
            .name("测试主人")
            .tagline("全栈开发者，热爱技术")
            .avatarUrl("https://example.com/avatar.jpg")
            .build();
    }

    // ===== getOwnerProfile =====

    @Test
    @DisplayName("getOwnerProfile：owner 存在时正确映射为 OwnerProfileResponse")
    void should_return_owner_profile_when_owner_exists() {
        // given
        when(ownerRepository.findById(1L)).thenReturn(Optional.of(testOwner));

        // when
        OwnerProfileResponse response = ownerService.getOwnerProfile();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("测试主人");
        assertThat(response.getTagline()).isEqualTo("全栈开发者，热爱技术");
        assertThat(response.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
    }

    @Test
    @DisplayName("getOwnerProfile：owner 不存在时抛 ResourceNotFoundException")
    void should_throw_ResourceNotFoundException_when_owner_not_found() {
        // given
        when(ownerRepository.findById(1L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> ownerService.getOwnerProfile())
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getOwnerProfile(ownerId)：指定 owner 存在时正确返回")
    void should_return_profile_for_specific_owner_id() {
        // given
        Owner owner2 = Owner.builder().id(2L).name("另一位主人").build();
        when(ownerRepository.findById(2L)).thenReturn(Optional.of(owner2));

        // when
        OwnerProfileResponse response = ownerService.getOwnerProfile(2L);

        // then
        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getName()).isEqualTo("另一位主人");
    }

    @Test
    @DisplayName("getOwnerProfile(ownerId)：指定 owner 不存在时抛 ResourceNotFoundException")
    void should_throw_when_specific_owner_not_found() {
        // given
        when(ownerRepository.findById(99L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> ownerService.getOwnerProfile(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ===== getInitialSuggestions =====

    @Test
    @DisplayName("getInitialSuggestions：返回按 sortOrder 排序且 enabled=true 的 suggestions 列表")
    void should_return_sorted_enabled_suggestions() {
        // given
        PromptSuggestion ps1 = PromptSuggestion.builder()
            .id(1L).text("介绍一下你自己").sortOrder(0).enabled(true).build();
        PromptSuggestion ps2 = PromptSuggestion.builder()
            .id(2L).text("你最近做了什么项目").sortOrder(1).enabled(true).build();
        PromptSuggestion ps3 = PromptSuggestion.builder()
            .id(3L).text("你擅长哪些技术").sortOrder(2).enabled(true).build();

        when(promptSuggestionRepository.findByOwnerIdAndEnabledTrueOrderBySortOrderAsc(1L))
            .thenReturn(List.of(ps1, ps2, ps3));

        // when
        List<String> suggestions = ownerService.getInitialSuggestions();

        // then
        assertThat(suggestions).containsExactly(
            "介绍一下你自己",
            "你最近做了什么项目",
            "你擅长哪些技术"
        );
    }

    @Test
    @DisplayName("getInitialSuggestions：无可用 suggestions 时返回空列表")
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
    @DisplayName("getInitialSuggestions(ownerId)：按指定 ownerId 查询")
    void should_query_suggestions_by_owner_id() {
        // given
        PromptSuggestion ps = PromptSuggestion.builder()
            .id(10L).text("关于我").sortOrder(0).enabled(true).build();
        when(promptSuggestionRepository.findByOwnerIdAndEnabledTrueOrderBySortOrderAsc(2L))
            .thenReturn(List.of(ps));

        // when
        List<String> suggestions = ownerService.getInitialSuggestions(2L);

        // then
        assertThat(suggestions).containsExactly("关于我");
    }
}
