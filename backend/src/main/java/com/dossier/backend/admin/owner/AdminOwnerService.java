package com.dossier.backend.admin.owner;

import com.dossier.backend.admin.owner.dto.ChangePasswordRequest;
import com.dossier.backend.admin.owner.dto.ChangeUsernameRequest;
import com.dossier.backend.admin.owner.dto.UpdateOwnerRequest;
import com.dossier.backend.common.exception.BusinessException;
import com.dossier.backend.owner.Owner;
import com.dossier.backend.owner.OwnerContextHolder;
import com.dossier.backend.owner.OwnerRepository;
import com.dossier.backend.owner.dto.OwnerProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端 Owner 信息服务 — 基于当前登录 owner 操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOwnerService {

    private final OwnerRepository ownerRepository;
    private final OwnerContextHolder ownerContextHolder;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public OwnerProfileResponse getOwnerProfile() {
        return mapToResponse(ownerContextHolder.getCurrentOwner());
    }

    @Transactional
    public OwnerProfileResponse updateOwnerProfile(UpdateOwnerRequest request) {
        Owner owner = ownerContextHolder.getCurrentOwner();

        if (request.getName() != null) owner.setName(request.getName());
        if (request.getTagline() != null) owner.setTagline(request.getTagline());
        if (request.getAvatarUrl() != null) owner.setAvatarUrl(request.getAvatarUrl());
        if (request.getContact() != null) owner.setContact(request.getContact());

        Owner saved = ownerRepository.save(owner);
        log.info("Updated owner profile id={}", saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public void changeUsername(ChangeUsernameRequest request) {
        Owner owner = ownerContextHolder.getCurrentOwner();
        String newUsername = request.getNewUsername();

        if (newUsername.equals(owner.getUsername())) return;

        if (ownerRepository.existsByUsername(newUsername)) {
            throw new BusinessException("USERNAME_TAKEN", "用户名已被占用");
        }

        owner.setUsername(newUsername);
        ownerRepository.save(owner);
        log.info("Owner id={} changed username to {}", owner.getId(), newUsername);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Owner owner = ownerContextHolder.getCurrentOwner();

        if (owner.getPasswordHash() == null ||
            !passwordEncoder.matches(request.getOldPassword(), owner.getPasswordHash())) {
            throw new BusinessException("WRONG_PASSWORD", "原密码错误");
        }

        owner.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        ownerRepository.save(owner);
        log.info("Owner id={} changed password", owner.getId());
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
