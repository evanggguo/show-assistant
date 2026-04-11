package com.showassistant.backend.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * TDD 4.2 — Spring AI 向量嵌入服务实现
 * 通过 Spring AI 的 EmbeddingModel 将文本转换为向量。
 * 注意：EmbeddingModel 通过 Optional 注入，在 Anthropic-only 配置下可能为 null。
 * 异常时和模型不可用时安全降级，返回空数组，不影响主流程。
 */
@Slf4j
@Service
public class SpringAiEmbeddingService implements EmbeddingService {

    @Nullable
    private final EmbeddingModel embeddingModel;

    @Autowired
    public SpringAiEmbeddingService(@Nullable EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        if (embeddingModel == null) {
            log.warn("EmbeddingModel is not configured. RAG embedding will be unavailable (Phase 2 stub mode).");
        }
    }

    /**
     * TDD 4.2 — 调用 Spring AI EmbeddingModel 生成文本向量
     * 若 EmbeddingModel 未配置或调用失败（网络异常、模型不可用等），
     * 捕获异常并返回空 float[]，保证系统在向量服务不可用时仍能正常运行（降级为无 RAG 模式）。
     *
     * @param text 需要嵌入的文本
     * @return 向量数组，失败时返回 new float[0]
     */
    @Override
    public float[] embed(String text) {
        if (embeddingModel == null) {
            log.debug("EmbeddingModel not available, returning empty vector for text length={}", text.length());
            return new float[0];
        }
        try {
            float[] result = embeddingModel.embed(text);
            log.debug("Generated embedding for text length={}, vector dim={}", text.length(), result.length);
            return result;
        } catch (Exception e) {
            log.warn("Failed to generate embedding, returning empty vector. Error: {}", e.getMessage());
            return new float[0];
        }
    }
}
