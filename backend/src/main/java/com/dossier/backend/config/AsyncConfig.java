package com.dossier.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * TDD 3.3 — 异步线程池配置
 * 为 SSE 流式推送和后台任务配置专用线程池，
 * 避免阻塞 Spring MVC 默认线程池。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * TDD 3.3.1 — SSE 异步任务执行器
     * 配置用于处理 SSE 流式推送的线程池
     */
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
