package com.showassistant.backend.admin.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateKnowledgeRequest {

    @NotNull(message = "类型不能为空")
    private String type;

    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;
}
