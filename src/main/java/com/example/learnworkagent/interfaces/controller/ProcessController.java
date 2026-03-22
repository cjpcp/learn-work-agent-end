package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.domain.process.dto.ProcessItem;
import com.example.learnworkagent.domain.process.dto.ProcessListResponse;
import com.example.learnworkagent.domain.process.service.ProcessService;
import com.example.learnworkagent.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 流程控制器
 * 提供流程相关的接口
 */
@RestController
@RequestMapping("/api/v1/process")
@RequiredArgsConstructor
public class ProcessController extends BaseController {

    private final ProcessService processService;

    /**
     * 根据用户获取流程列表
     *
     * @param user 用户信息
     * @return 未完成流程和已完成流程
     */
    @GetMapping("/list")
    public Result<ProcessListResponse> getProcessList(@AuthenticationPrincipal User user) {
        ProcessListResponse response = processService.getProcessList(user);
        return Result.success(response);
    }

    @GetMapping("/completed")
    public Result<List<ProcessItem>> getCompletedProcesses(@AuthenticationPrincipal User user) {
        List<ProcessItem> completedProcesses = processService.getCompletedProcesses(user);
        return Result.success(completedProcesses);
    }

    @GetMapping("/{id}")
    public Result<ProcessItem> getProcessDetail(@PathVariable String id, @RequestParam String type) {
        ProcessItem item = processService.getProcessDetail(id, type);
        return Result.success(item);
    }
}
