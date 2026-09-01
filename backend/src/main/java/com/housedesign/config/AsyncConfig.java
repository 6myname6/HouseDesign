package com.housedesign.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步线程池配置，为 AI 生成等耗时任务提供独立线程池，避免阻塞 Web 请求线程。
 */
@Configuration
public class AsyncConfig {

    /** 生成任务专用线程池（bean 名 generationExecutor），核心 2 / 最大 8 线程。 */
    @Bean(name = "generationExecutor")
    public Executor generationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("gen-");
        executor.initialize();
        return executor;
    }
}
