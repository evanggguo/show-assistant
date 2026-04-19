package com.dossier.backend.common.exception;

import lombok.Getter;

/**
 * TDD 6.2 — Business exception base class
 * Represents expected business errors, carrying an error code and a user-friendly message.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
