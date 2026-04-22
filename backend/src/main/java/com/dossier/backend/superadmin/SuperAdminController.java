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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Super-admin API — owner account management.
 * Two-tier access via X-Super-Admin-Token header; no JWT required.
 * Add-only token: list and create. Full-access token: list, create, and delete.
 */
@RestController
@RequestMapping("/api/super-admin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    @Value("${app.super-admin.password}")
    private String superAdminPassword;

    @Value("${app.super-admin.full-access-password}")
    private String superAdminFullAccessPassword;

    /**
     * Returns capabilities for the provided token.
     * The frontend calls this on login to determine which UI actions are available.
     * Returns 403 if the token is invalid.
     */
    @GetMapping("/capabilities")
    public ApiResponse<Map<String, Boolean>> getCapabilities(
        @RequestHeader("X-Super-Admin-Token") String token) {
        verifyReadWriteAccess(token);
        Map<String, Boolean> caps = new HashMap<>();
        caps.put("canDelete", superAdminFullAccessPassword.equals(token));
        return ApiResponse.ok(caps);
    }

    @GetMapping("/owners")
    public ApiResponse<List<OwnerSummaryResponse>> listOwners(
        @RequestHeader("X-Super-Admin-Token") String token) {
        verifyReadWriteAccess(token);
        return ApiResponse.ok(superAdminService.listOwners());
    }

    @PostMapping("/owners")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OwnerSummaryResponse> createOwner(
        @RequestHeader("X-Super-Admin-Token") String token,
        @Valid @RequestBody CreateOwnerRequest request) {
        verifyReadWriteAccess(token);
        return ApiResponse.ok(superAdminService.createOwner(request));
    }

    @DeleteMapping("/owners/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOwner(
        @RequestHeader("X-Super-Admin-Token") String token,
        @PathVariable Long id) {
        verifyFullAccess(token);
        superAdminService.deleteOwner(id);
    }

    /** Accepts either token — grants list/create access. */
    private void verifyReadWriteAccess(String token) {
        if (!superAdminPassword.equals(token) && !superAdminFullAccessPassword.equals(token)) {
            throw new BusinessException("FORBIDDEN", "Invalid super-admin token");
        }
    }

    /** Accepts only the full-access token — required for delete. */
    private void verifyFullAccess(String token) {
        if (!superAdminFullAccessPassword.equals(token)) {
            throw new BusinessException("FORBIDDEN", "This operation requires full-access credentials");
        }
    }
}
