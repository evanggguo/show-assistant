package com.dossier.backend.superadmin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class OwnerSummaryResponse {
    private Long id;
    private String username;
    private String name;
    private OffsetDateTime createdAt;
}
