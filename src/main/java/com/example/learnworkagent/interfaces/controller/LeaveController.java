package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.domain.leave.dto.LeaveApplicationRequest;
import com.example.learnworkagent.domain.leave.dto.LeaveSlipPreviewRequest;
import com.example.learnworkagent.domain.leave.entity.LeaveApplication;
import com.example.learnworkagent.domain.leave.service.LeaveApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 请假管理控制器。
 */
@Tag(name = "请假管理", description = "请假申请、审批、销假等相关接口")
@RestController
@RequestMapping("/api/v1/leave")
@RequiredArgsConstructor
public class LeaveController extends BaseController {

    private static final String DOCX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String CONTENT_DISPOSITION_TEMPLATE = "attachment; filename=\"%s\"; filename*=UTF-8''%s";

    private final LeaveApplicationService leaveApplicationService;

    /**
     * 提交请假申请。
     *
     * @param request 请假申请参数
     * @return 请假申请结果
     */
    @Operation(summary = "提交请假申请")
    @PostMapping("/applications")
    public Result<LeaveApplication> submitApplication(@Valid @RequestBody LeaveApplicationRequest request) {
        LeaveApplication application = leaveApplicationService.submitLeaveApplication(getRequiredCurrentUserId(), request);
        return Result.success(application);
    }

    /**
     * 撤销请假申请。
     *
     * @param id 请假申请ID
     * @return 响应结果
     */
    @Operation(summary = "撤销请假申请")
    @DeleteMapping("/applications/{id}")
    public Result<Void> withdrawApplication(@PathVariable Long id) {
        leaveApplicationService.withdrawApplication(id, getRequiredCurrentUserId());
        return Result.success();
    }

    /**
     * 获取申请详情。
     *
     * @param id 请假申请ID
     * @return 请假申请详情
     */
    @Operation(summary = "获取申请详情")
    @GetMapping("/applications/{id}")
    public Result<LeaveApplication> getApplication(@PathVariable Long id) {
        return Result.success(leaveApplicationService.getApplicationById(id));
    }

    /**
     * 分页查询我的请假申请。
     *
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    @Operation(summary = "分页查询我的请假申请")
    @GetMapping("/applications/my")
    public Result<PageResult<LeaveApplication>> getMyApplications(@Valid PageRequest pageRequest) {
        PageResult<LeaveApplication> result = leaveApplicationService.getUserApplications(getRequiredCurrentUserId(), pageRequest);
        return Result.success(result);
    }

    @Operation(summary = "预览请假条")
    @PostMapping("/slip-preview")
    public void previewLeaveSlip(@Valid @RequestBody LeaveSlipPreviewRequest request, HttpServletResponse response) {
        byte[] docBytes = leaveApplicationService.generateLeaveSlipPreview(request);
        try {
            String fileName = "请假条_" + request.getStudentName() + "_" + request.getStartDate() + ".docx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            response.setContentType(DOCX_CONTENT_TYPE);
            response.setHeader("Content-Disposition", CONTENT_DISPOSITION_TEMPLATE.formatted(encodedFileName, encodedFileName));
            response.getOutputStream().write(docBytes);
            response.getOutputStream().flush();
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "生成请假条预览失败: " + exception.getMessage());
        }
    }

    /**
     * 申请销假。
     *
     * @param id 请假申请ID
     * @return 响应结果
     */
    @Operation(summary = "申请销假")
    @PostMapping("/applications/{id}/cancel")
    public Result<Void> requestCancelLeave(@PathVariable Long id) {
        leaveApplicationService.requestCancelLeave(id);
        return Result.success();
    }

    @Operation(summary = "审批销假申请")
    @PostMapping("/applications/{id}/approve-cancel")
    public Result<Void> approveCancelRequest(@PathVariable Long id,
                                             @RequestBody ApproveCancelRequest request) {
        boolean approved = "APPROVED".equals(request.getApprovalStatus());
        leaveApplicationService.approveCancelRequest(id, approved, request.getApprovalComment());
        return Result.success();
    }

    /**
     * 分页查询待审批销假申请。
     *
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    @Operation(summary = "分页查询待审批销假申请")
    @GetMapping("/applications/pending-cancel")
    public Result<PageResult<LeaveApplication>> getPendingCancelRequests(@Valid PageRequest pageRequest) {
        PageResult<LeaveApplication> result = leaveApplicationService.getPendingCancelRequests(getRequiredCurrentUserId(), pageRequest);
        return Result.success(result);
    }

    @Data
    public static class ApproveCancelRequest {
        private String approvalStatus;
        private String approvalComment;

    }


    private Long getRequiredCurrentUserId() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录或登录已过期");
        }
        return userId;
    }
}
