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

/**
 * 管理端认证服务 — 从数据库校验 owner 账号密码，签发 JWT
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final JwtConfig jwtConfig;
    private final PasswordEncoder passwordEncoder;
    private final OwnerRepository ownerRepository;

    public LoginResponse login(LoginRequest request) {
        Owner owner = ownerRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new BusinessException("AUTH_FAILED", "用户名或密码错误"));

        if (owner.getPasswordHash() == null ||
            !passwordEncoder.matches(request.getPassword(), owner.getPasswordHash())) {
            throw new BusinessException("AUTH_FAILED", "用户名或密码错误");
        }

        String token = jwtConfig.generateToken(owner.getUsername());
        log.info("Owner login successful: username={}", owner.getUsername());
        return LoginResponse.builder()
            .token(token)
            .tokenType("Bearer")
            .build();
    }
}
