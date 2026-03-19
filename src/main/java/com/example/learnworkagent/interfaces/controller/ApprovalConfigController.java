package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.domain.approval.entity.ApprovalProcess;
import com.example.learnworkagent.domain.approval.entity.ApprovalStep;
import com.example.learnworkagent.domain.approval.repository.ApprovalProcessRepository;
import com.example.learnworkagent.domain.approval.repository.ApprovalStepRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审批流程配置控制器
 */
@Tag(name = "审批流程配置", description = "审批流程定义和配置相关接口")
@RestController
@RequestMapping("/api/v1/approval/config")
@RequiredArgsConstructor
public class ApprovalConfigController extends BaseController {

    private final ApprovalProcessRepository processRepository;
    private final ApprovalStepRepository stepRepository;

    /**
     * 获取所有审批流程
     */
    @Operation(summary = "获取所有审批流程")
    @GetMapping("/processes")
    public Result<?> getProcesses() {
        return Result.success(processRepository.findAll());
    }

    /**
     * 创建审批流程
     */
    @Operation(summary = "创建审批流程")
    @PostMapping("/processes")
    public Result<?> createProcess(@Valid @RequestBody ApprovalProcess process) {
        // 设置name字段为processName的值，确保两个字段一致
        process.setName(process.getProcessName());
        // 设置type字段为processType的值，确保两个字段一致
        process.setType(process.getProcessType());
        return Result.success(processRepository.save(process));
    }

    /**
     * 更新审批流程
     */
    @Operation(summary = "更新审批流程")
    @PutMapping("/processes/{id}")
    public Result<?> updateProcess(@PathVariable Long id, @Valid @RequestBody ApprovalProcess process) {
        process.setId(id);
        // 设置name字段为processName的值，确保两个字段一致
        process.setName(process.getProcessName());
        // 设置type字段为processType的值，确保两个字段一致
        process.setType(process.getProcessType());
        return Result.success(processRepository.save(process));
    }

    /**
     * 删除审批流程
     */
    @Operation(summary = "删除审批流程")
    @DeleteMapping("/processes/{id}")
    public Result<?> deleteProcess(@PathVariable Long id) {
        // 先删除关联的审批步骤
        ApprovalProcess process = processRepository.findById(id).orElse(null);
        if (process != null) {
            List<ApprovalStep> steps = stepRepository.findByProcessOrderByStepOrderAsc(process);
            stepRepository.deleteAll(steps);
        }
        // 再删除审批流程
        processRepository.deleteById(id);
        return Result.success();
    }

    /**
     * 获取流程的审批步骤
     */
    @Operation(summary = "获取流程的审批步骤")
    @GetMapping("/processes/{processId}/steps")
    public Result<?> getSteps(@PathVariable Long processId) {
        ApprovalProcess process = processRepository.findById(processId).orElse(null);
        if (process == null) {
            return Result.fail("流程不存在");
        }
        return Result.success(stepRepository.findByProcessOrderByStepOrderAsc(process));
    }

    /**
     * 添加审批步骤
     */
    @Operation(summary = "添加审批步骤")
    @PostMapping("/steps")
    public Result<?> addStep(@Valid @RequestBody ApprovalStep step) {
        // 设置name字段为stepName的值，确保两个字段一致
        step.setName(step.getStepName());
        // 设置approverType字段为approverRole的值，确保两个字段一致
        step.setApproverType(step.getApproverRole());
        // 设置orderIndex字段为stepOrder的值，确保两个字段一致
        step.setOrderIndex(step.getStepOrder());
        return Result.success(stepRepository.save(step));
    }

    /**
     * 更新审批步骤
     */
    @Operation(summary = "更新审批步骤")
    @PutMapping("/steps/{id}")
    public Result<?> updateStep(@PathVariable Long id, @Valid @RequestBody ApprovalStep step) {
        step.setId(id);
        // 设置name字段为stepName的值，确保两个字段一致
        step.setName(step.getStepName());
        // 设置approverType字段为approverRole的值，确保两个字段一致
        step.setApproverType(step.getApproverRole());
        // 设置orderIndex字段为stepOrder的值，确保两个字段一致
        step.setOrderIndex(step.getStepOrder());
        return Result.success(stepRepository.save(step));
    }

    /**
     * 删除审批步骤
     */
    @Operation(summary = "删除审批步骤")
    @DeleteMapping("/steps/{id}")
    public Result<?> deleteStep(@PathVariable Long id) {
        stepRepository.deleteById(id);
        return Result.success();
    }

    /**
     * 启用流程
     */
    @Operation(summary = "启用流程")
    @PostMapping("/processes/{id}/enable")
    public Result<?> enableProcess(@PathVariable Long id) {
        ApprovalProcess process = processRepository.findById(id).orElse(null);
        if (process == null) {
            return Result.fail("流程不存在");
        }
        process.setEnabled(true);
        return Result.success(processRepository.save(process));
    }

    /**
     * 禁用流程
     */
    @Operation(summary = "禁用流程")
    @PostMapping("/processes/{id}/disable")
    public Result<?> disableProcess(@PathVariable Long id) {
        ApprovalProcess process = processRepository.findById(id).orElse(null);
        if (process == null) {
            return Result.fail("流程不存在");
        }
        process.setEnabled(false);
        return Result.success(processRepository.save(process));
    }
}
