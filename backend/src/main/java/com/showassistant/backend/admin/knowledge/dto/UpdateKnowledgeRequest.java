package com.showassistant.backend.admin.knowledge.dto;

import lombok.Data;

@Data
public class UpdateKnowledgeRequest {

    private String title;
    private String content;
}
