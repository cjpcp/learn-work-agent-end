package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器
 */
@RestController
public class HealthController extends BaseController {

    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("系统运行正常");
    }

    @GetMapping("/api/v1/test/auth")
    public Result<String> testAuth() {
        Long userId = getCurrentUserId();
        return Result.success("认证成功，userId: " + userId);
    }
}
