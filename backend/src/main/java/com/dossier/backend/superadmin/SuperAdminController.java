package com.dossier.backend.superadmin;

import com.dossier.backend.common.exception.BusinessException;
import com.dossier.backend.common.response.ApiResponse;
import com.dossier.backend.superadmin.dto.CreateOwnerRequest;
import com.dossier.backend.superadmin.dto.OwnerSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Super-admin API — owner account create/delete.
 * Validated via a fixed X-Super-Admin-Token request header; no JWT required.
 */
@RestController
@RequestMapping("/api/super-admin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    @Value("${app.super-admin.password}")
    private String superAdminPassword;

    @GetMapping("/owners")
    public ApiResponse<List<OwnerSummaryResponse>> listOwners(
        @RequestHeader("X-Super-Admin-Token") String token) {
        verifyToken(token);
        return ApiResponse.ok(superAdminService.listOwners());
    }

    @PostMapping("/owners")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OwnerSummaryResponse> createOwner(
        @RequestHeader("X-Super-Admin-Token") String token,
        @Valid @RequestBody CreateOwnerRequest request) {
        verifyToken(token);
        return ApiResponse.ok(superAdminService.createOwner(request));
    }

    @DeleteMapping("/owners/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOwner(
        @RequestHeader("X-Super-Admin-Token") String token,
        @PathVariable Long id) {
        verifyToken(token);
        superAdminService.deleteOwner(id);
    }

    private void verifyToken(String token) {
        if (!superAdminPassword.equals(token)) {
            throw new BusinessException("FORBIDDEN", "Invalid super-admin token");
        }
    }
}
