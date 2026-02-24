package com.example.learnworkagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 学工智能体系统主应用
 */
@SpringBootApplication
@EnableAsync
public class LearnWorkAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearnWorkAgentApplication.class, args);
    }

}
