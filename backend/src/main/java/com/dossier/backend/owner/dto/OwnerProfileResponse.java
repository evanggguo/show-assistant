package com.dossier.backend.owner.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * TDD 6.4.1 — Owner profile response DTO
 * Owner information exposed to clients; does not include sensitive configuration data
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
