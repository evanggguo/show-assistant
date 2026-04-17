package com.dossier.backend.common.exception;

import lombok.Getter;

/**
 * TDD 6.2 — 业务异常基类
 * 用于表示可预期的业务错误，携带错误码和用户友好消息
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
