package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器.
 * <p>提供系统健康状态检查接口.</p>
 *
 * @author system
 */
@RestController
public class HealthController {

    /**
     * 健康检查接口.
     *
     * @return 系统运行状态
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("系统运行正常");
    }
}
