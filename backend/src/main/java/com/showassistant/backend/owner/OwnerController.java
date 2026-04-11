package com.showassistant.backend.owner;

import com.showassistant.backend.common.response.ApiResponse;
import com.showassistant.backend.owner.dto.OwnerProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * TDD 6.4 — Owner API 控制器
 * 提供 Owner 简介和初始提示词的查询接口
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;

    /**
     * TDD 6.4.1 — GET /api/owner/profile
     * 获取展示者的公开简介信息（name、tagline、avatarUrl、contact）
     */
    @GetMapping("/owner/profile")
    public ApiResponse<OwnerProfileResponse> getOwnerProfile() {
        return ApiResponse.ok(ownerService.getOwnerProfile());
    }

    /**
     * TDD 6.4.2 — GET /api/suggestions/initial
     * 获取 Owner 预设的初始引导提示词列表，供访客选择作为对话起点
     */
    @GetMapping("/suggestions/initial")
    public ApiResponse<List<String>> getInitialSuggestions() {
        return ApiResponse.ok(ownerService.getInitialSuggestions());
    }
}
