package com.dossier.backend.superadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateOwnerRequest {

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "用户名只能包含英文字母和数字")
    private String username;
}
