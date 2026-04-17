package com.dossier.backend.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SpringAiEmbeddingService 单元测试
 * 覆盖 embeddingModel 为 null、正常调用和异常降级场景
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpringAiEmbeddingService 单元测试")
class SpringAiEmbeddingServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Test
    @DisplayName("embed：embeddingModel 为 null 时返回 new float[0]（降级）")
    void should_return_empty_vector_when_embedding_model_is_null() {
        // given
        SpringAiEmbeddingService service = new SpringAiEmbeddingService(null);

        // when
        float[] result = service.embed("测试文本");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("embed：embeddingModel 正常时返回向量")
    void should_return_vector_when_embedding_model_works() {
        // given
        float[] expectedVector = {0.1f, 0.2f, 0.3f, 0.4f};
        when(embeddingModel.embed("测试文本")).thenReturn(expectedVector);
        SpringAiEmbeddingService service = new SpringAiEmbeddingService(embeddingModel);

        // when
        float[] result = service.embed("测试文本");

        // then
        assertThat(result).containsExactly(0.1f, 0.2f, 0.3f, 0.4f);
        verify(embeddingModel).embed("测试文本");
    }

    @Test
    @DisplayName("embed：embeddingModel.embed() 抛异常时，返回 new float[0]（降级）")
    void should_return_empty_vector_when_embedding_throws_exception() {
        // given
        when(embeddingModel.embed(anyString())).thenThrow(new RuntimeException("网络超时"));
        SpringAiEmbeddingService service = new SpringAiEmbeddingService(embeddingModel);

        // when
        float[] result = service.embed("测试文本");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("embed：embeddingModel 正常返回高维向量")
    void should_return_high_dimensional_vector() {
        // given
        float[] highDimVector = new float[1536]; // OpenAI 嵌入维度
        for (int i = 0; i < highDimVector.length; i++) {
            highDimVector[i] = (float) i / 1536;
        }
        when(embeddingModel.embed("长文本")).thenReturn(highDimVector);
        SpringAiEmbeddingService service = new SpringAiEmbeddingService(embeddingModel);

        // when
        float[] result = service.embed("长文本");

        // then
        assertThat(result).hasSize(1536);
    }
}
