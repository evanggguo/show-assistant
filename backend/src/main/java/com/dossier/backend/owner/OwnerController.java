package com.dossier.backend.owner;

import com.dossier.backend.common.response.ApiResponse;
import com.dossier.backend.owner.dto.OwnerProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * TDD 6.4 — Owner API controller
 * Provides endpoints to query owner profile and initial prompt suggestions
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;

    /**
     * TDD 6.4.1 — GET /api/owner/profile
     * Get the public profile of the owner (name, tagline, avatarUrl, contact)
     */
    @GetMapping("/owner/profile")
    public ApiResponse<OwnerProfileResponse> getOwnerProfile() {
        return ApiResponse.ok(ownerService.getOwnerProfile());
    }

    /**
     * TDD 6.4.2 — GET /api/suggestions/initial
     * Get the owner's preset initial prompt suggestions for visitors to select as conversation starters
     */
    @GetMapping("/suggestions/initial")
    public ApiResponse<List<String>> getInitialSuggestions() {
        return ApiResponse.ok(ownerService.getInitialSuggestions());
    }
}
