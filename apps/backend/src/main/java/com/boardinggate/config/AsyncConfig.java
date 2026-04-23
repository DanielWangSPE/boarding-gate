package com.boardinggate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 异步任务线程池（登录日志、审计等异步落库用）。
 */
@Configuration
public class AsyncConfig {

    @Bean("loginLogExecutor")
    public TaskExecutor loginLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("login-log-");
        executor.initialize();
        return executor;
    }
}
