package com.example.learnworkagent.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;

/**
 * 安全异步配置 - 确保SecurityContext在异步操作中传播
 */
@Configuration
@EnableAsync
public class WebAsyncConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 配置默认的异步请求超时时间为120秒
        configurer.setDefaultTimeout(120000L);
    }
}
