package com.dossier.backend.admin.knowledge.dto;

import lombok.Data;

@Data
public class UpdateKnowledgeRequest {

    private String title;
    private String content;
}
