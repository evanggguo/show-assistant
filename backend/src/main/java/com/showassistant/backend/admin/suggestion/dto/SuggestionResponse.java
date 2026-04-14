package com.showassistant.backend.admin.suggestion.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SuggestionResponse {

    private Long id;
    private String text;
    private Integer sortOrder;
    private Boolean enabled;
}
