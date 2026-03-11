package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.domain.approval.service.ApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 审批管理控制器
 */
@Tag(name = "审批管理", description = "审批流程相关接口")
@RestController
@RequestMapping("/api/v1/approval")
@RequiredArgsConstructor
public class ApprovalController extends BaseController {

    private final ApprovalService approvalService;

    /**
     * 获取我的待审批任务
     */
    @Operation(summary = "获取我的待审批任务")
    @GetMapping("/tasks/pending")
    public Result<?> getPendingTasks() {
        Long approverId = getCurrentUserId();
        return Result.success(approvalService.getPendingTasks(approverId));
    }

    /**
     * 处理审批任务
     */
    @Operation(summary = "处理审批任务")
    @PostMapping("/tasks/{id}/process")
    public Result<?> processTask(@PathVariable Long id, @Valid @RequestBody Map<String, Object> request) {
        Long approverId = getCurrentUserId();
        String status = (String) request.get("status");
        String comment = (String) request.get("comment");
        return Result.success(approvalService.processApprovalTask(id, approverId, status, comment));
    }

    /**
     * 获取审批详情
     */
    @Operation(summary = "获取审批详情")
    @GetMapping("/instances/{businessType}/{businessId}")
    public Result<?> getApprovalInstance(@PathVariable String businessType, @PathVariable Long businessId) {
        return Result.success(approvalService.getApprovalInstance(businessType, businessId));
    }
}
