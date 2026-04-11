package com.showassistant.backend.knowledge;

import com.showassistant.backend.knowledge.dto.KnowledgeEntryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * RagService 单元测试
 * Phase 2 stub 验证：retrieve() 始终返回空列表
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RagService 单元测试（Phase 2 stub）")
class RagServiceTest {

    private RagService ragService;

    @BeforeEach
    void setUp() {
        ragService = new RagService();
    }

    @Test
    @DisplayName("retrieve(ownerId, query)：Phase 2 stub 始终返回空列表，无论任何输入")
    void should_return_empty_list_for_any_input() {
        // when
        List<KnowledgeEntryDto> result = ragService.retrieve(1L, "关于项目的问题");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("retrieve(ownerId, query, topK)：Phase 2 stub 始终返回空列表")
    void should_return_empty_list_with_topK() {
        // when
        List<KnowledgeEntryDto> result = ragService.retrieve(1L, "任何查询", 10);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("retrieve：不同 ownerId 均返回空列表")
    void should_return_empty_list_for_different_owner_ids() {
        assertThat(ragService.retrieve(1L, "问题")).isEmpty();
        assertThat(ragService.retrieve(2L, "问题")).isEmpty();
        assertThat(ragService.retrieve(100L, "问题")).isEmpty();
    }

    @Test
    @DisplayName("retrieve：查询字符串为空时仍返回空列表")
    void should_return_empty_list_when_query_is_empty() {
        // when
        List<KnowledgeEntryDto> result = ragService.retrieve(1L, "");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("retrieve：topK 为 0 时仍返回空列表")
    void should_return_empty_list_when_topK_is_zero() {
        // when
        List<KnowledgeEntryDto> result = ragService.retrieve(1L, "查询", 0);

        // then
        assertThat(result).isEmpty();
    }
}
