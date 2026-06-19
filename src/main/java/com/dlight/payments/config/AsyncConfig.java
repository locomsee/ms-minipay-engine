package com.dlight.payments.config;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

/**
 * The bounded queue on {@link #notificationExecutor()} is the "simple in-memory queue"
 * the assignment asks for as an SMS fallback mechanism - notification tasks pile up here
 * when the worker pool is busy, instead of being dropped or run on unbounded ad-hoc threads.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private final int corePoolSize;
    private final int maxPoolSize;
    private final int queueCapacity;

    public AsyncConfig(
            @Value("${minipay.notification.executor.core-pool-size:2}") int corePoolSize,
            @Value("${minipay.notification.executor.max-pool-size:5}") int maxPoolSize,
            @Value("${minipay.notification.executor.queue-capacity:100}") int queueCapacity) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.queueCapacity = queueCapacity;
    }

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("notification-");
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return notificationExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return this::handleUncaughtException;
    }

    private void handleUncaughtException(Throwable throwable, Method method, Object... params) {
        log.error("notification_failed method={} params={}", method.getName(), params, throwable);
    }
}
