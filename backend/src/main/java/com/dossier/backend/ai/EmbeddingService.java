package com.dossier.backend.ai;

/**
 * TDD 4.2 — 向量嵌入服务接口
 * 定义文本转向量的标准接口，Phase 3 RAG 功能依赖此接口
 */
public interface EmbeddingService {

    /**
     * TDD 4.2 — 将文本转换为向量嵌入
     * 失败时返回空数组，不抛出异常，保持系统可用性
     *
     * @param text 需要嵌入的文本
     * @return 向量数组（float[]），失败时返回 new float[0]
     */
    float[] embed(String text);
}
