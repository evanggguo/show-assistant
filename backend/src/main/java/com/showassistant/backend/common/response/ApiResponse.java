package com.showassistant.backend.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TDD 6.1 — 统一 API 响应包装
 * 所有非 SSE 接口的统一返回格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String message;
    private String code;

    /**
     * TDD 6.1.1 — 成功响应工厂方法
     */
    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .build();
    }

    /**
     * TDD 6.1.2 — 错误响应工厂方法
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .code(code)
            .message(message)
            .build();
    }
}
