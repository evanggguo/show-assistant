package com.dossier.backend.owner;

import com.dossier.backend.common.exception.ResourceNotFoundException;
import com.dossier.backend.owner.dto.OwnerProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * TDD 6.4 — Owner 业务服务
 * 提供 Owner 信息查询和提示词管理功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OwnerService {

    // 默认使用 ID=1 的 Owner（MVP 单用户模式）
    private static final Long DEFAULT_OWNER_ID = 1L;

    private final OwnerRepository ownerRepository;
    private final PromptSuggestionRepository promptSuggestionRepository;

    /**
     * TDD 6.4.1 — 获取默认 Owner 简介
     * MVP 阶段只有一个 Owner，直接查询 ID=1
     */
    @Transactional(readOnly = true)
    public OwnerProfileResponse getOwnerProfile() {
        Owner owner = ownerRepository.findById(DEFAULT_OWNER_ID)
            .orElseThrow(() -> new ResourceNotFoundException("Owner", DEFAULT_OWNER_ID));
        return mapToProfileResponse(owner);
    }

    /**
     * TDD 6.4.1 — 获取指定 ID 的 Owner 简介
     */
    @Transactional(readOnly = true)
    public OwnerProfileResponse getOwnerProfile(Long ownerId) {
        Owner owner = ownerRepository.findById(ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Owner", ownerId));
        return mapToProfileResponse(owner);
    }

    /**
     * TDD 6.4.2 — 获取 Owner 的初始提示词列表
     * 返回启用状态的提示词，按 sort_order 升序排列
     */
    @Transactional(readOnly = true)
    public List<String> getInitialSuggestions(Long ownerId) {
        return promptSuggestionRepository
            .findByOwnerIdAndEnabledTrueOrderBySortOrderAsc(ownerId)
            .stream()
            .map(PromptSuggestion::getText)
            .toList();
    }

    /**
     * TDD 6.4.2 — 获取默认 Owner 的初始提示词列表
     */
    @Transactional(readOnly = true)
    public List<String> getInitialSuggestions() {
        return getInitialSuggestions(DEFAULT_OWNER_ID);
    }

    /**
     * TDD 6.4.1 — 获取默认 Owner 实体（内部使用）
     */
    @Transactional(readOnly = true)
    public Owner getDefaultOwner() {
        return ownerRepository.findById(DEFAULT_OWNER_ID)
            .orElseThrow(() -> new ResourceNotFoundException("Owner", DEFAULT_OWNER_ID));
    }

    /**
     * 按 username 查找 Owner（客户端路由使用）
     */
    @Transactional(readOnly = true)
    public Owner getOwnerByUsername(String username) {
        return ownerRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Owner", username));
    }

    /**
     * 将 Owner 实体映射为 OwnerProfileResponse DTO
     */
    private OwnerProfileResponse mapToProfileResponse(Owner owner) {
        return OwnerProfileResponse.builder()
            .id(owner.getId())
            .name(owner.getName())
            .tagline(owner.getTagline())
            .avatarUrl(owner.getAvatarUrl())
            .contact(owner.getContact())
            .customPrompt(owner.getCustomPrompt())
            .build();
    }
}
