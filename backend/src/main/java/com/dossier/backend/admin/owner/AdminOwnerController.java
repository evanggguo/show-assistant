package com.dossier.backend.admin.owner;

import com.dossier.backend.admin.owner.dto.ChangePasswordRequest;
import com.dossier.backend.admin.owner.dto.ChangeUsernameRequest;
import com.dossier.backend.admin.owner.dto.UpdateOwnerRequest;
import com.dossier.backend.common.response.ApiResponse;
import com.dossier.backend.owner.dto.OwnerProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Admin owner profile API. */
@RestController
@RequestMapping("/api/admin/owner")
@RequiredArgsConstructor
public class AdminOwnerController {

    private final AdminOwnerService adminOwnerService;

    @GetMapping("/profile")
    public ApiResponse<OwnerProfileResponse> getProfile() {
        return ApiResponse.ok(adminOwnerService.getOwnerProfile());
    }

    @PutMapping("/profile")
    public ApiResponse<OwnerProfileResponse> updateProfile(@RequestBody UpdateOwnerRequest request) {
        return ApiResponse.ok(adminOwnerService.updateOwnerProfile(request));
    }

    @PutMapping("/username")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeUsername(@Valid @RequestBody ChangeUsernameRequest request) {
        adminOwnerService.changeUsername(request);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        adminOwnerService.changePassword(request);
    }
}
