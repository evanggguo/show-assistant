package com.showassistant.backend.admin.suggestion.dto;

import lombok.Data;

@Data
public class UpdateSuggestionRequest {

    private String text;
    private Integer sortOrder;
    private Boolean enabled;
}
