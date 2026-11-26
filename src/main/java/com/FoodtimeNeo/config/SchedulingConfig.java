package com.FoodtimeNeo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableAsync
public class SchedulingConfig {
    /** Dedicated scheduler threads so slow AI sweeps never block daily recommendations or cleanup. */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("foodtime-sched-");
        scheduler.initialize();
        return scheduler;
    }
    /** Bounded pool for instant AI triage; on overload tasks are dropped and the scheduled sweep catches up. */
    @Bean("aiTriageExecutor")
    public Executor aiTriageExecutor(
            @Value("${app.ai.triage.pool-core:4}") int core,
            @Value("${app.ai.triage.pool-max:8}") int max,
            @Value("${app.ai.triage.pool-queue:500}") int queue) {
        if (core < 1 || max < core || queue < 1) {
            throw new IllegalArgumentException(
                    "Invalid AI triage pool settings: core=" + core + ", max=" + max + ", queue=" + queue);
        }
        var pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(core);
        pool.setMaxPoolSize(max);
        pool.setQueueCapacity(queue);
        pool.setThreadNamePrefix("ai-triage-");
        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        pool.initialize();
        return pool;
    }
}
