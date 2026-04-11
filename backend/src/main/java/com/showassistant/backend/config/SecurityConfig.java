package com.showassistant.backend.config;

import org.springframework.context.annotation.Configuration;

/**
 * TDD 6.3 — 安全配置（Phase 2 占位）
 *
 * Phase 2 暂时不启用 Spring Security（依赖未缓存，网络不通）。
 * 等 spring-boot-starter-security 可用后，恢复为：
 *
 * <pre>
 * {@literal @}Configuration
 * {@literal @}EnableWebSecurity
 * public class SecurityConfig {
 *     {@literal @}Bean
 *     public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 *         http.csrf(AbstractHttpConfigurer::disable)
 *             .authorizeHttpRequests(auth -> auth
 *                 .requestMatchers("/api/chat/**", "/api/owner/**",
 *                                  "/api/suggestions/**", "/api/conversations/**").permitAll()
 *                 .requestMatchers("/api/admin/**").denyAll()
 *                 .anyRequest().permitAll()
 *             );
 *         return http.build();
 *     }
 * }
 * </pre>
 */
@Configuration
public class SecurityConfig {
    // Phase 2: 无 Spring Security，所有路径默认可访问
    // Phase 5（管理端）时引入 JWT 认证
}
