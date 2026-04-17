package com.dossier.backend.admin.owner.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class UpdateOwnerRequest {

    private String name;
    private String tagline;
    private String avatarUrl;
    private Map<String, Object> contact;

    @Size(max = 2000)
    private String customPrompt;
}
