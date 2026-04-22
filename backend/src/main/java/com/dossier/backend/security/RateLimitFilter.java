package com.dossier.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String ip   = extractClientIp(request);

        try {
            if (isChatStreamPath(path)) {
                rateLimitService.checkChatLimit(ip);
            } else if (path.equals("/api/admin/auth/login")) {
                rateLimitService.checkLoginLimit(ip);
            }
        } catch (RateLimitException e) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Retry-After", "60");
            response.getWriter().write(
                "{\"success\":false,\"code\":\"" + e.getCode() + "\",\"message\":\"" + e.getMessage() + "\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isChatStreamPath(String path) {
        // Matches /api/owners/{username}/chat/stream and legacy /api/chat/stream
        return path.equals("/api/chat/stream") ||
               (path.startsWith("/api/owners/") && path.endsWith("/chat/stream"));
    }

    /**
     * Extract real client IP from X-Forwarded-For set by Nginx.
     * Since Nginx is the sole entry point, this header is trusted.
     */
    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
