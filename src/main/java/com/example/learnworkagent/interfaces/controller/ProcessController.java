package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.domain.process.dto.ProcessItem;
import com.example.learnworkagent.domain.process.service.ProcessService;
import com.example.learnworkagent.domain.user.entity.Admin;
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

    @GetMapping("/pending/all")
    public Result<PageResult<ProcessItem>> getPendingAll(@AuthenticationPrincipal Admin admin, PageRequest pageRequest) {
        return Result.success(processService.getPendingAll(admin, pageRequest));
    }

    @GetMapping("/pending/award")
    public Result<PageResult<ProcessItem>> getPendingAward(@AuthenticationPrincipal Admin admin, PageRequest pageRequest) {
        return Result.success(processService.getPendingAward(admin, pageRequest));
    }

    @GetMapping("/pending/leave")
    public Result<PageResult<ProcessItem>> getPendingLeave(@AuthenticationPrincipal Admin admin, PageRequest pageRequest) {
        return Result.success(processService.getPendingLeave(admin, pageRequest));
    }

    @GetMapping("/pending/leave-cancel")
    public Result<PageResult<ProcessItem>> getPendingLeaveCancel(@AuthenticationPrincipal Admin admin, PageRequest pageRequest) {
        return Result.success(processService.getPendingLeaveCancel(admin, pageRequest));
    }

    @GetMapping("/completed/all")
    public Result<PageResult<ProcessItem>> getCompletedAll(@AuthenticationPrincipal Admin admin, PageRequest pageRequest) {
        return Result.success(processService.getCompletedAll(admin, pageRequest));
    }

    @GetMapping("/completed/award")
    public Result<PageResult<ProcessItem>> getCompletedAward(@AuthenticationPrincipal Admin admin, PageRequest pageRequest) {
        return Result.success(processService.getCompletedAward(admin, pageRequest));
    }

    @GetMapping("/completed/leave")
    public Result<PageResult<ProcessItem>> getCompletedLeave(@AuthenticationPrincipal Admin admin, PageRequest pageRequest) {
        return Result.success(processService.getCompletedLeave(admin, pageRequest));
    }

    @GetMapping("/completed/leave-cancel")
    public Result<PageResult<ProcessItem>> getCompletedLeaveCancel(@AuthenticationPrincipal Admin admin, PageRequest pageRequest) {
        return Result.success(processService.getCompletedLeaveCancel(admin, pageRequest));
    }


    @GetMapping("/{id}")
    public Result<ProcessItem> getProcessDetail(@PathVariable String id, @RequestParam String type) {
        return Result.success(processService.getProcessDetail(id, type));
    }
}
