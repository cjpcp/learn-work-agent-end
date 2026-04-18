package com.example.learnworkagent.domain.leave.service;

import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.common.enums.ApprovalStatusEnum;
import com.example.learnworkagent.common.enums.LeaveSlipStatusEnum;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.utils.ApplicationServiceUtils;
import com.example.learnworkagent.domain.approval.service.ApprovalService;
import com.example.learnworkagent.domain.leave.dto.LeaveApplicationRequest;
import com.example.learnworkagent.domain.leave.dto.LeaveSlipPreviewRequest;
import com.example.learnworkagent.domain.leave.entity.LeaveApplication;
import com.example.learnworkagent.domain.leave.repository.LeaveApplicationRepository;
import com.example.learnworkagent.infrastructure.external.template.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * 请假申请服务.
 * <p>提供请假申请、审批、销假等业务逻辑.</p>
 *
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveApplicationService {

    private static final String BUSINESS_TYPE_LEAVE = "LEAVE";
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final TemplateService templateService;
    private final ApprovalService approvalService;
    private final ObjectMapper objectMapper;

    /**
     * 提交请假申请。
     *
     * @param applicantId 申请人ID
     * @param request     请假申请参数
     * @return 保存后的请假申请
     */
    @Transactional
    public LeaveApplication submitLeaveApplication(Long applicantId, LeaveApplicationRequest request) {
        validateLeaveDates(request);

        int leaveDays = calculateLeaveDays(request);
        LeaveApplication application = buildLeaveApplication(applicantId, request, leaveDays);
        LeaveApplication savedApplication = leaveApplicationRepository.save(application);
        createApprovalFlow(savedApplication, applicantId, request);
        return savedApplication;
    }


    public byte[] generateLeaveSlipPreview(LeaveSlipPreviewRequest request) {
        int days = (int) java.time.temporal.ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        try {
            return templateService.generateLeaveSlipPreview(
                    request.getStudentName(),
                    request.getCardNumber(),
                    request.getGrade(),
                    request.getClassName(),
                    request.getPhone(),
                    request.getLeaveType(),
                    request.getStartDate(),
                    request.getEndDate(),
                    days,
                    request.getReason()
            );
        } catch (Exception exception) {
            log.error("生成请假条预览失败", exception);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "生成请假条预览失败: " + exception.getMessage());
        }
    }

    /**
     * 申请销假。
     *
     * @param applicationId 请假申请ID
     */
    @Transactional
    public void requestCancelLeave(Long applicationId) {
        LeaveApplication application = getApplicationById(applicationId);

        if (!application.isApproved()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只有已批准的申请才能申请销假");
        }
        if (Boolean.TRUE.equals(application.getCancelled())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该申请已销假");
        }
        if (Boolean.TRUE.equals(application.getCancelRequested())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该申请已提交销假申请");
        }

        application.setCancelRequested(true);
        application.setCancelApprovalStatus(ApprovalStatusEnum.PENDING.getCode());
        leaveApplicationRepository.save(application);
    }

    /**
     * 审批销假申请。
     *
     * @param applicationId 请假申请ID
     * @param approved      是否批准
     * @param comment       审批意见
     */
    @Transactional
    public void approveCancelRequest(Long applicationId, boolean approved, String comment) {
        LeaveApplication application = getApplicationById(applicationId);

        if (!Boolean.TRUE.equals(application.getCancelRequested())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该申请未提交销假申请");
        }
        if (!ApprovalStatusEnum.PENDING.getCode().equals(application.getCancelApprovalStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "销假申请已审批");
        }

        if (approved) {
            application.markCancelled();
        }
        application.setCancelApprovalStatus(approved ? ApprovalStatusEnum.APPROVED.getCode() : ApprovalStatusEnum.REJECTED.getCode());
        application.setCancelApprovalComment(comment);
        application.setCancelApprovalTime(LocalDateTime.now());
        leaveApplicationRepository.save(application);
    }

    /**
     * 获取申请详情。
     *
     * @param applicationId 请假申请ID
     * @return 请假申请
     */
    public LeaveApplication getApplicationById(Long applicationId) {
        return leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ResultCode.LEAVE_APPLICATION_NOT_FOUND));
    }

    /**
     * 分页查询用户的请假申请。
     *
     * @param userId      用户ID
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    public PageResult<LeaveApplication> getUserApplications(Long userId, PageRequest pageRequest) {
        Pageable pageable = buildPageable(pageRequest);
        Page<LeaveApplication> page = leaveApplicationRepository
                .findByApplicantIdAndDeletedFalseOrderByCreateTimeDesc(userId, pageable);
        return buildPageResult(page, pageRequest);
    }


    /**
     * 分页查询待审批销假申请。
     *
     * @param approverId  审批人ID（原请假审批人）
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    public PageResult<LeaveApplication> getPendingCancelRequests(Long approverId, PageRequest pageRequest) {
        Pageable pageable = buildPageable(pageRequest);
        Page<LeaveApplication> page = leaveApplicationRepository.findPendingCancelRequestsByApproverId(approverId, pageable);
        return buildPageResult(page, pageRequest);
    }

    private void validateLeaveDates(LeaveApplicationRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "开始日期不能晚于结束日期");
        }
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "开始日期不能早于今天");
        }
    }

    private int calculateLeaveDays(LeaveApplicationRequest request) {
        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        return Math.toIntExact(days);
    }

    private LeaveApplication buildLeaveApplication(Long applicantId, LeaveApplicationRequest request, int days) {
        LeaveApplication application = new LeaveApplication();
        application.setApplicantId(applicantId);
        application.setLeaveType(request.getLeaveType());
        application.setStartDate(request.getStartDate());
        application.setEndDate(request.getEndDate());
        application.setDays(days);
        application.setReason(request.getReason());
        application.setAttachmentUrl(request.getAttachmentUrl());
        application.setStudentName(request.getStudentName());
        application.setDepartmentName(request.getDepartmentName());
        application.setGrade(request.getGrade());
        application.setClassName(request.getClassName());
        application.setApprovalStatus(ApprovalStatusEnum.PENDING.getCode());
        application.setLeaveSlipStatus(LeaveSlipStatusEnum.NOT_GENERATED.getCode());
        return application;
    }

    private void createApprovalFlow(LeaveApplication application, Long applicantId, LeaveApplicationRequest request) {
        try {
            String applicantInfoJson = objectMapper.writeValueAsString(buildApplicantInfo(request));
            approvalService.createApprovalInstance(BUSINESS_TYPE_LEAVE, application.getId(), applicantId, applicantInfoJson);
        } catch (Exception exception) {
            log.error("创建请假审批流程失败，申请ID: {}", application.getId(), exception);
        }
    }

    private Map<String, Object> buildApplicantInfo(LeaveApplicationRequest request) {
        Map<String, Object> applicantInfo = new HashMap<>(4);
        applicantInfo.put("studentName", request.getStudentName());
        applicantInfo.put("departmentName", request.getDepartmentName());
        applicantInfo.put("grade", request.getGrade());
        applicantInfo.put("className", request.getClassName());
        return applicantInfo;
    }

    private Pageable buildPageable(PageRequest pageRequest) {
        return ApplicationServiceUtils.buildPageable(pageRequest);
    }

    private PageResult<LeaveApplication> buildPageResult(Page<LeaveApplication> page, PageRequest pageRequest) {
        return ApplicationServiceUtils.buildPageResult(page, pageRequest);
    }

    @Transactional
    public void withdrawApplication(Long applicationId, Long userId) {
        LeaveApplication application = getApplicationById(applicationId);
        if (!application.getApplicantId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此申请");
        }
        if (!ApprovalStatusEnum.PENDING.getCode().equals(application.getApprovalStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只能撤销待审批的申请");
        }
        approvalService.cancelApprovalInstance(BUSINESS_TYPE_LEAVE, applicationId);
    }


}
