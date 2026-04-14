package com.showassistant.backend.admin.owner;

import com.showassistant.backend.admin.owner.dto.UpdateOwnerRequest;
import com.showassistant.backend.common.exception.ResourceNotFoundException;
import com.showassistant.backend.owner.Owner;
import com.showassistant.backend.owner.OwnerRepository;
import com.showassistant.backend.owner.dto.OwnerProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端 Owner 信息服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOwnerService {

    private static final Long DEFAULT_OWNER_ID = 1L;

    private final OwnerRepository ownerRepository;

    @Transactional(readOnly = true)
    public OwnerProfileResponse getOwnerProfile() {
        Owner owner = ownerRepository.findById(DEFAULT_OWNER_ID)
            .orElseThrow(() -> new ResourceNotFoundException("Owner", DEFAULT_OWNER_ID));
        return mapToResponse(owner);
    }

    @Transactional
    public OwnerProfileResponse updateOwnerProfile(UpdateOwnerRequest request) {
        Owner owner = ownerRepository.findById(DEFAULT_OWNER_ID)
            .orElseThrow(() -> new ResourceNotFoundException("Owner", DEFAULT_OWNER_ID));

        if (request.getName() != null) owner.setName(request.getName());
        if (request.getTagline() != null) owner.setTagline(request.getTagline());
        if (request.getAvatarUrl() != null) owner.setAvatarUrl(request.getAvatarUrl());
        if (request.getContact() != null) owner.setContact(request.getContact());

        Owner saved = ownerRepository.save(owner);
        log.info("Updated owner profile id={}", saved.getId());
        return mapToResponse(saved);
    }

    private OwnerProfileResponse mapToResponse(Owner owner) {
        return OwnerProfileResponse.builder()
            .id(owner.getId())
            .name(owner.getName())
            .tagline(owner.getTagline())
            .avatarUrl(owner.getAvatarUrl())
            .contact(owner.getContact())
            .build();
    }
}
