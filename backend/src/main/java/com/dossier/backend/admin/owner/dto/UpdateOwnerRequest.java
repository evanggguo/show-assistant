package com.dossier.backend.admin.owner.dto;

import lombok.Data;

import java.util.Map;

@Data
public class UpdateOwnerRequest {

    private String name;
    private String tagline;
    private String avatarUrl;
    private Map<String, Object> contact;
}
