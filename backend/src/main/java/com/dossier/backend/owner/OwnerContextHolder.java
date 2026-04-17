package com.dossier.backend.owner;

import com.dossier.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 从 Spring SecurityContext 中解析当前登录的 Owner
 * 管理端接口通过此组件获取当前 owner，替代原来硬编码的 DEFAULT_OWNER_ID=1
 */
@Component
@RequiredArgsConstructor
public class OwnerContextHolder {

    private final OwnerRepository ownerRepository;

    public Owner getCurrentOwner() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ownerRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("OWNER_NOT_FOUND", "Owner not found: " + username));
    }

    public Long getCurrentOwnerId() {
        return getCurrentOwner().getId();
    }
}
