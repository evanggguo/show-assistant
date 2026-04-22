package com.dossier.backend.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class AiTokenBudgetService {

    private final AtomicLong dailyCallCount = new AtomicLong(0);

    @Value("${app.ai-budget.daily-max-calls:500}")
    private long dailyMaxCalls;

    /**
     * Atomically increment the daily counter.
     * Must be called synchronously in the controller before the SseEmitter is created,
     * so GlobalExceptionHandler can return HTTP 429 on budget exhaustion.
     */
    public void consumeOrThrow() {
        long current = dailyCallCount.incrementAndGet();
        if (current > dailyMaxCalls) {
            dailyCallCount.decrementAndGet();
            log.warn("Daily AI call budget exhausted: count={}, max={}", current - 1, dailyMaxCalls);
            throw new RateLimitException("AI_BUDGET_EXCEEDED",
                "The AI assistant has reached its daily usage limit. Please try again tomorrow.");
        }
        log.debug("AI call #{} of daily budget {}", current, dailyMaxCalls);
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void resetDailyCount() {
        long previous = dailyCallCount.getAndSet(0);
        log.info("Daily AI call counter reset. Previous count: {}", previous);
    }

    public long getCurrentDailyCount() {
        return dailyCallCount.get();
    }
}
