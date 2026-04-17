package com.dossier.backend.knowledge;

import com.dossier.backend.ai.EmbeddingService;
import com.dossier.backend.knowledge.dto.KnowledgeEntryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RagService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RagService 单元测试")
class RagServiceTest {

    @Mock
    private KnowledgeRepository knowledgeRepository;

    @Mock
    private EmbeddingService embeddingService;

    private RagService ragService;

    @BeforeEach
    void setUp() {
        ragService = new RagService(knowledgeRepository, embeddingService);
    }

    @Test
    @DisplayName("embedding 不可用时返回空列表")
    void should_return_empty_list_when_embedding_unavailable() {
        when(embeddingService.embed(anyString())).thenReturn(new float[0]);

        List<KnowledgeEntryDto> result = ragService.retrieve(1L, "关于项目的问题");

        assertThat(result).isEmpty();
        verify(knowledgeRepository, never()).findSimilarByOwner(any(), any(), anyInt());
    }

    @Test
    @DisplayName("embedding 可用时调用向量检索")
    void should_call_repository_when_embedding_available() {
        float[] mockEmbedding = new float[768];
        when(embeddingService.embed(anyString())).thenReturn(mockEmbedding);
        when(knowledgeRepository.findSimilarByOwner(any(), any(), anyInt()))
            .thenReturn(Collections.emptyList());

        List<KnowledgeEntryDto> result = ragService.retrieve(1L, "关于项目的问题", 5);

        assertThat(result).isEmpty();
        verify(knowledgeRepository).findSimilarByOwner(eq(1L), any(), eq(5));
    }

    @Test
    @DisplayName("retrieve 便捷重载默认 topK=5")
    void should_use_default_topK_5() {
        float[] mockEmbedding = new float[768];
        when(embeddingService.embed(anyString())).thenReturn(mockEmbedding);
        when(knowledgeRepository.findSimilarByOwner(any(), any(), anyInt()))
            .thenReturn(Collections.emptyList());

        ragService.retrieve(1L, "查询");

        verify(knowledgeRepository).findSimilarByOwner(eq(1L), any(), eq(5));
    }
}
