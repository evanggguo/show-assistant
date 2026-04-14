package com.showassistant.backend.admin.auth;

import com.showassistant.backend.admin.auth.dto.LoginRequest;
import com.showassistant.backend.admin.auth.dto.LoginResponse;
import com.showassistant.backend.common.exception.BusinessException;
import com.showassistant.backend.config.JwtConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 管理员认证服务 — 校验用户名/密码，签发 JWT
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final JwtConfig jwtConfig;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password-hash}")
    private String adminPasswordHash;

    public LoginResponse login(LoginRequest request) {
        if (!adminUsername.equals(request.getUsername()) ||
            !passwordEncoder.matches(request.getPassword(), adminPasswordHash)) {
            throw new BusinessException("AUTH_FAILED", "用户名或密码错误");
        }
        String token = jwtConfig.generateToken(request.getUsername());
        log.info("Admin login successful: username={}", request.getUsername());
        return LoginResponse.builder()
            .token(token)
            .tokenType("Bearer")
            .build();
    }
}
