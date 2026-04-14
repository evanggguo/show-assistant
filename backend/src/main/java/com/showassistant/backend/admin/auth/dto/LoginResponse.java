package com.showassistant.backend.admin.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private String token;
    private String tokenType;
}
