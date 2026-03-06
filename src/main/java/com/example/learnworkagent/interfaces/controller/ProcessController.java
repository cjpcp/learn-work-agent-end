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

@RestController
@RequestMapping("/api/v1/process")
@RequiredArgsConstructor
public class ProcessController extends BaseController {

    private final ProcessService processService;

    @GetMapping("/list")
    public Result<ProcessListResponse> getProcessList(@AuthenticationPrincipal User user) {
        ProcessListResponse response = processService.getProcessList(user);
        return Result.success(response);
    }

    @GetMapping("/{id}")
    public Result<ProcessItem> getProcessDetail(@PathVariable String id, @RequestParam String type, @AuthenticationPrincipal User user) {
        ProcessItem item = processService.getProcessDetail(id, type);
        return Result.success(item);
    }
}
