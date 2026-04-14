package com.showassistant.backend.admin.suggestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSuggestionRequest {

    @NotBlank(message = "提示词内容不能为空")
    private String text;

    @NotNull(message = "排序值不能为空")
    private Integer sortOrder;

    private Boolean enabled = true;
}
