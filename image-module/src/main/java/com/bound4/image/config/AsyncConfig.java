package com.bound4.image.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리를 위한 설정
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);
    
    /**
     * 썸네일 생성을 위한 비동기 실행자
     */
    @Bean(name = "thumbnailTaskExecutor")
    public Executor thumbnailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("thumbnail-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.setRejectedExecutionHandler((runnable, exec) -> {
            logger.warn("Thumbnail task rejected, queue full. Task: {}", runnable);
            throw new RuntimeException("Thumbnail task queue is full");
        });
        
        executor.initialize();
        
        logger.info("Thumbnail task executor initialized with core={}, max={}, queue={}", 
                   executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }
}