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
 * Unit tests for SpringAiEmbeddingService.
 * Covers null embeddingModel, normal invocation, and exception fallback scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpringAiEmbeddingService Unit Tests")
class SpringAiEmbeddingServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Test
    @DisplayName("embed: returns new float[0] (fallback) when embeddingModel is null")
    void should_return_empty_vector_when_embedding_model_is_null() {
        // given
        SpringAiEmbeddingService service = new SpringAiEmbeddingService(null);

        // when
        float[] result = service.embed("test text");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("embed: returns the vector when embeddingModel works normally")
    void should_return_vector_when_embedding_model_works() {
        // given
        float[] expectedVector = {0.1f, 0.2f, 0.3f, 0.4f};
        when(embeddingModel.embed("test text")).thenReturn(expectedVector);
        SpringAiEmbeddingService service = new SpringAiEmbeddingService(embeddingModel);

        // when
        float[] result = service.embed("test text");

        // then
        assertThat(result).containsExactly(0.1f, 0.2f, 0.3f, 0.4f);
        verify(embeddingModel).embed("test text");
    }

    @Test
    @DisplayName("embed: returns new float[0] (fallback) when embeddingModel.embed() throws an exception")
    void should_return_empty_vector_when_embedding_throws_exception() {
        // given
        when(embeddingModel.embed(anyString())).thenThrow(new RuntimeException("network timeout"));
        SpringAiEmbeddingService service = new SpringAiEmbeddingService(embeddingModel);

        // when
        float[] result = service.embed("test text");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("embed: returns a high-dimensional vector correctly")
    void should_return_high_dimensional_vector() {
        // given
        float[] highDimVector = new float[1536]; // OpenAI embedding dimension
        for (int i = 0; i < highDimVector.length; i++) {
            highDimVector[i] = (float) i / 1536;
        }
        when(embeddingModel.embed("long text")).thenReturn(highDimVector);
        SpringAiEmbeddingService service = new SpringAiEmbeddingService(embeddingModel);

        // when
        float[] result = service.embed("long text");

        // then
        assertThat(result).hasSize(1536);
    }
}
