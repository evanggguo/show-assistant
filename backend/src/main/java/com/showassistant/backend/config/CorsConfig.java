package com.showassistant.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * TDD 6.3.2 — CORS 跨域配置
 * 允许前端开发服务器 http://localhost:3000 发起跨域请求，
 * 支持所有 HTTP 方法和请求头，允许携带凭据（credentials）。
 */
@Configuration
public class CorsConfig {

    /**
     * TDD 6.3.2 — CORS 过滤器
     * 注册全局 CORS 过滤器，作用于所有 API 路径
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
