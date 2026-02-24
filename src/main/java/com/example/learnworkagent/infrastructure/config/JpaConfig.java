package com.example.learnworkagent.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA配置
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.example.learnworkagent.domain")
public class JpaConfig {
}
