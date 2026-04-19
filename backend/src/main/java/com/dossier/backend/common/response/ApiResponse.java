package com.dossier.backend.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TDD 6.1 — Unified API response wrapper for all non-SSE endpoints.
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

    /** TDD 6.1.1 — Success response factory. */
    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .build();
    }

    /** TDD 6.1.2 — Error response factory. */
    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .code(code)
            .message(message)
            .build();
    }
}
