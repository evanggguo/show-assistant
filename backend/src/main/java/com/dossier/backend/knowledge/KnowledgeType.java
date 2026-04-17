package com.dossier.backend.knowledge;

/**
 * TDD 5.1 — 知识条目类型枚举
 * 定义知识库中条目的分类
 */
public enum KnowledgeType {
    /** 纯文本内容 */
    TEXT,
    /** FAQ 问答对 */
    FAQ,
    /** 从文档中提取的内容 */
    DOCUMENT_CHUNK,
    /** 结构化数据 */
    STRUCTURED
}
