package com.showassistant.backend.admin.document.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class DocumentResponse {

    private Long id;
    private String filename;
    private String fileType;
    private Long fileSize;
    private String status;
    private OffsetDateTime createdAt;
}
