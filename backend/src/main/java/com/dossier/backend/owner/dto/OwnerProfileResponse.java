package com.dossier.backend.owner.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * TDD 6.4.1 — Owner 简介响应 DTO
 * 对外暴露的 Owner 信息，不含敏感配置数据
 */
@Data
@Builder
public class OwnerProfileResponse {

    private Long id;
    private String name;
    private String tagline;
    private String avatarUrl;
    private Map<String, Object> contact;
    private String customPrompt;
}
