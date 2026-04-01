package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.common.exception.BusinessException;
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
        process.syncCompatibleFields();
        return Result.success(processRepository.save(process));
    }

    /**
     * 更新审批流程
     */
    @Operation(summary = "更新审批流程")
    @PutMapping("/processes/{id}")
    public Result<?> updateProcess(@PathVariable Long id, @Valid @RequestBody ApprovalProcess process) {
        requireProcess(id);
        process.setId(id);
        process.syncCompatibleFields();
        return Result.success(processRepository.save(process));
    }

    /**
     * 删除审批流程
     */
    @Operation(summary = "删除审批流程")
    @DeleteMapping("/processes/{id}")
    public Result<?> deleteProcess(@PathVariable Long id) {
        ApprovalProcess process = requireProcess(id);
        List<ApprovalStep> steps = stepRepository.findByProcessOrderByStepOrderAsc(process);
        stepRepository.deleteAll(steps);
        processRepository.delete(process);
        return Result.success();
    }

    /**
     * 获取流程的审批步骤
     */
    @Operation(summary = "获取流程的审批步骤")
    @GetMapping("/processes/{processId}/steps")
    public Result<?> getSteps(@PathVariable Long processId) {
        ApprovalProcess process = requireProcess(processId);
        return Result.success(stepRepository.findByProcessOrderByStepOrderAsc(process));
    }

    /**
     * 添加审批步骤
     */
    @Operation(summary = "添加审批步骤")
    @PostMapping("/steps")
    public Result<?> addStep(@Valid @RequestBody ApprovalStep step) {
        syncStepCompatibleFields(step);
        return Result.success(stepRepository.save(step));
    }

    /**
     * 更新审批步骤
     */
    @Operation(summary = "更新审批步骤")
    @PutMapping("/steps/{id}")
    public Result<?> updateStep(@PathVariable Long id, @Valid @RequestBody ApprovalStep step) {
        requireStep(id);
        step.setId(id);
        syncStepCompatibleFields(step);
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
        ApprovalProcess process = requireProcess(id);
        process.enable();
        return Result.success(processRepository.save(process));
    }

    /**
     * 禁用流程
     */
    @Operation(summary = "禁用流程")
    @PostMapping("/processes/{id}/disable")
    public Result<?> disableProcess(@PathVariable Long id) {
        ApprovalProcess process = requireProcess(id);
        process.disable();
        return Result.success(processRepository.save(process));
    }

    private void syncStepCompatibleFields(ApprovalStep step) {
        step.setName(step.getStepName());
        step.setApproverType(step.getApproverRole());
        step.setOrderIndex(step.getStepOrder());
    }

    private ApprovalProcess requireProcess(Long processId) {
        return processRepository.findById(processId)
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "流程不存在: " + processId));
    }

    private void requireStep(Long stepId) {
        stepRepository.findById(stepId)
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "审批步骤不存在: " + stepId));
    }
}
