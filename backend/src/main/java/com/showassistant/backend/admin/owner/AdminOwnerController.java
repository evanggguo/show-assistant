package com.showassistant.backend.admin.owner;

import com.showassistant.backend.admin.owner.dto.UpdateOwnerRequest;
import com.showassistant.backend.common.response.ApiResponse;
import com.showassistant.backend.owner.dto.OwnerProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端 Owner 信息接口
 */
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
}
