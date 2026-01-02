package com.backend.farmon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "customAsyncExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 기본적으로 유지할 스레드 수
        executor.setMaxPoolSize(5); // 최대 생성 가능한 스레드 수
        executor.setQueueCapacity(100); // 작업 대기열의 크기, 최대 풀 크기 이상의 작업이 들어올 경우 대기열에 저장되는 작업 수
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        return executor;
    }
}
