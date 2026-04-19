package com.dossier.backend.admin.auth;

import com.dossier.backend.admin.auth.dto.LoginRequest;
import com.dossier.backend.admin.auth.dto.LoginResponse;
import com.dossier.backend.common.exception.BusinessException;
import com.dossier.backend.config.JwtConfig;
import com.dossier.backend.owner.Owner;
import com.dossier.backend.owner.OwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Admin authentication service — validates owner credentials from the database and issues a JWT. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final JwtConfig jwtConfig;
    private final PasswordEncoder passwordEncoder;
    private final OwnerRepository ownerRepository;

    public LoginResponse login(LoginRequest request) {
        Owner owner = ownerRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new BusinessException("AUTH_FAILED", "Invalid username or password"));

        if (owner.getPasswordHash() == null ||
            !passwordEncoder.matches(request.getPassword(), owner.getPasswordHash())) {
            throw new BusinessException("AUTH_FAILED", "Invalid username or password");
        }

        String token = jwtConfig.generateToken(owner.getUsername());
        log.info("Owner login successful: username={}", owner.getUsername());
        return LoginResponse.builder()
            .token(token)
            .tokenType("Bearer")
            .build();
    }
}
