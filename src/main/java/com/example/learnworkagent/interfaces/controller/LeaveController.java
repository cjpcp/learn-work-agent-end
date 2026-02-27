package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.domain.leave.dto.ApprovalRequest;
import com.example.learnworkagent.domain.leave.dto.LeaveApplicationRequest;
import com.example.learnworkagent.domain.leave.entity.LeaveApplication;
import com.example.learnworkagent.domain.leave.service.LeaveApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

/**
 * 请假管理控制器
 */
@Tag(name = "请假管理", description = "请假申请、审批、销假等相关接口")
@RestController
@RequestMapping("/api/v1/leave")
@RequiredArgsConstructor
public class LeaveController extends BaseController {

    private final LeaveApplicationService leaveApplicationService;

    /**
     * 提交请假申请
     *
     * @param request 请假申请信息封装：请假类型、开始日期、结束日期、请假事由、附件等
     * @return 完整请假信息：申请ID、申请人ID、请假类型、开始日期、结束日期、请假天数、请假事由、附件URL、审批人ID、审批状态、审批意见、审批时间、请假条
     */
    @Operation(summary = "提交请假申请")
    @PostMapping("/applications")
    public Result<LeaveApplication> submitApplication(@Valid @RequestBody LeaveApplicationRequest request) {
        Long userId = getCurrentUserId();
        LeaveApplication application = leaveApplicationService.submitLeaveApplication(
                userId,
                request.getLeaveType(),
                request.getStartDate(),
                request.getEndDate(),
                request.getReason(),
                request.getAttachmentUrl()
        );
        return Result.success(application);
    }

    /**
     * 获取申请详情
     *
     * @param id 请假id
     * @return 完整请假信息：申请ID、申请人ID、请假类型、开始日期、结束日期、请假天数、请假事由、附件URL、审批人ID、审批状态、审批意见、审批时间、请假条
     */
    @Operation(summary = "获取申请详情")
    @GetMapping("/applications/{id}")
    public Result<LeaveApplication> getApplication(@PathVariable Long id) {
        LeaveApplication application = leaveApplicationService.getApplicationById(id);
        return Result.success(application);
    }

    /**
     * 分页查询我的请假申请
     *
     * @param pageRequest 分页参数
     * @return 分页查询结果
     */
    @Operation(summary = "分页查询我的请假申请")
    @GetMapping("/applications/my")
    public Result<PageResult<LeaveApplication>> getMyApplications(@Valid PageRequest pageRequest) {
        Long userId = getCurrentUserId();
        PageResult<LeaveApplication> result = leaveApplicationService.getUserApplications(userId, pageRequest);
        return Result.success(result);
    }

    /**
     * 分页查询待审批的申请（审批人）
     *
     * @param pageRequest 分页参数
     * @return 分页查询结果
     */
    @Operation(summary = "分页查询待审批的申请（审批人）")
    @GetMapping("/applications/pending")
    public Result<PageResult<LeaveApplication>> getPendingApplications(@Valid PageRequest pageRequest) {
        Long approverId = getCurrentUserId();
        PageResult<LeaveApplication> result = leaveApplicationService.getPendingApplications(approverId, pageRequest);
        return Result.success(result);
    }

    /**
     * 审批请假申请
     *
     * @param id      请假id
     * @param request 审批信息封装：审批状态、审批意见
     * @return 响应结果
     */
    @Operation(summary = "审批请假申请")
    @PostMapping("/applications/{id}/approve")
    public Result<Void> approveApplication(@PathVariable Long id, @Valid @RequestBody ApprovalRequest request) {
        Long approverId = getCurrentUserId();
        leaveApplicationService.approveLeaveApplication(id, approverId, request.getApprovalStatus(), request.getApprovalComment());
        return Result.success();
    }

    /**
     * 生成请假条
     *
     * @param id 请假id
     * @return 响应结果
     */
    @Operation(summary = "生成请假条")
    @PostMapping("/applications/{id}/generate-slip")
    public Result<Void> generateLeaveSlip(@PathVariable Long id) {
        leaveApplicationService.generateLeaveSlip(id);
        return Result.success();
    }

    /**
     * 销假
     *
     * @param id 请假id
     * @return 响应结果
     */
    @Operation(summary = "销假")
    @PostMapping("/applications/{id}/cancel")
    public Result<Void> cancelLeave(@PathVariable Long id) {
        leaveApplicationService.cancelLeave(id);
        return Result.success();
    }

    /**
     * 下载请假条PDF
     *
     * @param id 请假id
     * @param response HTTP响应
     */
    @Operation(summary = "下载请假条PDF", description = "下载已生成的请假条PDF文件")
    @GetMapping("/applications/{id}/download-slip")
    public void downloadLeaveSlip(@PathVariable Long id, HttpServletResponse response) {
        LeaveApplication application = leaveApplicationService.getApplicationById(id);
        
        // 如果请假条尚未生成，自动生成
        if (application.getLeaveSlipUrl() == null || application.getLeaveSlipUrl().isEmpty()) {
            leaveApplicationService.generateLeaveSlip(id);
            // 重新获取更新后的申请信息
            application = leaveApplicationService.getApplicationById(id);
        }
        
        try {
            // 重定向到OSS文件URL
            response.sendRedirect(application.getLeaveSlipUrl());
        } catch (Exception e) {
            throw new RuntimeException("下载请假条失败: " + e.getMessage());
        }
    }
}
