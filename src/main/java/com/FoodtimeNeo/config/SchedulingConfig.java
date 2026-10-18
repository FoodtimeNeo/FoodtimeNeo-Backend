package com.FoodtimeNeo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableAsync
public class SchedulingConfig {
    /** Bounded pool for instant AI triage; on overload tasks are dropped and the scheduled sweep catches up. */
    @Bean("aiTriageExecutor")
    public Executor aiTriageExecutor() {
        var pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(2);
        pool.setMaxPoolSize(4);
        pool.setQueueCapacity(200);
        pool.setThreadNamePrefix("ai-triage-");
        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        pool.initialize();
        return pool;
    }
}
