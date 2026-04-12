package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.domain.approval.dto.ApprovalProcessRequest;
import com.example.learnworkagent.domain.approval.dto.ApprovalStepRequest;
import com.example.learnworkagent.domain.approval.entity.ApprovalProcess;
import com.example.learnworkagent.domain.approval.entity.ApprovalStep;
import com.example.learnworkagent.domain.approval.repository.ApprovalProcessRepository;
import com.example.learnworkagent.domain.approval.repository.ApprovalStepRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 审批流程配置控制器.
 * <p>提供审批流程和审批步骤的增删改查接口，仅允许管理员角色操作写操作.</p>
 *
 * @author system
 * @see ApprovalProcess
 * @see ApprovalStep
 */
@Tag(name = "审批流程配置", description = "审批流程定义和配置相关接口")
@RestController
@RequestMapping("/api/v1/approval/config")
@RequiredArgsConstructor
public class ApprovalConfigController {

    private final ApprovalProcessRepository processRepository;
    private final ApprovalStepRepository stepRepository;

    /**
     * 获取所有审批流程列表.
     *
     * @return 审批流程列表
     */
    @Operation(summary = "获取所有审批流程")
    @GetMapping("/processes")
    public Result<List<ApprovalProcess>> getProcesses() {
        List<ApprovalProcess> processes = processRepository.findAll();
        return Result.success(processes);
    }

    /**
     * 创建审批流程.
     *
     * @param request 审批流程请求参数
     * @return 创建的审批流程
     */
    @Operation(summary = "创建审批流程")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/processes")
    public Result<ApprovalProcess> createProcess(@Valid @RequestBody ApprovalProcessRequest request) {
        ApprovalProcess savedProcess = processRepository.save(buildProcess(new ApprovalProcess(), request));
        return Result.success(savedProcess);
    }

    /**
     * 更新指定审批流程.
     *
     * @param id      审批流程ID
     * @param request 审批流程请求参数
     * @return 更新后的审批流程
     */
    @Operation(summary = "更新审批流程")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/processes/{id}")
    public Result<ApprovalProcess> updateProcess(@PathVariable Long id, @Valid @RequestBody ApprovalProcessRequest request) {
        ApprovalProcess savedProcess = processRepository.save(buildProcess(requireProcess(id), request));
        return Result.success(savedProcess);
    }

    /**
     * 删除指定审批流程及其关联的审批步骤.
     *
     * @param id 审批流程ID
     * @return 操作结果
     */
    @Operation(summary = "删除审批流程")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/processes/{id}")
    public Result<Void> deleteProcess(@PathVariable Long id) {
        ApprovalProcess process = requireProcess(id);
        List<ApprovalStep> steps = stepRepository.findByProcessOrderByStepOrderAsc(process);
        stepRepository.deleteAll(steps);
        processRepository.delete(process);
        return Result.success();
    }

    /**
     * 获取指定流程的所有审批步骤.
     *
     * @param processId 流程ID
     * @return 审批步骤列表（按步骤顺序排列）
     */
    @Operation(summary = "获取流程的审批步骤")
    @GetMapping("/processes/{processId}/steps")
    public Result<List<ApprovalStep>> getSteps(@PathVariable Long processId) {
        ApprovalProcess process = requireProcess(processId);
        List<ApprovalStep> steps = stepRepository.findByProcessOrderByStepOrderAsc(process);
        return Result.success(steps);
    }

    /**
     * 添加审批步骤.
     *
     * @param request 审批步骤请求参数
     * @return 创建的审批步骤
     */
    @Operation(summary = "添加审批步骤")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/steps")
    public Result<ApprovalStep> addStep(@Valid @RequestBody ApprovalStepRequest request) {
        ApprovalStep savedStep = stepRepository.save(buildStep(new ApprovalStep(), request));
        return Result.success(savedStep);
    }

    /**
     * 更新指定审批步骤.
     *
     * @param id      审批步骤ID
     * @param request 审批步骤请求参数
     * @return 更新后的审批步骤
     */
    @Operation(summary = "更新审批步骤")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/steps/{id}")
    public Result<ApprovalStep> updateStep(@PathVariable Long id, @Valid @RequestBody ApprovalStepRequest request) {
        ApprovalStep savedStep = stepRepository.save(buildStep(requireStep(id), request));
        return Result.success(savedStep);
    }

    /**
     * 删除指定审批步骤.
     *
     * @param id 审批步骤ID
     * @return 操作结果
     */
    @Operation(summary = "删除审批步骤")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/steps/{id}")
    public Result<Void> deleteStep(@PathVariable Long id) {
        ApprovalStep step = requireStep(id);
        stepRepository.delete(step);
        return Result.success();
    }

    /**
     * 启用指定审批流程.
     *
     * @param id 审批流程ID
     * @return 更新后的审批流程
     */
    @Operation(summary = "启用流程")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/processes/{id}/enable")
    public Result<ApprovalProcess> enableProcess(@PathVariable Long id) {
        ApprovalProcess process = requireProcess(id);
        process.enable();
        ApprovalProcess savedProcess = processRepository.save(process);
        return Result.success(savedProcess);
    }

    /**
     * 禁用指定审批流程.
     *
     * @param id 审批流程ID
     * @return 更新后的审批流程
     */
    @Operation(summary = "禁用流程")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/processes/{id}/disable")
    public Result<ApprovalProcess> disableProcess(@PathVariable Long id) {
        ApprovalProcess process = requireProcess(id);
        process.disable();
        ApprovalProcess savedProcess = processRepository.save(process);
        return Result.success(savedProcess);
    }

    private ApprovalProcess buildProcess(ApprovalProcess process, ApprovalProcessRequest request) {
        process.setProcessName(request.getProcessName());
        process.setProcessType(request.getProcessType());
        process.setDescription(request.getDescription());
        process.setEnabled(request.getEnabled() != null ? request.getEnabled() : Boolean.TRUE);
        process.setVersion(request.getVersion() != null ? request.getVersion() : 1);
        return process;
    }

    private ApprovalStep buildStep(ApprovalStep step, ApprovalStepRequest request) {
        step.setProcess(requireProcess(request.getProcessId()));
        step.setStepName(request.getStepName());
        step.setStepOrder(request.getStepOrder());
        step.setApprovalType(request.getApprovalType());
        step.setApproverRole(request.getApproverRole());
        step.setApproverUserId(request.getApproverUserId());
        step.setMustPass(request.getMustPass() != null ? request.getMustPass() : Boolean.TRUE);
        step.setAssignMode(request.getAssignMode() != null ? request.getAssignMode() : "USER");
        step.setRoleId(request.getRoleId());
        return step;
    }

    private ApprovalProcess requireProcess(Long processId) {
        return processRepository.findById(processId)
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "流程不存在: " + processId));
    }

    private ApprovalStep requireStep(Long stepId) {
        return stepRepository.findById(stepId)
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "审批步骤不存在: " + stepId));
    }
}
