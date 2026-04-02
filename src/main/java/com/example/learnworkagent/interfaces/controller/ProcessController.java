package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.domain.process.dto.ProcessItem;
import com.example.learnworkagent.domain.process.dto.ProcessListResponse;
import com.example.learnworkagent.domain.process.service.ProcessService;
import com.example.learnworkagent.domain.user.entity.Admin;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/process")
@RequiredArgsConstructor
public class ProcessController extends BaseController {

    private final ProcessService processService;

    @GetMapping("/list")
    public Result<ProcessListResponse> getProcessList(@AuthenticationPrincipal Admin admin) {
        return Result.success(processService.getProcessList(admin));
    }

    @GetMapping("/completed")
    public Result<List<ProcessItem>> getCompletedProcesses(@AuthenticationPrincipal Admin admin) {
        return Result.success(processService.getCompletedProcesses(admin));
    }

    @GetMapping("/{id}")
    public Result<ProcessItem> getProcessDetail(@PathVariable String id, @RequestParam String type) {
        return Result.success(processService.getProcessDetail(id, type));
    }
}
