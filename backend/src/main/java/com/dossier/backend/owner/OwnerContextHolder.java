package com.dossier.backend.owner;

import com.dossier.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the currently authenticated owner from Spring's SecurityContext.
 * Admin endpoints use this instead of the hard-coded DEFAULT_OWNER_ID=1.
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
