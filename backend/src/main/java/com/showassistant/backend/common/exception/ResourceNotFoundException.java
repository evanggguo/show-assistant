package com.showassistant.backend.common.exception;

/**
 * TDD 6.2 — 资源未找到异常
 * 用于表示请求的资源不存在（对应 HTTP 404）
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceType, Long id) {
        super("RESOURCE_NOT_FOUND", resourceType + " not found with id: " + id);
    }

    public ResourceNotFoundException(String resourceType, String key) {
        super("RESOURCE_NOT_FOUND", resourceType + " not found: " + key);
    }

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }
}
