package com.example.learnworkagent.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA配置类.
 * <p>配置JPA审计功能和仓储扫描路径.</p>
 *
 * @author system
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.example.learnworkagent.domain")
public class JpaConfig {
}
