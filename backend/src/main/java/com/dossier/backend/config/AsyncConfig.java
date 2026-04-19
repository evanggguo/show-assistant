package com.dossier.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * TDD 3.3 — Async thread pool configuration
 * Dedicated thread pool for SSE streaming and background tasks,
 * keeping Spring MVC's default pool unblocked.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /** TDD 3.3.1 — SSE async task executor thread pool. */
    @Bean(name = "sseTaskExecutor")
    public Executor sseTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("sse-");
        executor.initialize();
        return executor;
    }
}
