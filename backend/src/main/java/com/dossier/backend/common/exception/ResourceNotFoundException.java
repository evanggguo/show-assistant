package com.dossier.backend.common.exception;

/**
 * TDD 6.2 — Resource not found exception (HTTP 404).
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
