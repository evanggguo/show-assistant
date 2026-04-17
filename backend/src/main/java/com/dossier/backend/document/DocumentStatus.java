package com.dossier.backend.document;

/**
 * TDD 5.1 — 文档处理状态枚举
 * 跟踪上传文档的处理进度
 */
public enum DocumentStatus {
    /** 上传完成，待处理 */
    PENDING,
    /** 正在解析和分块 */
    PROCESSING,
    /** 处理完成，已生成知识条目 */
    COMPLETED,
    /** 处理失败 */
    FAILED
}
