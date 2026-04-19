package com.dossier.backend.admin.suggestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSuggestionRequest {

    @NotBlank(message = "Suggestion text must not be blank")
    private String text;

    @NotNull(message = "Sort order must not be null")
    private Integer sortOrder;

    private Boolean enabled = true;
}
