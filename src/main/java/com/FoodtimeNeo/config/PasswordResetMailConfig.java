package com.FoodtimeNeo.config;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.ThreadPoolExecutor;
@Configuration(proxyBeanMethods=false)
public class PasswordResetMailConfig {
    @Bean("passwordResetMailExecutor")
    public ThreadPoolTaskExecutor passwordResetMailExecutor() {
        var pool=new ThreadPoolTaskExecutor();pool.setCorePoolSize(2);pool.setMaxPoolSize(2);pool.setQueueCapacity(100);
        pool.setThreadNamePrefix("password-reset-mail-");pool.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        pool.setWaitForTasksToCompleteOnShutdown(true);pool.setAwaitTerminationSeconds(10);return pool;
    }
}
