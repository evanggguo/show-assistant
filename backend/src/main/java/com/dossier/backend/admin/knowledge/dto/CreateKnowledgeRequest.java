package com.dossier.backend.admin.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateKnowledgeRequest {

    @NotNull(message = "Type must not be null")
    private String type;

    private String title;

    @NotBlank(message = "Content must not be blank")
    private String content;
}
