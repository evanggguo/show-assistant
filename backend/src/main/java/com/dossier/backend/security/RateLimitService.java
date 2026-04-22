package com.dossier.backend.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RateLimitService {

    private final ConcurrentHashMap<String, Bucket> chatBuckets         = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> loginBuckets        = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> conversationBuckets = new ConcurrentHashMap<>();

    @Value("${app.rate-limit.chat.per-minute:10}")
    private int chatPerMinute;

    @Value("${app.rate-limit.chat.burst:3}")
    private int chatBurst;

    @Value("${app.rate-limit.login.per-minute:5}")
    private int loginPerMinute;

    @Value("${app.rate-limit.login.burst:2}")
    private int loginBurst;

    @Value("${app.rate-limit.conversation.per-hour:30}")
    private int conversationPerHour;

    public void checkChatLimit(String ip) {
        Bucket bucket = chatBuckets.computeIfAbsent(ip,
            k -> buildBucket(chatBurst, chatPerMinute, Duration.ofMinutes(1)));
        if (!bucket.tryConsume(1)) {
            log.warn("Chat rate limit exceeded for IP={}", ip);
            throw new RateLimitException("RATE_LIMIT_CHAT",
                "Too many chat requests. Please wait a moment before sending another message.");
        }
    }

    public void checkLoginLimit(String ip) {
        Bucket bucket = loginBuckets.computeIfAbsent(ip,
            k -> buildBucket(loginBurst, loginPerMinute, Duration.ofMinutes(1)));
        if (!bucket.tryConsume(1)) {
            log.warn("Login rate limit exceeded for IP={}", ip);
            throw new RateLimitException("RATE_LIMIT_LOGIN",
                "Too many login attempts. Please wait before trying again.");
        }
    }

    public void checkConversationLimit(String ip) {
        Bucket bucket = conversationBuckets.computeIfAbsent(ip,
            k -> buildBucket(conversationPerHour, conversationPerHour, Duration.ofHours(1)));
        if (!bucket.tryConsume(1)) {
            log.warn("Conversation creation rate limit exceeded for IP={}", ip);
            throw new RateLimitException("RATE_LIMIT_CONVERSATION",
                "Too many conversations started. Please try again later.");
        }
    }

    private Bucket buildBucket(int capacity, int refillTokens, Duration refillPeriod) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(refillTokens, refillPeriod));
        return Bucket.builder().addLimit(limit).build();
    }
}
